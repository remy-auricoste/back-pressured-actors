package fr.catsoft.backpressured

import fr.catsoft.backpressured.pool.ActorPoolByMinMailBox
import fr.catsoft.backpressured.model.Message

object Test extends App {
  val ak3 = MyActor("AK3", 1500, None)
  val poolAk3 = new ActorPoolByMinMailBox[String] with StatusDefined with LinkedActor {
    override def actorTemplate: Actor[String] = ak3
    override def poolSize: Int = 2

    override def getStatus: String = poolActors.map(_.mailBoxSize).mkString("+")
  }
  val ak2 = MyActor("AK2", 500, Some(poolAk3))
  val ak1 = MyActor("AK1", 100, Some(ak2))

  val producer = new Producer[String] {
    override def name: String = "PRODUCER"
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


  producer.run
  Thread.sleep(1000 * 60 * 10)
}

trait StatusDefined { this: Actor[_] =>
  def getStatus: String = {
    var result: String = mailBoxSize.toString
    if (outPipes.size() > 0) {
      val outPipe = outPipes.entrySet().iterator().next().getValue
      val pipe: String = outPipe != null && outPipe.isPressured match {
        case true => "-|-"
        case false => "---"
      }
      result = result + pipe
      result = result + outPipe.dest.asInstanceOf[StatusDefined].getStatus
    }
    result
  }
}

trait LinkedActor { this: Actor[_] with StatusDefined =>
  def getIncoming: LinkedActor with StatusDefined = {
    inPipes.size() == 0 match {
      case true => this
      case false => {
        inPipes.entrySet().iterator().next().getKey match {
          case x: LinkedActor => x.getIncoming
          case x => this
        }
      }
    }
  }
}

case class MyActor(override val name: String, sleep: Long, destOption: Option[Actor[String] with StatusDefined], limit: Int = 10)
  extends Actor[String] with StatusDefined with LinkedActor with ActorCloneable[String] {
  override def mailBoxLimit: Int = limit

  override def process(implicit messageInContext: Option[Message[String]]): (String) => Unit = param => {
    Thread.sleep(sleep)
    //println(s"$name treating message $param")
    println(getIncoming.getStatus)
    val result = s"$param $name"
    destOption match {
      case Some(dest) => send(dest, result)
      case None => //println(s"received : $result")
    }
  }


}
