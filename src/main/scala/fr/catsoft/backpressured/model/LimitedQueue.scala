package fr.catsoft.backpressured.model

import scala.collection.mutable

case class LimitedQueue[T](limit: Int) extends mutable.Queue[T] {

  /**
    * @param elems
    * @return true if queue is full
    */
  def add(elems: T*): Boolean = {
    val count = size + elems.size
    enqueue(elems: _*)
    count > limit
  }

  def isFull: Boolean = {
    size >= limit
  }
}