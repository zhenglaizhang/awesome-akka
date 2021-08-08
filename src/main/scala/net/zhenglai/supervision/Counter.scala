package net.zhenglai.supervision

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}

object Counter {
  sealed trait Command
  case class Increment(nr: Int) extends Command
  case class GetCount(replyTo: ActorRef[Int]) extends Command

  def apply(): Behavior[Command] =
    Behaviors
      .supervise(Behaviors.supervise(counter(1)).onFailure(SupervisorStrategy.restart))
      .onFailure[IllegalStateException](SupervisorStrategy.stop)

  private def counter(count: Int): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case Increment(nr) =>
        counter((count + nr))
      case GetCount(replyTo) =>
        replyTo ! count
        Behaviors.same
    }
}
