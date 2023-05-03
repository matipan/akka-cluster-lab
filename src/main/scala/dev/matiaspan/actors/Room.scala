package dev.matiaspan.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef

sealed trait Room
case class Join(username: String) extends Room
case class Leave(username: String) extends Room
case class NewMessage(username: String, content: String) extends Room

object Room {
  def apply(): Behavior[Room] = Behaviors.setup { context =>
    var users: Map[String, ActorRef[Message]] = Map.empty[String, ActorRef[Message]]

    Behaviors.receiveMessage { 
      case Join(username) =>
        val user = context.spawn(Client(username, context.self), username)
        users += (username -> user)
        Behaviors.same
      case Leave(username) =>
        users = users.removed(username)
        Behaviors.same
      case NewMessage(username, content) =>
        users.find(user => user._1 == username) match {
          // iterate over all users and send them a message notification
          // ignoring the user that actually sent the message
          case Some(user) => users.filter(user => user._1 != username).foreach(user => user._2 ! MessageNotification(username, content))
          case None => println(s"user $username is not a part of the room")
        }
        Behaviors.same
    }
  }
}
