package de.aktey.akka.visualmailbox.web

import com.typesafe.config.Config

/**
  * Created by jamesanto on 8/29/16.
  */
case class WebConfig(host: String, port: Int)

object WebConfig {

  private val pathHost = "de.aktey.akka.visualmailbox.web.server.address.host"
  private val pathPort = "de.aktey.akka.visualmailbox.web.server.address.port"

  def fromConfig(config: Config): WebConfig = {
    val host = if(config.hasPath(pathPort)) config.getString(pathHost) else "0.0.0.0"
    val port = if(config.hasPath(pathPort)) config.getInt(pathPort) else 8080
    WebConfig(host, port)
  }
}
