package de.aktey.akka.visualmailbox

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch._
import com.typesafe.config.Config

class VisualMailboxType() extends MailboxType with ProducesMessageQueue[VisualMailbox] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new VisualMailbox(UnboundedMailbox().create(owner, system), owner, system)
      with UnboundedMessageQueueSemantics
      with MultipleConsumerSemantics
}


/**
  * Created by ruben on 09.05.16.
  */
class VisualMailbox(val backend: MessageQueue, owner: Option[ActorRef], system: Option[ActorSystem]) extends MessageQueue {

  override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    val metric = VisualMailboxMetric(
      handle.sender.path.toSerializationFormat,
      receiver.path.toSerializationFormat,
      backend.numberOfMessages
    )
    system.foreach(_.eventStream.publish(metric))
    backend.enqueue(receiver, handle)
  }

  override def dequeue(): Envelope = backend.dequeue()

  override def numberOfMessages: Int = backend.numberOfMessages

  override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = backend.cleanUp(owner, deadLetters)

  override def hasMessages: Boolean = backend.hasMessages
}


