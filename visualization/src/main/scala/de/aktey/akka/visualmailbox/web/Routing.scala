package de.aktey.akka.visualmailbox.web

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent

import scala.concurrent.duration._

/**
  * Created by ruben on 23.05.16.
  */
object Routing {
  def api(metricFlow: Source[String, ActorRef]): Route = pathPrefix("api") {
    import de.heikoseeberger.akkasse.EventStreamMarshalling._

    path("events") {
      complete {
        metricFlow
          .map(s => ServerSentEvent(s, "vmm"))
          .keepAlive(20.seconds, () => ServerSentEvent.heartbeat)
      }
    }
  }

  val static: Route = pathEndOrSingleSlash {
    getFromResource("web/index.html")
  } ~ getFromResourceDirectory("web")

  def root(metricFlow: Source[String, ActorRef]): Route = api(metricFlow) ~ static
}
