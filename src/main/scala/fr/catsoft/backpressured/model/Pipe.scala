package fr.catsoft.backpressured.model

import fr.catsoft.backpressured.Actor

case class Pipe(source: Actor[_], dest: Actor[_]) {
  private var pressured = false

  def setPressure(value: Boolean): Boolean = this.synchronized {
    pressured != value match {
      case false => false
      case true => {
        //println(s"pressure ${source.name} => ${dest.name} : $value, ${dest.mailBox.size}")
        pressured = value
        true
      }
    }
  }

  def isPressured: Boolean = this.synchronized {
    pressured
  }
}
