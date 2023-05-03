package dev.matiaspan.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef

sealed trait Message
case class MessageNotification(from: String, content: String) extends Message
case class MessageInput(content: String) extends Message

object Client {
  def apply(username: String, room: ActorRef[Room]): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case MessageNotification(from, content) =>
        println(s"${from}: ${content}")
      case MessageInput(content) =>
        room ! NewMessage(username, content)
    }

    Behaviors.same
  }
}
