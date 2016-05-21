package de.aktey.akka.visualmailbox

import java.net.InetSocketAddress

import com.typesafe.config.Config

/**
  * Created by ruben on 21.05.16.
  */
case class VisualMailboxMetricClientConfig(serverAddress: InetSocketAddress)

object VisualMailboxMetricClientConfig {
  def fromConfig(config: Config) = VisualMailboxMetricClientConfig(
    serverAddress = new InetSocketAddress(
      config.getString("de.aktey.akka.visualmailbox.server.address.host"),
      config.getInt("de.aktey.akka.visualmailbox.server.address.port")
    )
  )
}