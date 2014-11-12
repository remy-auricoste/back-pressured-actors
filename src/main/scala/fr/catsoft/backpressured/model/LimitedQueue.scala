package fr.catsoft.backpressured.model

import scala.collection.mutable

case class LimitedQueue[T](limit: Int) extends mutable.Queue[T] {
  override def enqueue(elems: T*): Unit = {
    val count = size + elems.size
    super.enqueue(elems:_*)
    if (count > limit) {
      throw new FullQueueException(s"$count over limit $limit")
    }
  }
}


