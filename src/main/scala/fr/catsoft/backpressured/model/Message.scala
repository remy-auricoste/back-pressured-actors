package fr.catsoft.backpressured.model

import fr.catsoft.backpressured.Actor

case class Message[T](sourceOpt: Option[Actor[_]], dest: Actor[T], content: T)