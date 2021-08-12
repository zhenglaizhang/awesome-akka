package net.zhenglai.akk.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{IOResult, OverflowStrategy}
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString

import java.nio.file.Paths
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object ReusablePieces {
  // Sink[String, Future[IOResult]], which means that it accepts strings as its input and when materialized it will create auxiliary information of type Future[IOResult]
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  def count[A]: Flow[A, Int, NotUsed] = Flow[A].map(_ => 1)

  def sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
}

object HelloStream extends App {

  import ReusablePieces._

  implicit val system: ActorSystem = ActorSystem("HelloStream")
  implicit val ec = system.dispatcher
  val source: Source[Int, NotUsed] =
    Source(1 to 1000)
      .filterNot(_ > 200)
      .map(_.toString)
      .map(_.toIntOption)
      .mapConcat(identity)
  val done: Future[Done] =
    source.runWith(Sink.foreach(println))
  // source.runForeach(println)


  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
  val slowComputation = identity[String]
  val result: Future[IOResult] = factorials
    .zipWith(Source(0 to 200))((num, idx) => s"idx! = $num")
    .throttle(1, 1.second)
    .buffer(10, OverflowStrategy.dropHead)
    .map(slowComputation)
    .map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("/tmp", "factorials.txt")))
  val result2: Future[IOResult] = factorials.map(_.toString).runWith(lineSink("/tmp/factorials2.txt"))

  Future.sequence(Seq(result, result2))
    .onComplete(_ => system.terminate())
}

object HelloGraph extends App {
  implicit val system: ActorSystem = ActorSystem("HelloGraph")
  implicit val ec = system.dispatcher
  val writeInts: Sink[Int, NotUsed] = Flow[Int].toMat(Sink.foreach(println))(Keep.left)
  val writeStrings: Sink[String, NotUsed] = Flow[String].toMat(Sink.foreach(println))(Keep.left)
  val g = RunnableGraph.fromGraph(GraphDSL.create()) { implicit b =>
    import GraphDSL.Implicits._
    val bcast = b.add
  }
}