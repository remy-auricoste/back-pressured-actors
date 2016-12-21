package fr.catsoft.backpressured.pool

import fr.catsoft.backpressured.Actor

trait ActorPoolByMinMailBox[T] extends ActorPool[T] {
  override def poolChoice: (Seq[Actor[T]]) => Actor[T] = _.minBy(_.mailboxMessageWaitingSize)
}
