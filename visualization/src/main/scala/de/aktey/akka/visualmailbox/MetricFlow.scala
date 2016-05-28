package de.aktey.akka.visualmailbox

import akka.actor.{ActorRef, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source

import scala.annotation.tailrec

object MetricFlow {

  // subscriber as flow source
  // that registrates itself to a router
  class MetricsSubscriber(router: ActorRef) extends ActorPublisher[VisualMailboxMetric] {

    import akka.stream.actor.ActorPublisherMessage._

    val MaxBufferSize = 100
    var buf = Vector.empty[VisualMailboxMetric]

    router ! self

    def receive = {
      case metric: VisualMailboxMetric if buf.size == MaxBufferSize =>
      case metric: VisualMailboxMetric =>
        if (buf.isEmpty && totalDemand > 0)
          onNext(metric)
        else {
          buf :+= metric
          deliverBuf()
        }
      case Request(_) =>
        deliverBuf()
      case Cancel =>
        context.stop(self)
    }

    @tailrec
    private def deliverBuf(): Unit =
      if (totalDemand > 0) {
        if (totalDemand <= Int.MaxValue) {
          val (use, keep) = buf.splitAt(totalDemand.toInt)
          buf = keep
          use foreach onNext
        } else {
          val (use, keep) = buf.splitAt(Int.MaxValue)
          buf = keep
          use foreach onNext
          deliverBuf()
        }
      }
  }

  object MetricsSubscriber {
    def props(router: ActorRef) = Props(new MetricsSubscriber(router))
  }

  def metricSource(router: ActorRef): Source[String, ActorRef] =
    Source.actorPublisher[VisualMailboxMetric](MetricsSubscriber.props(router)).map {
      case VisualMailboxMetric(sender, receiver, receiverMailBoxSize, meassureTimeMillies) =>
        s"""{
            |  "sender": "$sender",
            |  "receiver": "$receiver",
            |  "receiverMailBoxSize": $receiverMailBoxSize,
            |  "meassureTimeMillies": $meassureTimeMillies
            |}""".stripMargin
    }
}
