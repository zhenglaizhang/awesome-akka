package net.zhenglai.akkhttp

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

// curl http://localhost:8080/hello
// curl -H "Content-Type: application/json" -X POST -d '{"items":[{"name":"hhgtg","id":42}]}' http://localhost:8080/create-order
// curl http://localhost:8080/item/42

object Models {
  final case class Item(name: String, id: Long)

  final case class Order(items: List[Item])

  // formats for unmarshalling and marshalling
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)
}


object HelloAkkaHttp {

  import Models._

  var orders: List[Item] = Nil

  def fetchItem(itemId: Long)(implicit ec: ExecutionContext): Future[Option[Item]] = Future {
    orders.find(_.id == itemId)
  }

  def saveOrder(order: Order): Future[Done] = {
    order match {
      case Order(items) => items ::: orders
      case _ => orders
    }
    Future.successful(Done)
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "my-system")
    implicit val ec = system.executionContext

    val helloRoute =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http"))
        }
      }

    val orderRoute: Route = concat(
      get {
        pathPrefix("item" / LongNumber) { id =>
          val maybeItem: Future[Option[Item]] = fetchItem(id)
          onSuccess(maybeItem) {
            case Some(item) => complete(item)
            case None => complete(StatusCodes.NotFound)
          }
        }
      },
      post {
        path("create-order") {
          entity(as[Order]) { order =>
            val saved: Future[Done] = saveOrder(order)
            onSuccess(saved) { _ => complete("Order created") }
          }
        }
      }
    )

    val route = concat(helloRoute, orderRoute)
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println("Server new online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
