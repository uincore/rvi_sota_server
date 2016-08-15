/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import java.util.UUID

import org.genivi.sota.data.{Device, DeviceT, Namespace}
import org.genivi.sota.db.Operators.regex
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.refined.SlickRefined._
import java.time.Instant

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object DeviceRepository {
  import Device._
  import org.genivi.sota.db.SlickExtensions._

  // TODO generalize
  implicit val deviceNameColumnType =
    MappedColumnType.base[DeviceName, String](
      { case DeviceName(value) => value.toString }, DeviceName
    )

  implicit val deviceIdColumnType =
    MappedColumnType.base[DeviceId, String](
      { case DeviceId(value) => value.toString }, DeviceId
    )

  // scalastyle:off
  class DeviceTable(tag: Tag) extends Table[Device](tag, "Device") {
    def namespace = column[Namespace]("namespace")
    def id = column[Id]("uuid")
    def deviceName = column[Option[DeviceName]]("device_name")
    def deviceId = column[Option[DeviceId]]("device_id")
    def deviceType = column[DeviceType]("device_type")
    def lastSeen = column[Option[Instant]]("last_seen")

    def * = (namespace, id, deviceName, deviceId, deviceType, lastSeen).shaped <>
      ((Device.apply _).tupled, Device.unapply)

    def pk = primaryKey("id", id)
  }

  // scalastyle:on
  private val devices = TableQuery[DeviceTable]

  def list(ns: Namespace): DBIO[Seq[Device]] = devices.filter(_.namespace === ns).result

  def create(ns: Namespace, device: DeviceT)
             (implicit ec: ExecutionContext): DBIO[Id] = {
    val id: Id = Id(refineV[ValidId](UUID.randomUUID.toString).right.get)

    (devices += Device(ns, id, device.deviceName, device.deviceId, device.deviceType))
      .handleIntegrityErrors(Errors.ConflictingDevice)
      .map(_ => id)
  }

  def exists(ns: Namespace, id: Id)
            (implicit ec: ExecutionContext): DBIO[Device] =
    devices
      .filter(d => d.namespace === ns && d.id === id)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Device]](DBIO.failed(Errors.MissingDevice))(DBIO.successful))

  def findByDeviceId(ns: Namespace, deviceId: DeviceId)
                    (implicit ec: ExecutionContext): DBIO[Seq[Device]] =
    devices
      .filter(d => d.namespace === ns && d.deviceId === deviceId)
      .result

  def search(ns: Namespace, re: String Refined Regex): DBIO[Seq[Device]] =
    devices
      .filter(d => d.namespace === ns && regex(d.deviceName, re))
      .result

  def update(ns: Namespace, id: Id, device: DeviceT)
            (implicit ec: ExecutionContext): DBIO[Unit] = {

    val dbIO = for {
      _ <- exists(ns, id)
      _ <- devices
        .update(Device(ns, id, device.deviceName, device.deviceId, device.deviceType))
        .handleIntegrityErrors(Errors.ConflictingDevice)
    } yield ()

    dbIO.transactionally
  }

  def findById(id: Device.Id)(implicit ec: ExecutionContext): DBIO[Device] = {
    devices
      .filter(_.id === id)
      .result
      .headOption
      .flatMap(_.fold[DBIO[Device]](DBIO.failed(Errors.MissingDevice))(DBIO.successful))
  }

  def updateLastSeen(id: Id)
                    (implicit ec: ExecutionContext): DBIO[Unit] = for {
    device <- findById(id)
    newDevice = device.copy(lastSeen = Some(Instant.now()))
    _ <- devices.insertOrUpdate(newDevice)
  } yield ()

  def delete(ns: Namespace, id: Id)
            (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      _ <- exists(ns, id)
      _ <- devices.filter(d => d.namespace === ns && d.id === id).delete
      _ <- SystemInfo.delete(id)
    } yield ()

    dbIO.transactionally
  }
}
