package fr.catsoft.backpressured.actor

import scala.concurrent.Future

/**
  * Created by rauricoste on 21/12/16.
  */
trait SwitchableMachine {
  def isRunning: Future[Boolean]

  def stop: Future[Boolean]

  def start: Future[Boolean]
}
