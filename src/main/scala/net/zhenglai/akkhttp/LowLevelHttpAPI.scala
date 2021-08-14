package net.zhenglai.akkhttp

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object LowLevelHttpAPI {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "lowlevel")
    implicit val ec: ExecutionContext = system.executionContext

    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
        HttpResponse(entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "<html><body>hello world</body></html>"
        ))
      case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
        HttpResponse(entity = "PONG")
      case HttpRequest(GET, Uri.Path("/crash"), _, _, _) =>
        sys.error("BOOM")
      case r: HttpRequest =>
        r.discardEntityBytes() // important to drain incoming http entity stream
        HttpResponse(StatusCodes.NotFound, entity = "Unknown resource!")
    }

    val bindingF = Http().newServerAt("localhost", 8080).bindSync(requestHandler)
    println(s"Server online now\nPress RETURN to stop")
    StdIn.readLine()
    bindingF.flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}
