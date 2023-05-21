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
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect

object Order {

  final case class State(var order: OrderModel) extends CborSerializable {
    def createOrder(newOrder: OrderModel): State = {
      println(s"Creating brand new order: $newOrder")
      copy(order = newOrder)
    }

    def updateOrder(items: Int, price: Float): State = {
      println(s"Updating order with: $items - $price")
      copy(order = OrderModel(order.id, order.items + items, order.price + price, order.userID))
    }
  }
  object State {
    val empty = State(OrderModel(0,0,0,0))
  }

  sealed trait Command extends CborSerializable
  case class AddItems(items: Int, price: Float, replyTo: ActorRef[Response]) extends Command
  case class Create(id: Int, items: Int, price: Float, userID: Int, replyTo: ActorRef[Response]) extends Command
  case class Get(replyTo: ActorRef[Response]) extends Command

  sealed trait Response extends CborSerializable
  case class OrderResponse(order: Option[OrderModel]) extends Response
  case class OrderUpdateConfirmed(order: OrderModel) extends Response
  case class OrderUpdateRejected(reason: String) extends Response

  sealed trait Event extends CborSerializable
  case class OrderCreated(order: OrderModel) extends Event
  case class OrderUpdated(items: Int, price: Float) extends Event

  val TypeKey: EntityTypeKey[Order.Command] =
    EntityTypeKey[Order.Command]("Order")

  def initSharding(system: ActorSystem[Nothing]): Unit = {
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      Order(entityContext.entityId)
    })
  }

  def handleEvent(state: State, event: Event): State = {
    event match {
      case OrderCreated(order: OrderModel) => state.createOrder(order)
      case OrderUpdated(items: Int, price: Float) => state.updateOrder(items, price)
    }
  }

  def handleCommand(state: State, command: Command): Effect[Event, State] = {
    command match {
      case AddItems(items: Int, price: Float, replyTo: ActorRef[Response]) =>
        println(s"Received update request with $items-$price. Current state: ${state.order}")
        if (state.order.id == 0) {
          replyTo ! OrderUpdateRejected("order does not exist")
          Effect.none
        } else if (items < 1 || (state.order.price + price < state.order.price)) {
          replyTo ! OrderUpdateRejected("items and price should always be incremental")
          Effect.none
        } else {
          Effect
            .persist(OrderUpdated(items, price))
            .thenRun(orderUpdated => replyTo ! OrderUpdateConfirmed(orderUpdated.order))
        }
      case Create(id, items, price, userID, replyTo) =>
        // if the ID of the order if different than 0 it means the order
        // already exists so we should not do anything and simply return
        // the existing order
        // if not we persist the event and return the newly created order
        println(s"Received create request with $id-$items-$price-$userID. Current state: ${state.order}")
        if (state.order.id != 0) {
          replyTo ! OrderResponse(Some(state.order))
          Effect.none
        } else {
          Effect
            .persist(OrderCreated(OrderModel(id, items, price, userID)))
            .thenRun(orderCreated => replyTo ! OrderResponse(Some(orderCreated.order)))
        }
      case Get(replyTo) =>
        println(s"Received get request. Current state: ${state.order}")
        // send back none if the order does not exist or the order otherwise
        if (state.order.id == 0) {
          replyTo ! OrderResponse(None)
        } else {
          replyTo ! OrderResponse(Some(state.order))
        }
        Effect.none
    }
  }

  def apply(id: String): Behavior[Order.Command] = {
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("Order", id),
      State.empty,
      (state, command) => handleCommand(state, command),
      (state, event) => handleEvent(state, event))
  }
}
