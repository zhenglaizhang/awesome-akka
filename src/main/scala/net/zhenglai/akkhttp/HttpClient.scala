package net.zhenglai.akkhttp

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success}

// connection pools to allow multiple requests to the same server to be handled more performantly by re-using TCP connections to the server
object HttpClient {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
    implicit val ec = system.executionContext
    val responseF: Future[HttpResponse] = Http().singleRequest((HttpRequest(uri = "https://akka.io")))
    responseF.onComplete {
      case Success(res) => println(res.entity)
      case Failure(ex) => sys.error(s"error: $ex")
    }
  }
}
