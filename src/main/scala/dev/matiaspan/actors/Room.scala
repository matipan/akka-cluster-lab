package dev.matiaspan.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.actor.typed.ActorSystem

sealed trait Room
case class Join(username: String) extends Room
case class Leave(username: String) extends Room
case class NewMessage(username: String, content: String) extends Room

object Room {

  def apply(): Behavior[Room] = Behaviors.setup { context =>
    val sharding = ClusterSharding(context.system)
    Client.initSharding(context.system, context.self)

    def getClient(username: String): EntityRef[Client.Command] = {
      sharding.entityRefFor(Client.TypeKey, username)
    }

    var users: Map[String, EntityRef[Client.Command]] = Map.empty[String, EntityRef[Client.Command]]

    Behaviors.receiveMessage { 
      case Join(username) =>
        users.find(user => user._1 == username) match {
          case Some(_) => println(s"$username already exists")
          case None => 
            println(s"adding $username to the room")
            val user = getClient(username)

            users += (username -> user)
        }

        Behaviors.same
      case Leave(username) =>
        users.find(user => user._1 == username) match {
          case Some(_) =>
            users = users.removed(username)
            println(s"$username left")
          case None => println(s"$username is not in the room")
        }

        Behaviors.same
      case NewMessage(username, content) =>
        users.find(user => user._1 == username) match {
          // iterate over all users and send them a message notification
          // ignoring the user that actually sent the message
          case Some(user) => users.filter(user => user._1 != username).foreach(user => user._2 ! Client.MessageNotification(username, content))
          case None => println(s"user $username is not a part of the room")
        }

        Behaviors.same
    }
  }
}
