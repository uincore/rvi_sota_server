/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.client

import java.time.Instant

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{Device, DeviceT, Namespace}
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class DeviceRegistryClient(baseUri: Uri, devicesUri: Uri)
                          (implicit system: ActorSystem, mat: ActorMaterializer)
    extends DeviceRegistry {

  import CirceMarshallingSupport._
  import Device._
  import HttpMethods._
  import StatusCodes._

  private val log = LoggerFactory.getLogger(this.getClass)

  type Request[T] = HttpClientRequest[T]

  override def searchDevice(ns: Namespace, re: String Refined Regex)
                           (implicit ec: ExecutionContext): Request[Seq[Device]] =
    HttpClientRequest[Seq[Device]](HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("regex" -> re.get, "namespace" -> ns.get))))
      .recover { case t =>
        log.error("Could not contact device registry", t)
        Seq.empty[Device]
      }

  override def createDevice(device: DeviceT)
                           (implicit ec: ExecutionContext): Request[Id] = HttpClientRequest {
    Marshal(device).to[MessageEntity].map { entity =>
      HttpRequest(method = POST, uri = baseUri.withPath(devicesUri.path), entity = entity)
    }
  }

  override def fetchDevice(id: Id)
                          (implicit ec: ExecutionContext): Request[Device] =
    HttpClientRequest[Device](HttpRequest(uri = baseUri.withPath(devicesUri.path / id.show)))

  override def fetchByDeviceId(ns: Namespace, id: DeviceId)
                              (implicit ec: ExecutionContext): Request[Device] =
    HttpClientRequest[Seq[Device]](HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("namespace" -> ns.get, "deviceId" -> id.show))))
      .flatMap {
        case d +: _ => FastFuture.successful(d)
        case _ => FastFuture.failed(Errors.MissingDevice)
      }

  override def updateDevice(id: Id, device: DeviceT)
                           (implicit ec: ExecutionContext): Request[Unit] = HttpClientRequest {
    Marshal(device).to[MessageEntity].map { entity =>
      HttpRequest(method = PUT, uri = baseUri.withPath(devicesUri.path / id.show), entity = entity)
    }
  }

  override def deleteDevice(id: Id)
                  (implicit ec: ExecutionContext): Request[Unit] =
    HttpClientRequest[Unit](
      HttpRequest(method = DELETE, uri = baseUri.withPath(devicesUri.path / id.show))
    )

  override def updateLastSeen(id: Id, seenAt: Instant = Instant.now)
                             (implicit ec: ExecutionContext): Request[Unit] =
    HttpClientRequest[Unit](HttpRequest(method = POST, uri = baseUri.withPath(devicesUri.path / id.show / "ping")))

  override def updateSystemInfo(id: Id, json: Json)
                              (implicit ec: ExecutionContext): Request[Unit] =
    HttpClientRequest[Unit](HttpRequest(method = PUT,
                                        uri = baseUri.withPath(devicesUri.path / id.show / "system_info"),
      entity = HttpEntity(ContentTypes.`application/json`, json.noSpaces)))

  override def getSystemInfo(id: Id)
                            (implicit ec: ExecutionContext): Request[Json] =
    HttpClientRequest[Json](HttpRequest(method = GET,
                                        uri = baseUri.withPath(devicesUri.path / id.show / "system_info")))
}
