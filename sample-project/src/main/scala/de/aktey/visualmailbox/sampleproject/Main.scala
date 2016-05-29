package de.aktey.visualmailbox.sampleproject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.concurrent.duration._

/**
  * Created by ruben on 29.05.16.
  */
object Main extends App {

  val system = ActorSystem("visualmailbox-sample")

  system.actorOf(Props(new Root(7)), "root")
}

class Root(leafs: Int) extends Actor {

  import context._

  def childName(i: Int) = s"node-$i"

  (0 until leafs).foreach { i => context.actorOf(Props(new Node(2)), childName(i)) }
  (0 until leafs).foreach { i =>
    child(childName(i)).foreach {
      s => s ! child(childName((i + 1) % leafs))
    }
  }

  system.scheduler.scheduleOnce(5.seconds) {
    child(childName(0)).foreach(_ ! "tack")
    (1 until leafs).flatMap(i => child(childName(i))).foreach(_ ! "")
  }

  def receive = {
    case "tick" => sender ! ""
  }
}

class Node(leafs: Int) extends Actor {

  import context.{dispatcher, _}

  var partner: Option[ActorRef] = None

  def scheduleTack(d: FiniteDuration) = partner.foreach { p =>
    system.scheduler.scheduleOnce(d, p, "tack")
  }

  def childName(i: Int) = s"node-$i"

  (0 until leafs).foreach { i => context.actorOf(Props(new Leaf()), childName(i)) }

  def receive = {
    case "tack" =>
      scheduleTack(2.seconds)
      parent ! "tick"
      children.foreach(_ ! "foo")
    case p: Option[ActorRef] => partner = p
  }
}

class Leaf() extends Actor {

  def receive = {
    case "foo" =>
  }
}
