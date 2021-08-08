package net.zhenglai

import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors

object MasterControlApp {
  sealed trait Command

  final case class SpawnJob(name: String, replyToWhenDone: ActorRef[JobDone]) extends Command

  final case class JobDone(name: String)

  case object GracefulShutdown extends Command

  private final case class JobTerminated(name: String, replyToWhenDone: ActorRef[JobDone]) extends Command


  def apply(): Behavior[Command] = {
    Behaviors.receive[Command] { (context, message) =>
      message match {
        case SpawnJob(jobName, replyToWhenDone) =>
          context.log.info("Spawning job {}!", jobName)
          val job = context.spawn(Job(jobName), name = jobName)
          context.watchWith(job, JobTerminated(jobName, replyToWhenDone))
          Behaviors.same
        case GracefulShutdown =>
          context.log.info("Initiating graceful shutdown...")
          context.log.info("Simulating some graceful shutdown")
          Behaviors.same
        case JobTerminated(jobName, replyToWhenDone) =>
          context.log.info("Job stopped: {}", jobName)
          replyToWhenDone ! JobDone(jobName)
          Behaviors.same
      }
    }.receiveSignal {
      case (context, PostStop) =>
        context.log.info("Master Control App stopped")
        Behaviors.same
    }
  }

  object Job {
    sealed trait Command

    // When cleaning up resources from PostStop you should also consider doing the same for the PreRestart signal, which is emitted when the actor is restarted.
    // Note that PostStop is not emitted for a restart.
    def apply(name: String): Behavior[Command] = {
      Behaviors.receiveSignal[Command] {
        case (context, PostStop) =>
          context.log.info("Worker {} stopped", name)
          Behaviors.same
      }
    }
  }
}
