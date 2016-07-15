/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.db

import com.typesafe.config.{Config, ConfigFactory}
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

trait BootMigrations {
  private val _migrateConfig = ConfigFactory.load()

  private val _migrateLog = LoggerFactory.getLogger(this.getClass)

  // Database migrations
  if (_migrateConfig.getBoolean("database.migrate")) {
    _migrateLog.info("Running migrations")

    val url = _migrateConfig.getString("database.url")
    val user = _migrateConfig.getString("database.properties.user")
    val password = _migrateConfig.getString("database.properties.password")

    val flyway = new Flyway
    flyway.setDataSource(url, user, password)
    val count = flyway.migrate()

    _migrateLog.info(s"Ran $count migrations")
  }
}
