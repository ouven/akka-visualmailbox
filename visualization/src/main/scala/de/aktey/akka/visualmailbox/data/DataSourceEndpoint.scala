package de.aktey.akka.visualmailbox.data

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Udp.Received
import de.aktey.akka.visualmailbox.packing.Packing
import de.aktey.akka.visualmailbox.{MetricEnvelope, VisualMailboxMetric}

import scala.util.{Failure, Success}

/**
  * Created by ruben on 23.05.16.
  */
class DataSourceEndpoint(router: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case Received(datagram, _) => Packing.unpack[MetricEnvelope](datagram.to[Array]) match {
      case Success(MetricEnvelope(1, payload)) =>
        Packing.unpack[List[VisualMailboxMetric]](payload) match {
          case Success(list) => list.foreach(router ! _)
          case Failure(e) => log.error(e, "unmarshal error")
        }
      case Success(MetricEnvelope(version, _)) => log.warning("unknown protocol version: " + version)
      case Failure(e) => log.error(e, "unmarshal error")
    }
  }
}

object DataSourceEndpoint {
  def props(router: ActorRef) = Props(new DataSourceEndpoint(router))
}
