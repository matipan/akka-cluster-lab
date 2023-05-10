package dev.matiaspan

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class OrderModel(id: Int, items: Int, price: Float, userID: Int) extends JsonSupport

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val orderModelFormat = jsonFormat4(OrderModel)
}
