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
  var buffer: List[VisualMailboxMetric] = Nil

  context.system.eventStream.subscribe(self, classOf[VisualMailboxMetric])

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = context.system.eventStream.unsubscribe(self)

  def receive: Receive = {
    case v: VisualMailboxMetric =>
      buffer ::= v
      if (buffer.size > 40) self ! "flush"

    case "flush" =>
      udpSender ! Packing.pack(buffer)
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












