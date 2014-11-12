package fr.catsoft.backpressured.model

import fr.catsoft.backpressured.Actor
import fr.catsoft.backpressured.actor.LoopRunnable

case class Message[T](sourceOption: Option[LoopRunnable], dest: Actor[T], content: T)