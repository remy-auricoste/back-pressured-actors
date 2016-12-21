package fr.catsoft.backpressured

import fr.catsoft.backpressured.model.Message

trait Producer[T] extends Actor[T] {
  def dest: Actor[T]

  def produce: Option[T]

  override def mailBoxSize: Int = 0

  override def process(implicit messageInContext: Message[T]): (T) => Unit = throw new UnsupportedOperationException

  override protected def runAction: Unit = {
    val destVal = dest
    produce match {
      case Some(value) => send(dest, value)
      case None => stop
    }
  }
}
