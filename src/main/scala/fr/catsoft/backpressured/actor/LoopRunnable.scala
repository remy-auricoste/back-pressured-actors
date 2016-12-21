package fr.catsoft.backpressured.actor

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait LoopRunnable extends SwitchableMachine {
  private var running = false

  override def isRunning: Future[Boolean] = Future.successful(running)

  override def stop: Future[Boolean] = Future.successful {
    synchronized {
      running = false
      running
    }
  }

  override def start: Future[Boolean] = Future.successful {
    synchronized {
      if (!running) {
        running = true
        runLoop
        running
      } else {
        running
      }
    }
  }

  private def runLoop: Future[Unit] = Future {
    try {
      runAction
    } catch {
      case ex: Throwable => ex.printStackTrace()
    }
    if (running) {
      runLoop
    }
  }

  protected def runAction: Unit
}
