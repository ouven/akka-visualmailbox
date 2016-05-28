package de.aktey.akka.visualmailbox

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import de.aktey.akka.visualmailbox.packing.Packing

/**
  * Created by ruben on 13.05.16.
  */
object VisualMailboxMetricClient extends ExtensionId[VisualMailboxMetricClient] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): VisualMailboxMetricClient = {
    new VisualMailboxMetricClient(
      system,
      VisualMailboxMetricClientConfig.fromConfig(system.settings.config)
    )
  }

  override def lookup(): ExtensionId[_ <: Extension] = VisualMailboxMetricClient
}

class VisualMailboxMetricClient(system: ExtendedActorSystem, config: VisualMailboxMetricClientConfig) extends Extension {
  private val udpSender = system.systemActorOf(
    Props(new UdpSender(config.serverAddress)).withDispatcher("de.aktey.akka.visualmailbox.client.dispatcher"),
    "de-aktey-akka-visualmailbox-sender"
  )
  system.systemActorOf(
    Props(new VisualMailboxMetricListener(udpSender)).withDispatcher("de.aktey.akka.visualmailbox.client.dispatcher"),
    "de-aktey-akka-visualmailbox-receiver"
  )
}

class VisualMailboxMetricListener(udpSender: ActorRef) extends Actor {

  import context._

  import concurrent.duration._

  var buffer: List[VisualMailboxMetric] = Nil

  system.eventStream.subscribe(self, classOf[VisualMailboxMetric])
  system.scheduler.schedule(1.second, 1.second, self, "flush")

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    system.eventStream.unsubscribe(self)
  }

  def receive: Receive = {
    case v: VisualMailboxMetric =>
      buffer ::= v
      if (buffer.size > 40) self ! "flush"

    case "flush" if buffer.nonEmpty =>
      udpSender ! Packing.pack(MetricEnvelope(1, Packing.pack(buffer)))
      buffer = Nil
  }
}

class UdpSender(remote: InetSocketAddress) extends Actor {

  import context._

  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady =>
      context.become(ready(sender()))
  }

  def ready(send: ActorRef): Receive = {
    case msg: Array[Byte] =>
      send ! Udp.Send(ByteString(msg), remote)
  }
}












