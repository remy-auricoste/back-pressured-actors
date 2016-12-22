package fr.catsoft.backpressured

import fr.catsoft.backpressured.pool.ActorPoolByMinMailBox
import fr.catsoft.backpressured.model.Message
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Test extends App {
  val poolAk3 = new ActorPoolByMinMailBox[String] with StatusDefined {
    override def actorTemplate: Boolean => Actor[String] = bool => MyActor("AK3", 2000, None, 5)

    override def poolSize: Int = 2

    override def getStatus: String = {
      val full = if (mailboxIn.isFull) "-|-" else "---"
      full + mailboxMessageWaitingSize + "-" +poolActors.map(_.asInstanceOf[StatusDefined].getStatus).mkString("+")
    }
  }
  val ak2 = MyActor("AK2", 500, Some(poolAk3))
  val ak1 = MyActor("AK1", 100, Some(ak2))

  val producer = new Producer[String] {
    override def dest: Actor[String] = ak1

    var cpt = 0

    private def sendMessage = {
      cpt += 1
      //println(s"sending message $cpt")
      Some(s"MESSAGE $cpt")
    }

    override def produce: Option[String] = cpt < 50 match {
      case true => sendMessage
      case _ => {
        Thread.sleep(20000)
        sendMessage
      }
    }
  }
  producer.start
  Thread.sleep(1000 * 60 * 10)

  trait StatusDefined {
    this: Actor[_] =>

    def getStatus: String = {
      val result: String = if (mailboxIn.isFull) "-|-" else "---"
      result + mailboxMessageWaitingSize + "-" + name
    }
  }

  case class MyActor(override val name: String, sleep: Long, destOption: Option[Actor[String] with StatusDefined], limit: Int = 9)
    extends Actor[String] with StatusDefined {
    override def mailBoxSize: Int = limit

    override def process(implicit messageInContext: Message[String]): (String) => Unit = param => {
      Thread.sleep(sleep)
      //println(s"$name treating message $param")

      println(ak1.getStatus)
      val result = s"$param $name"
      destOption match {
        case Some(dest) => send(dest, result)
        case None => println(s"received : $result")
      }
    }

    override def getStatus: String = {
      super.getStatus + destOption.map(_.getStatus).getOrElse("")
    }
  }
}

