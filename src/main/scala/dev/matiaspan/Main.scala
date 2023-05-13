package dev.matiaspan

import akka.stream.ActorMaterializer

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

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
import akka.http.scaladsl.server.Directives
import scala.concurrent.Await
import akka.rollingupdate.kubernetes.PodDeletionCost

object Main extends App {
  val config = ConfigFactory.load()

  implicit val system = ActorSystem[Nothing](Behaviors.empty, "OrderService")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  AkkaManagement.get(system).start();
  ClusterBootstrap.get(system).start();

  PodDeletionCost(system).start()

  val sharding = ClusterSharding(system)
  Order.initSharding(system)

  val orderService: OrderService = new OrderService(system)
  val routes = new Routes(orderService, system, executionContext).routes

  val port = config.getInt("chat.http.port")
  val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", port)

  bindingFuture.foreach { binding =>
    println(s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
  }
}

class Routes(orderService: OrderService, implicit val system: ActorSystem[Nothing], implicit val executionContext: ExecutionContextExecutor) extends Directives with JsonSupport  {
  val routes: Route = pathPrefix("orders") {
    concat(
      post {
        parameters("id","items","price","userID") { (id, items, price, userID) =>
          val order = orderService.createOrder(new OrderModel(id.toInt, items.toInt, price.toFloat, userID.toInt))

          Await.result(order, scala.concurrent.duration.Duration.Inf) match {
            case Some(order) => complete(200, order)
            case None => complete(400, "order already exists")
          }
        }
      },
      path(IntNumber) { id =>
        get {
          val order = orderService.getOrder(id)

          Await.result(order, scala.concurrent.duration.Duration.Inf) match {
            case Some(order) => complete(200, order)
            case None => complete(404, "order not found")
          }
        }
      }
      )
  }
}
