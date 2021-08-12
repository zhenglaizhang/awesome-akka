package net.zhenglai

import akka.Done
import akka.actor.typed.ActorRef

import scala.concurrent.Future

trait DB {
  def save(id: String, value: String): Future[Done]

  def load(id: String): Future[String]
}

object DataAccess {
  sealed trait Command

  final case class Save(value: String, replyTo: ActorRef[Done]) extends Command
}

object Stash {

}
