package dev.matiaspan.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity

/*
 * We basically need to make sure that users are sharded so that a given
 * room sends the message to wherever the client may be. For this we need
 * to:
 *  1. Have an initialization method for clients that specifies the entity
 *     type key
 *  2. Change spawn to instead of entity ref from the sharding stuff
 *
 * In this scenario we still need to perform a `Join` method in each
 * nodes of the cluster.
 */

object Client {

  sealed trait Command
  case class MessageNotification(from: String, content: String) extends Command with CborSerializable
  case class MessageInput(content: String) extends Command with CborSerializable

  val TypeKey: EntityTypeKey[Client.Command] =
    EntityTypeKey[Client.Command]("Client")

  // initSharding initializes sharding for the client. It receives the
  // actor system it belongs to which is basically a room so we can 
  // treat it as an ActorRef when we send it to apply that manages the
  // messages of this actor
  def initSharding(system: ActorSystem[Nothing], room: ActorRef[Room]): Unit =
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      Client(entityContext.entityId, room)
    })

  def apply(username: String, room: ActorRef[Room]): Behavior[Client.Command] = Behaviors.receive { (context, message) =>
    message match {
      case MessageNotification(from, content) =>
        println(s"[$username] - ${from}: ${content}")
      case MessageInput(content) =>
        room ! NewMessage(username, content)
    }

    Behaviors.same
  }
}
