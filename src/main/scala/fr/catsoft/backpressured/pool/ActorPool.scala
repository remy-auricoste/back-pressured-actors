package fr.catsoft.backpressured.pool

import fr.catsoft.backpressured.Actor
import fr.catsoft.backpressured.model.Message

trait ActorPool[T] extends Actor[T] {
  def poolSize: Int
  def actorTemplate: Boolean => Actor[T]
  def poolChoice: Seq[Actor[T]] => Actor[T]

  private lazy val singleActor: Actor[T] = actorTemplate(true)

  override def mailBoxSize: Int = singleActor.mailBoxSize

  protected lazy val poolActors: Seq[Actor[T]] = Seq(singleActor) ++ (1 until poolSize).map(x => actorTemplate(true))

  override def process(implicit messageInContext: Message[T]): (T) => Unit = param => {
    send(poolChoice(poolActors), param)
  }
}
