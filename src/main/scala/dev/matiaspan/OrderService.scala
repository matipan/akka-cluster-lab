package dev.matiaspan

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import actors.Order

import scala.concurrent.duration._

import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import scala.concurrent._
import scala.util.Success
import scala.util.Failure

class OrderService(actorSystem: ActorSystem[Nothing]) {

  implicit val system = actorSystem
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val timeout: Timeout = 3.seconds

  val sharding = ClusterSharding(system)

  def createOrder(order: OrderModel): Future[Option[OrderModel]] = {
    val actor = sharding.entityRefFor(Order.TypeKey, order.id.toString)

    actor.ask[Order.Response](ref => Order.Create(order.id, order.items, order.price, order.userID, ref)).map {
      case Order.OrderResponse(order) => order
    }.recover {
      case _ => None
    }
  }

  def addItems(id: Int, items: Int, price: Float): Future[Option[String]] = {
    val actor = sharding.entityRefFor(Order.TypeKey, id.toString)

    actor.ask[Order.Response](ref => Order.AddItems(items, price, ref)).map {
      case Order.OrderUpdateConfirmed(_) => None
      case Order.OrderUpdateRejected(reason) => Some(reason)
    }.recover {
      case _ => None
    }
  }

  def getOrder(id: Int): Future[Option[OrderModel]] = {
    val order = sharding.entityRefFor(Order.TypeKey, id.toString)

    order.ask[Order.Response](ref => Order.Get(ref)).map {
      case Order.OrderResponse(order) => order
    }.recover {
      case _ => None
    }
  }
}
