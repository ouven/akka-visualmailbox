package de.aktey.akka.visualmailbox

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.io.Udp.{Bind, Bound, CommandFailed}
import akka.io.{IO, Udp}
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.aktey.akka.visualmailbox.data.DataSourceEndpoint
import de.aktey.akka.visualmailbox.web.{Routing, WebConfig}

import scala.concurrent.duration._

/**
  * Created by ruben on 10.05.16.
  */
object VisualMailboxMetricServer extends App {

  val allConfig = ConfigFactory.load()
  val config = VisualMailboxMetricClientConfig.fromConfig(allConfig)

  implicit val system = ActorSystem("visualmailbox-visualizer")
  implicit val meterializer = ActorMaterializer()
  implicit val bindTimeout = Timeout(2.seconds)

  import system._

  val router = system.actorOf(MetricsRouter.props(), "router")

  val dataHandler = system.actorOf(DataSourceEndpoint.props(router), "data-sink")

  (IO(Udp) ? Bind(dataHandler, config.serverAddress)).map {
    case CommandFailed(cmd) =>
      system.terminate()
    case Bound(address) =>
      log.info(s"""{"type":"udp-bound","address":"$address"}""")
  }

  val webConfig = WebConfig.fromConfig(allConfig)

  Http()
    .bindAndHandle(Routing.root(MetricFlow.metricSource(router)), webConfig.host, webConfig.port)
    .foreach { case ServerBinding(address) =>
      log.info(s"""{"type":"http-bound","address":"$address"}""")
    }
}
