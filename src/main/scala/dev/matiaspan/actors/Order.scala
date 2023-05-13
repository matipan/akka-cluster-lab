package dev.matiaspan.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.typed.Cluster
import dev.matiaspan.OrderModel

object Order {

  sealed trait Command
  case class UpdateItems(items: Int) extends Command with CborSerializable
  case class Create(id: Int, items: Int, price: Float, userID: Int) extends Command with CborSerializable
  case class Get(id: Int, replyTo: ActorRef[Response]) extends Command with CborSerializable

  sealed trait Response
  case class OrderResponse(order: Option[OrderModel]) extends Response with CborSerializable

  val TypeKey: EntityTypeKey[Order.Command] =
    EntityTypeKey[Order.Command]("Order")

  def initSharding(system: ActorSystem[Nothing]): Unit = {
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      Order(entityContext.entityId)
    })
  }

  def apply(id: String): Behavior[Order.Command] = Behaviors.setup { context => 
    val cluster = Cluster(context.system)

    var state: OrderModel = OrderModel(0,0,0,0)

    println(s"Order lives @ ${cluster.selfMember.address} - ${cluster.selfMember.roles.mkString(",")}")

    Behaviors.receiveMessage { message =>
      message match {
        case UpdateItems(items) =>
          println(s"[${cluster.selfMember.address}] [$id] - Updating items to ${items}")
        case Create(id, items, price, userID) =>
          println(s"[${cluster.selfMember.address}]  [$id] - Creating order with ${items} items, price ${price} and userID ${userID}")

          state = OrderModel(id, items, price, userID)
        case Get(id, replyTo) =>
          println(s"[${cluster.selfMember.address}]  [$id] - Getting order")
          if (state.id == 0) {
            replyTo ! OrderResponse(None)
          } else {
            replyTo ! OrderResponse(Some(state))
          }
      }

      Behaviors.same
    }
  }
}
