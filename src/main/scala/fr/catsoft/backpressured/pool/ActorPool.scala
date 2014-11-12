package fr.catsoft.backpressured.pool

import fr.catsoft.backpressured.model.Message
import fr.catsoft.backpressured.{ActorCloneable, Actor}

trait ActorPool[T] extends Actor[T] {
  def poolSize: Int
  def actorTemplate: Actor[T] with ActorCloneable
  def poolChoice: Seq[Actor[T]] => Actor[T]

  override private[back] def mailBoxLimit: Int = actorTemplate.mailBoxLimit

  protected lazy val poolActors: Seq[Actor[T]] = (0 until poolSize).map(x => actorTemplate.clone)

  override def process(implicit messageInContext: Option[Message[T]]): (T) => Unit = param => {
    send(poolChoice(poolActors), param)
  }
}
