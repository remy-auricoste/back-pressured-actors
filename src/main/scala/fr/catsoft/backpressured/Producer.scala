package fr.catsoft.backpressured

import fr.catsoft.backpressured.model.Message

trait Producer[T] extends Actor[Unit] {

  override private[back] def mailBoxLimit: Int = 0

  def dest: Actor[T]

  def produce: Option[T]

  override protected def runAction: Unit = {
    val destVal = dest
    produce match {
      case Some(value) => {
        send(dest, value)(None)
        if (getOutPipe(destVal).isPressured) {
          stop
        }
      }
      case None => stop
    }
  }

  override def process(implicit messageInContext: Option[Message[Unit]]): (Unit) => Unit = Unit => {
    throw new UnsupportedOperationException
  }
}
