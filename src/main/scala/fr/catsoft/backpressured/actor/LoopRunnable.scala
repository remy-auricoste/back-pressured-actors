package fr.catsoft.backpressured.actor

import scala.concurrent.Future

trait LoopRunnable {
  def name: String

  private var running = false

  protected def stop = this.synchronized {
    running = false
    //println(s"$name status : stopped")
  }


  def run: Future[Unit] = Future {
    this.synchronized {
      if (!running) {
        //println(s"$name status : running")
        running = true
        runLoop
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
