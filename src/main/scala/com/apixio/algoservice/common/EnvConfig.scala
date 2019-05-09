package com.apixio.algoservice.common

import java.util

import com.typesafe.config.{Config, ConfigFactory}

object EnvConfig {
  lazy val config: Config = ConfigFactory.load()
  lazy val appConfig: Config = config.getConfig("algo-svc")
  lazy val daos = appConfig.getConfig("daos").root.unwrapped()
  lazy val ensembleProperties: util.Map[String, AnyRef] = appConfig.getConfig("ensembleProperties").root().unwrapped()
  lazy val jdbcConfig: Config = appConfig.getConfig("daos").getConfig("persistenceConfig").getConfig("jdbc_signal_control")
}
