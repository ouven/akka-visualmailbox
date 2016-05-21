package de.aktey.akk.visualmailbox

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.io.Udp.{Bind, CommandFailed, Received}
import akka.io.{IO, Udp}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.aktey.akka.visualmailbox.{VisualMailboxMetric, VisualMailboxMetricClientConfig}
import de.aktey.akka.visualmailbox.packing.Packing

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by ruben on 10.05.16.
  */
object VisualMailboxMetricServer extends App {

  val config = VisualMailboxMetricClientConfig.fromConfig(ConfigFactory.load())

  implicit val system = ActorSystem("visualizer")
  implicit val bindTimeout = Timeout(2.seconds)

  import system._

  val handler = system.actorOf(Props(new Actor with ActorLogging {
    def receive = {
      case Received(datagram, _) =>
        Packing.unpack[List[VisualMailboxMetric]](datagram.to[Array]) match {
          case Success(list) => list.foreach(println)
          case Failure(e) => log.error(e, "unmarshal error")
        }
    }
  }))

  (IO(Udp) ? Bind(handler, config.serverAddress)).map {
    case CommandFailed(cmd) => system.terminate()
  }
}
