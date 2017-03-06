/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.client

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.data.Namespace
import com.advancedtelematic.libtuf.data.TufDataType.{Checksum, RepoId}
import com.advancedtelematic.libtuf.reposerver._
import java.util.UUID
import scala.concurrent.Future

object FakeReposerverClient extends ReposerverClient {

  private var store: Map[Namespace, RepoId] = Map.empty

  def createRoot(namespace: Namespace): Future[RepoId] = {
    val repoId = RepoId(UUID.randomUUID())
    store = store + (namespace -> repoId)
    FastFuture.successful(repoId)
  }

  def addTarget(namespace: Namespace,
                fileName: String,
                uri: Uri,
                checksum: Checksum,
                length: Int): Future[Unit] =
    store.get(namespace) match {
      case Some(_) => Future.successful(())
      case None    => Future.failed(new Exception("not authorized"))
    }

}
