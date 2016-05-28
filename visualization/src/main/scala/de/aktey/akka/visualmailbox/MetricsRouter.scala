package de.aktey.akka.visualmailbox

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

class MetricsRouter extends Actor with ActorLogging {

  import context._

  var routees: Set[ActorRef] = Set.empty

  override def postStop() {
    routees foreach unwatch
  }

  def receive = {
    case registrar: ActorRef =>
      watch(registrar)
      routees += registrar
      if (log.isDebugEnabled) log.debug(s"""{"type":"registerd","registered":"$registrar","routees":${routees.size}}""")
    case Terminated(ref) =>
      unwatch(ref)
      routees -= ref
      if (log.isDebugEnabled) log.debug(s"""{"type":"unregistered","terminated":"$ref","routees":${routees.size}}""")
    case msg =>
      routees foreach (_ forward msg)
  }
}

object MetricsRouter {
  def props() = Props(new MetricsRouter)
}
