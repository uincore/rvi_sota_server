package org.genivi.sota.device_registry

import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri.Query
import cats.syntax.show._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.Uuid
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import scala.concurrent.ExecutionContext


trait GroupRequests {
  self: ResourceSpec =>

  val groupsApi = "device_groups"

  def createGroupFromDevices(device1: Uuid, device2: Uuid, groupName: Name)
                            (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri("device_groups", "from_attributes"), CreateGroupRequest(device1, device2, groupName).asJson)

  def listDevicesInGroup(groupId: Uuid)(implicit ec: ExecutionContext): HttpRequest =
    Get(Resource.uri("device_groups", groupId.underlying.get, "devices"))

  def countDevicesInGroup(groupId: Uuid)(implicit ec: ExecutionContext): HttpRequest =
    Get(Resource.uri("device_groups", groupId.underlying.get, "count"))

  def listGroups(): HttpRequest = Get(Resource.uri(groupsApi))

  def fetchGroupInfo(id: Uuid): HttpRequest = Get(Resource.uri(groupsApi, id.underlying.get))

  def createGroupInfo(id: Uuid, groupName: Name, json: Json)
                     (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(groupsApi, id.underlying.get).withQuery(Query("groupName" -> groupName.get)), json)

  def createGroupInfoOk(id: Uuid, groupName: Name, json: Json) =
    createGroupInfo(id, groupName, json) ~> route ~> check {
      status shouldBe Created
    }

  def updateGroupInfo(id: Uuid, groupName: Name, json: Json)
                     (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(groupsApi, id.underlying.get).withQuery(Query("groupName" -> groupName.get)), json)

  def renameGroup(id: Uuid, newGroupName: Name)(implicit ec: ExecutionContext): HttpRequest = {
    Put(Resource.uri(groupsApi, id.underlying.get, "rename").withQuery(Query("groupName" -> newGroupName.get)))
  }
}