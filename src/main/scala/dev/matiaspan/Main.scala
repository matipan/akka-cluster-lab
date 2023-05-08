package dev.matiaspan

import akka.stream.ActorMaterializer

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor

import akka.actor.typed._
import akka.actor.typed.scaladsl._

import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.cluster.sharding.typed.scaladsl.EntityRef

import dev.matiaspan.actors._

import com.typesafe.config.ConfigFactory
import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap

object Main extends App {
  val config = ConfigFactory.load()

  implicit val system: ActorSystem[Room] = ActorSystem(Room(), "chat")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val sharding = ClusterSharding(system)

  // Akka Management hosts the HTTP routes used by bootstrap
  AkkaManagement.get(system).start();

  // Starting the bootstrap process needs to be done explicitly
  ClusterBootstrap.get(system).start();

  val room: ActorRef[Room] = system

  val routes: Route = concat(
    post {
      path("message") {
        parameters("username", "content") { (username, content) =>
          room ! actors.NewMessage(username, content)
          complete("message sent")
        }
      }
    },
    post {
      path("join") {
        parameters("username") { username =>
          room ! actors.Join(username)
          complete("user joined")
        }
      }
    },
    post {
      path("leave") {
        parameters("username") { username =>
          room ! actors.Leave(username)
          complete("user joined")
        }
      }
    }
    )

  val port = config.getInt("chat.http.port")
  val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", port)

  bindingFuture.foreach { binding =>
    println(s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
  }
}
