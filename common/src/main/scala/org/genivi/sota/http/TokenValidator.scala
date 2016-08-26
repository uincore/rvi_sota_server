package org.genivi.sota.http

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{FormData,HttpEntity,HttpMethods,HttpRequest,Multipart,RequestEntity,StatusCodes,Uri}
import akka.http.scaladsl.model.headers.{Authorization,BasicHttpCredentials,OAuth2BearerToken}
import akka.http.scaladsl.unmarshalling.{Unmarshal,Unmarshaller}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0, Directives}
import akka.http.scaladsl.server.directives.{AuthenticationDirective, Credentials}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.util.ByteString
import cats.data.Xor
import com.typesafe.config.ConfigFactory
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure,Success,Try}

case class ValidationResponse(active: Boolean)

object TokenValidator {
  def apply()(implicit system: ActorSystem, mat: ActorMaterializer) = new TokenValidator
}

class TokenValidator(implicit system: ActorSystem, mat: ActorMaterializer) {
  import CirceMarshallingSupport._
  import Directives._
  import Json._

  private val config = ConfigFactory.load()
  val logger = LoggerFactory.getLogger(this.getClass)

  lazy val authPlusUri = Uri(config.getString("authplus.api.uri"))
  lazy val clientId = config.getString("authplus.client.id")
  lazy val clientSecret = config.getString("authplus.client.secret")

  private def authPlusValidate(token: String)
                              (implicit ec: ExecutionContext): Future[Boolean] = {
    import StatusCodes._
    val uri = authPlusUri.withPath(Uri("/introspect").path)
    val entity = HttpEntity(`application/json`, fromString(token).noSpaces )
    val form = Multipart.FormData(Map("token" -> entity))
    val request = Post(uri,form) ~>
            Authorization(BasicHttpCredentials(clientId, clientSecret))
    for {
      response <- Http().singleRequest(request)
      status <- response.status match {
        case OK => Unmarshal(response.entity).to[ValidationResponse].map(_.active)
        case _  => {
          logger.error(s"Something went wrong ${response.toString}")
          FastFuture.successful(false)
        }
      }
    } yield status
  }

  def validate: Directive0 = {
    extractExecutionContext flatMap { implicit ec =>
      AuthToken.fromConfig() flatMap {
        case None =>
          Directives.pass
        case Some(token) =>
          onComplete(authPlusValidate(token)) flatMap {
            case Success(true)  => Directives.pass
            case Success(false) => {
              logger.info("AuthPlus rejects the token")
              reject(AuthorizationFailedRejection)
            }
            case Failure(err) => {
              logger.error(s"Something went wrong ${err.toString}")
              reject(AuthorizationFailedRejection)
            }
          }
      }
    }
  }
}
