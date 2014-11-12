package fr.catsoft.backpressured.actor

import fr.catsoft.backpressured.model.LimitedQueue

trait Mailboxed[T] {
  private[back] def mailBoxLimit: Int

  private[back] val mailBox = LimitedQueue[T](mailBoxLimit)
}
