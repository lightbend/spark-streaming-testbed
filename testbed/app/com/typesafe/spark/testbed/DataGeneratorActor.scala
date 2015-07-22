package com.typesafe.spark.testbed

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef
import java.nio.channels.AsynchronousSocketChannel
import akka.actor.PoisonPill
import java.nio.ByteBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Cancellable
import scala.collection.mutable
import scala.annotation.tailrec
import play.api.Logger
import scala.concurrent.Promise
import com.typesafe.spark.rs.TcpSubscriberFactory
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class DataGeneratorActor(scheduler: ActorRef) extends Actor {

  def receive = initialState

  val logger: Logger = Logger(this.getClass)

  val initialState: Actor.Receive = {
    case DataGeneratorActor.TestPlanMsg(testPlan) =>
      logger.info("New test plan. DataGenerator waking up.")
      context.become(executeNewTestPlan(testPlan, sender))
  }

  def executeNewTestPlan(testPlan: TestPlan, requestor: ActorRef): Actor.Receive = {
    import scala.concurrent.duration._
    val tickTask = context.system.scheduler.schedule(0.second, 1.second, self, DataGeneratorActor.TickMsg)
    val startTime = System.currentTimeMillis() / 1000 * 1000 + 1000 // beginning of the next second
    val dataGenerator = new DataGenerator(testPlan)

    executeTestPlan(dataGenerator, startTime, 0, requestor, tickTask)
  }

  def executeTestPlan(
    dataGenerator: DataGenerator,
    startTime: Long,
    tick: Int,
    requestor: ActorRef,
    tickTask: Cancellable): Actor.Receive = {
    case DataGeneratorActor.TestPlanMsg(testPlan) =>
      tickTask.cancel()
      scheduler ! EpochSchedulerActor.clearMsg
      context.become(executeNewTestPlan(testPlan, sender))
    case DataGeneratorActor.TickMsg =>
      if (dataGenerator.isDoneAt(tick)) {
        // test plan is done, return in waiting state
        // scheduler continues to push the scheduled messages
        logger.info("Test plan done. DataGenerator going to sleep.")
        tickTask.cancel()
        if (requestor != context.system.deadLetters) {
          requestor ! DataGeneratorActor.TestPlanDoneMsg(0)
        }
        context.become(initialState)
      } else {
        // continue test plan
        val values = dataGenerator.valuesFor(tick)
        
        values.flatMap { _.values }.groupBy { x => x}.foreach{ t =>
          logger.info(s"At tick $tick, ${t._2.size} times ${t._1}")
        }
        
        values.foreach { data =>
          scheduler ! EpochSchedulerActor.ScheduleMsg(data.shiftTime(startTime))
        }
        context.become(executeTestPlan(dataGenerator, startTime, tick + 1, requestor, tickTask))
      }
  }

}

object DataGeneratorActor {

  case object StopMsg
  case object TickMsg
  case class TestPlanMsg(testPlan: TestPlan)
  case class TestPlanDoneMsg(status: Int)

  def props(scheduler: ActorRef) = Props(classOf[DataGeneratorActor], scheduler)

}

class EpochSchedulerActor(serverManager: ActorRef) extends Actor {

  val logger: Logger = Logger(this.getClass)

  private val scheduledItems = mutable.PriorityQueue[DataAtTime]() /* mutable !! */

  def receive = emptyState

  private val emptyState: Actor.Receive = {
    case EpochSchedulerActor.ScheduleMsg(data) =>
      logger.info("Scheduler waking up.")
      context.become(startScheduler(data))
  }

  private def startScheduler(firstData: DataAtTime): Actor.Receive = {
    import scala.concurrent.duration._
    val tickTask = context.system.scheduler.schedule(0.second, 10.microsecond, self, EpochSchedulerActor.TickMsg)
    scheduledItems += firstData
    runningState(tickTask)
  }

  private def runningState(tickTask: Cancellable): Actor.Receive = {
    case EpochSchedulerActor.ScheduleMsg(data) =>
      scheduledItems += data
    case EpochSchedulerActor.TickMsg =>
      pushReadyItems()
      if (scheduledItems.isEmpty) {
        tickTask.cancel()
        logger.info("Scheduler queue empty. Going to sleep.")
        context.become(emptyState)
      }
    case EpochSchedulerActor.clearMsg =>
      scheduledItems.clear()
  }

  private def pushReadyItems() {
    val currentTime = System.currentTimeMillis()
    @tailrec
    def loop() {
      scheduledItems.headOption match {
        case Some(head) if head.time <= currentTime =>
          serverManager ! ServerManagerActor.SendInts(head.values)
          scheduledItems.dequeue()
          loop()
        case _ =>
      }
    }
    loop()
  }

}

object EpochSchedulerActor {

  case class ScheduleMsg(data: DataAtTime)
  case object clearMsg
  private case object TickMsg

  def props(serverManager: ActorRef) = Props(classOf[EpochSchedulerActor], serverManager)
}

class ServerManagerActor(socketPort: Int, reactiveSocketPort: Int) extends Actor {

  def receive = initialization

  val initialization: Actor.Receive = {
    case ServerManagerActor.StartMsg =>
      val server = Server(self, socketPort)
      val reactiveServer = TcpSubscriberFactory(reactiveSocketPort)
      reactiveServer.onConnect { sub => self ! ServerManagerActor.IncomingSubscriber(sub) }
      context.become(processConnectionsAndData(server, reactiveServer,  Nil))
  }

  def processConnectionsAndData(server: Server, reactiveServer: TcpSubscriberFactory, connectionActors: List[ActorRef]): Actor.Receive = {
    case Server.IncomingConnectionMsg(socket) =>
      val connectionActor = context.actorOf(ConnectionManagerActor.props(socket, self))
      context.become(processConnectionsAndData(server, reactiveServer, connectionActor :: connectionActors))
    case ServerManagerActor.IncomingSubscriber(sub) =>
      val subscriberActor = context.actorOf(SubscriberActor.props(sub, self))
      subscriberActor ! SubscriberActor.StartMsg
      context.become(processConnectionsAndData(server, reactiveServer, subscriberActor :: connectionActors))
    case ServerManagerActor.ConnectionClosedMsg(connectionActor) =>
      context.become(processConnectionsAndData(server, reactiveServer, connectionActors.filterNot { _ == connectionActor }))
    case ServerManagerActor.StopMsg =>
      connectionActors.foreach { _ ! ServerManagerActor.StopMsg }
      server.close()
      reactiveServer.stop()
      context.become(initialization)
    case m: ServerManagerActor.SendInts =>
      connectionActors.foreach { _ ! m }
  }

}

object ServerManagerActor {

  case object StartMsg
  case object StopMsg
  case class IncomingSubscriber(subscriber: Subscriber[String])
  case class ConnectionClosedMsg(connectionActor: ActorRef)
  case class SendInts(is: List[Int])

  def props(socketPort: Int, reactiveSocketPort: Int) = Props(classOf[ServerManagerActor], socketPort, reactiveSocketPort)
}

class ConnectionManagerActor(socket: AsynchronousSocketChannel, serverManager: ActorRef) extends Actor {

  val logger: Logger = Logger(this.getClass)

  def receive = readyToWrite

  val readyToWrite: Actor.Receive = {
    case Server.ConnectionClosedMsg =>
      serverManager ! ServerManagerActor.ConnectionClosedMsg(self)
      self ! PoisonPill
    case ServerManagerActor.StopMsg =>
      socket.close()
      self ! PoisonPill
    case ServerManagerActor.SendInts(is) =>
      val promise = Promise[Integer]

      socket.write(ByteBuffer.wrap(is.mkString("", "\n", "\n").getBytes), promise, new CompletionHandlerForPromise[Integer])

      promise.future.foreach { byteWritten =>
        self ! ConnectionManagerActor.DataWritten
      }

      context.become(waitingWriteAck)
  }

  val waitingWriteAck: Actor.Receive = {
    case Server.ConnectionClosedMsg =>
      serverManager ! ServerManagerActor.ConnectionClosedMsg(self)
      self ! PoisonPill
    case ServerManagerActor.StopMsg =>
      socket.close()
      self ! PoisonPill
    case ConnectionManagerActor.DataWritten =>
      context.become(readyToWrite)
    case ServerManagerActor.SendInts(is) =>
      // TODO: it would be good to be able to delay data a bit. Maybe
      logger.warn(s"unable to deliver ${is.size} values")
  }

}

object ConnectionManagerActor {

  case object DataWritten

  def props(socket: AsynchronousSocketChannel, serverManager: ActorRef) = Props(classOf[ConnectionManagerActor], socket, serverManager)
}

class SubscriberActor(sub: Subscriber[String], serverManager: ActorRef) extends Actor {

  val logger: Logger = Logger(this.getClass)

  def receive = initialization

  val initialization: Actor.Receive = {
    case SubscriberActor.StartMsg =>
      println("initialize subscriber")
      val subscription = new SubscriberActorSubscription(self)
      sub.onSubscribe(subscription)
      context.become(processing(0))
  }

  def processing(requested: Long): Actor.Receive = {
    case SubscriberActor.RequestMsg(n) =>
      logger.info(s"received request for $n values")
      context.become(processing(requested + n))
    case ServerManagerActor.SendInts(is) =>
      val nbToSend = is.size
      if (requested == 0) {
        logger.warn(s"unable to deliver $nbToSend values")
      } else {
        if (nbToSend > requested) {
          is.take(requested.toInt).foreach { i =>
            sub.onNext(i.toString)
          }
          context.become(processing(0))
        } else {
          is.foreach { i =>
            sub.onNext(i.toString)
          }
          context.become(processing(requested - nbToSend))
        }
      }
    case ServerManagerActor.StopMsg =>
      sub.onComplete()
      self ! PoisonPill
  }

}

class SubscriberActorSubscription(subscriberActor: ActorRef) extends Subscription {
  def cancel(): Unit = {
    subscriberActor ! SubscriberActor.CancelMsg
  }

  def request(n: Long): Unit = {
    subscriberActor ! SubscriberActor.RequestMsg(n)
  }
}

object SubscriberActor {

  case object StartMsg
  case object CancelMsg
  case class RequestMsg(n: Long)

  def props(sub: Subscriber[String], serverManager: ActorRef) = Props(classOf[SubscriberActor], sub, serverManager)
}
