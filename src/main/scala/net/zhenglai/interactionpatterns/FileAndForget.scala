package net.zhenglai.interactionpatterns

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Printer {
  sealed trait Command

  case class PrintMe(message: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.receive {
      case (context, PrintMe(message)) =>
        context.log.info(message)
        Behaviors.same
    }
}

object FileAndForget {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Printer(), "fire-and-forget-example")
    // the system is also the top level actor ref
    val printer: ActorRef[Printer.Command] = system
    printer ! Printer.PrintMe("message 1")
    printer ! Printer.PrintMe("not message 2")
  }
}
