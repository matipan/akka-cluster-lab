package dev.matiaspan

import akka.actor.typed.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import dev.matiaspan.actors._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.server.Route
import akka.actor.typed.ActorRef

object Main extends App {
  implicit val system: ActorSystem[Room] = ActorSystem(Room(), "chat")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val room: ActorRef[Room] = system

  val routes: Route = concat(
    post {
      path("message") {
        parameters("username", "content") { (username, content) =>
          room ! NewMessage(username, content)
          complete("message sent")
        }
      }
    },
    post {
      path("join") {
        parameters("username") { username =>
          room ! Join(username)
          complete("user joined")
        }
      }
    },
    post {
      path("leave") {
        parameters("username") { username =>
          room ! Leave(username)
          complete("user joined")
        }
      }
    }
  )

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  bindingFuture.foreach { binding =>
    println(s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
  }
}
