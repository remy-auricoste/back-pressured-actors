package fr.catsoft.backpressured

import scala.util.{Failure, Success, Try, Random}
import java.util.concurrent.ConcurrentHashMap
import fr.catsoft.backpressured.model.{Pipe, Message, FullQueueException}
import fr.catsoft.backpressured.actor.{Mailboxed, LoopRunnable}

trait Actor[T] extends Mailboxed[Message[T]] with LoopRunnable {
  val mailBoxLowLimit = mailBoxLimit / 2
  private lazy val randomName = Random.nextInt().toString

  def name = randomName

  def mailBoxSize = mailBox.size

  val inPipes = new ConcurrentHashMap[LoopRunnable, Pipe]()
  val outPipes = new ConcurrentHashMap[Actor[_], Pipe]()

  def process(implicit messageInContext: Option[Message[T]]): T => Unit

  override protected def runAction: Unit = dequeue

  private def dequeue: Unit = {
    Try(mailBox.dequeue()) match {
      case Success(message) => {
        message.sourceOption match {
          case Some(source) if mailBox.size == mailBoxLowLimit && inPipes.get(source).isPressured =>
            val pipe = inPipes.get(source)
            val changed = pipe.setPressure(false)
            if (changed) {
              pipe.source.run
            }
          case _ =>
        }
        implicit val implMessage = Some(message)
        Try(process(implMessage)(message.content)) match {
          case Failure(ex) => {
            // TODO : handle ex
            //println("ERROR : " + ex.getMessage)
          }
          case _ =>
        }
      }
      case Failure(ex: NoSuchElementException) =>
        //println(s"$name empty")
        inPipes.map(_._2).map {
          pipe =>
            pipe.setPressure(false)
            pipe.source.run
        }
        stop
    }
  }

  def receive(message: Message[T], pipeOption: Option[Pipe] = None) = {
    val dest = message.dest
    dest.mailBox.enqueue(message)
    pipeOption match {
      case Some(pipe) if pipe.isPressured =>
      case _ => dest.run
    }
  }

  protected def getOutPipe(dest: Actor[_]): Pipe = {
    var pipe = outPipes.get(dest)
    if (pipe == null) {
      pipe = Pipe(this, dest)
      outPipes.put(dest, pipe)
      dest.inPipes.put(this, pipe)
    }
    pipe
  }

  def send[A](dest: Actor[A], content: A)(implicit messageInContext: Option[Message[T]]): Unit = {
    val message = Message[A](Some(this), dest, content)
    val pipe = getOutPipe(dest)
    try {
      dest.receive(message, Some(pipe))
      if (pipe.isPressured) {
        throw new FullQueueException("already pressured")
      }
    } catch {
      case ex: FullQueueException => {
        pipe.setPressure(true)
        messageInContext.flatMap(_.sourceOption) match {
          case Some(source) => inPipes.get(source).setPressure(true)
          case None =>
        }
        stop
      }
    }
  }
}

trait ActorCloneable { this: Actor[T] =>
  def clone: Actor[T] with ActorCloneable[T]
}
