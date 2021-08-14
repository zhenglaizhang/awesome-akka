package net.zhenglai.akkhttp

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.util.Random

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

object Auction {
  sealed trait Message

  case class Bid(userId: String, offer: Int) extends Message

  case class Bids(bids: List[Bid])

  case class GetBids(replyTo: ActorRef[Bids]) extends Message

  def apply: Behaviors.Receive[Message] = apply(List.empty)

  def apply(bids: List[Bid]): Behaviors.Receive[Message] = Behaviors.receive {
    case (ctx, bid@Bid(userId, offer)) =>
      ctx.log.info(s"Bid complete: $userId, $offer")
      apply(bids :+ bid)
    case (_, GetBids(replyTo)) =>
      replyTo ! Bids(bids)
      Behaviors.same
  }

  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)
}


object HelloAkkaHttp {

  import Models._
  import Auction._

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
    val auction: ActorRef[Auction.Message] = ActorSystem(Auction.apply, "auction")

    val helloRoute =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http"))
        }
      }

    val numbers = Source.fromIterator(() => Iterator.continually(Random.nextInt()))

    val auctionRoute = path("auction") {
      concat(
        put {
          parameters("bid".as[Int], "user") { (bid, user) =>
            auction ! Bid(user, bid)
            complete(StatusCodes.Accepted, "bid placed")
          }
        },
        get {
          implicit val timeout: Timeout = 5.seconds
          val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
          complete(bids)
        }
      )
    }

    // One of the strengths of Akka HTTP is that streaming data is at its heart meaning that both request and response bodies can be streamed through the server achieving constant memory usage even for very large requests or responses.
    // Streaming responses will be backpressured by the remote client so that the server will not push data faster than the client can handle, streaming requests means that the server decides how fast the remote client can push the data of the request body.
    val numberRoute = path("random") {
      get {
        complete(HttpEntity(
          ContentTypes.`text/plain(UTF-8)`,
          numbers.map(n => ByteString(s"$n\n"))
        ))
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

    val route = concat(helloRoute, numberRoute, orderRoute)
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println("Server new online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
