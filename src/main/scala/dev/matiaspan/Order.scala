package dev.matiaspan

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

import spray.json._, DefaultJsonProtocol._

final case class OrderModel(id: Int, var items: Int, var price: Float, userID: Int)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val orderModelFormat = jsonFormat4(OrderModel.apply)
}
