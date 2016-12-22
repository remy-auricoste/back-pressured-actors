package fr.catsoft.backpressured

import java.util.concurrent.ConcurrentHashMap

import fr.catsoft.backpressured.actor.LoopRunnable
import fr.catsoft.backpressured.model.{LimitedQueue, Message}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Random, Success, Try}
import scala.collection.JavaConversions._

trait Actor[T] extends LoopRunnable {
  var pressureReceived = new ConcurrentHashMap[String, Boolean]()
  var pressureSent = new ConcurrentHashMap[String, Boolean]()

  def mailBoxSize: Int
  protected val mailboxIn = LimitedQueue[Message[T]](mailBoxSize)

  protected val mailBoxLowLimit: Int = mailBoxSize / 2
  println(s"limit = $mailBoxLowLimit")

  private lazy val randomName = Random.nextInt().toString

  def name = randomName

  def process(implicit messageInContext: Message[T]): T => Unit

  override protected def runAction: Unit = dequeue

  private def dequeue: Unit = {
//    println(s"$name treating message")
    Try(mailboxIn.dequeue()) match {
      case Success(message) => {
//        println(s"$name, $message, ${mailboxIn.size}, $pressureReceived")
        message.sourceOpt match {
          case Some(source) if mailboxIn.size <= mailBoxLowLimit && Option(pressureSent.get(source.name)) == Some(true) => sendPressure(source, pressure = false)
          case _ =>
        }
        Try(process(message)(message.content)) match {
          case Failure(ex) => {
            // TODO : handle ex
            ex.printStackTrace()
          }
          case _ =>
        }
      }
      case Failure(ex: NoSuchElementException) => stop
    }
  }

  def receive(message: Message[T]): Future[Boolean] = Future.successful {
    val wasEmpty = mailboxIn.isEmpty
    val fullQueue = mailboxIn.add(message)
    if (fullQueue) {
      message.sourceOpt.map(sendPressure(_, pressure = true))
    } else if (wasEmpty && isPressured) {
      start
    }
    true
  }

  private def isPressured: Boolean = pressureReceived.forall(tuple => !tuple._2)

  def send[A](dest: Actor[A], content: A): Future[Boolean] = {
    val message = Message[A](Some(this), dest, content)
    dest.receive(message)
  }

  def mailboxMessageWaitingSize: Int = mailboxIn.size

  def receivePressure(name: String, pressure: Boolean): Future[Boolean] = Future {
    pressureReceived.put(name, pressure)
//    println(s"${this.name} : receive pressure $name -> $pressure")
    if (pressure) {
      stop
    } else if (isPressured) {
      start
    }
    true
  }

  def sendPressure(dest: Actor[_], pressure: Boolean): Future[Boolean] = {
//    println(s"${this.name} sending pressure ${dest.name} -> $pressure")
    pressureSent.put(dest.name, pressure)
    dest.receivePressure(this.name, pressure)
  }
}