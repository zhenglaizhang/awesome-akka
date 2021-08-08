package net.zhenglai.interactionpatterns

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.FiniteDuration

object Buncher {
  sealed trait Command
  final case class ExcitingMessage(message: String) extends Command
  final case class Batch(messages: Vector[Command])
  private case object Timeout extends Command
  private case object TimerKey

  def apply(target: ActorRef[Batch], after: FiniteDuration, maxSize: Int): Behavior[Command] = {
    Behaviors.withTimers(timers => new Buncher(timers, target, after, maxSize).idle())
  }
}

class Buncher(
             timers: TimerScheduler[Buncher.Command],
             target: ActorRef[Buncher.Batch],
             after: FiniteDuration,
             maxSize: Int) {
  import Buncher._
  private def idle(): Behavior[Command] = {
    Behaviors.receiveMessage[Command] { message =>
      timers.startSingleTimer(TimerKey, Timeout, after)
      active(Vector(message))
    }
  }

  def active(buffer: Vector[Buncher.Command]): Behavior[Command] = {
    Behaviors.receiveMessage[Command] {
      case ExcitingMessage(m) =>
        val newBuffer = buffer :+ m
        if (newBuffer.size == maxSize) {
          timers.cancel(TimerKey)
          target ! Batch(newBuffer)
          idle()
        }
      case Buncher.Timeout =>
        target ! Batch(buffer)
        idle()
    }
  }
}

object Scheduling {

}
