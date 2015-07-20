package com.typesafe.test

import org.scalatest.FunSuite
import com.typesafe.spark.rs.TcpSubscriberFactory
import com.typesafe.spark.rs.TcpPublisher
import scala.util.Try
import scala.annotation.tailrec
import com.typesafe.spark.rs.TcpSubscriber
import scala.util.Success
import java.nio.channels.AlreadyBoundException
import scala.util.Failure

class ToyRsTcpSuite extends FunSuite {

  test("Request 10, publish 15, receive 10, cancel") {
    val testP = new TestPublisher
    val testS = new TestSubscriber

    val factory = getTcpSubscriberFactory()
    factory.onConnect { tcpS => testP.subscribe(tcpS) }

    val tcpP = new TcpPublisher("localhost", factory.port)

    tcpP.subscribe(testS)
    testS.waitSubscription(1)

    testS.request(10)
    testP.newValues(items(1, 15))
    testS.receive(10, 1)
    testS.waitReceiveNone(1)
    testS.cancel

    factory.stop()
  }

  test("Request 0, publish 15, receive 0, cancel") {
    val testP = new TestPublisher
    val testS = new TestSubscriber

    val factory = getTcpSubscriberFactory()
    factory.onConnect { tcpS => testP.subscribe(tcpS) }

    val tcpP = new TcpPublisher("localhost", factory.port)

    tcpP.subscribe(testS)
    testS.waitSubscription(1)

    testP.newValues(items(1, 15))
    testS.waitReceiveNone(1)
    testS.cancel

    factory.stop()
  }

  test("Request 10, publish 5, receive 5, cancel") {
    val testP = new TestPublisher
    val testS = new TestSubscriber

    val factory = getTcpSubscriberFactory()
    factory.onConnect { tcpS => testP.subscribe(tcpS) }

    val tcpP = new TcpPublisher("localhost", factory.port)

    tcpP.subscribe(testS)
    testS.waitSubscription(1)

    testS.request(10)
    testP.newValues(items(1, 5))
    testS.receive(5, 1)
    testS.waitReceiveNone(1)
    testS.cancel

    factory.stop()
  }

  test("Request 10, publish 5, receive 5, completed") {
    val testP = new TestPublisher
    val testS = new TestSubscriber

    val factory = getTcpSubscriberFactory()
    factory.onConnect { tcpS => testP.subscribe(tcpS) }

    val tcpP = new TcpPublisher("localhost", factory.port)

    tcpP.subscribe(testS)
    testS.waitSubscription(1)

    testS.request(10)
    testP.newValues(items(1, 5))
    testS.receive(5, 1)
    testS.waitReceiveNone(1)
    testP.complete
    testS.waitCompleted(1)

    factory.stop()
  }

  test("Request 5, publish 10, receive 5, completed") {
    val testP = new TestPublisher
    val testS = new TestSubscriber

    val factory = getTcpSubscriberFactory()
    factory.onConnect { tcpS => testP.subscribe(tcpS) }

    val tcpP = new TcpPublisher("localhost", factory.port)

    tcpP.subscribe(testS)
    testS.waitSubscription(1)

    testS.request(5)
    testP.newValues(items(1, 10))
    testS.receive(5, 1)
    testS.waitReceiveNone(1)
    testP.complete
    testS.waitCompleted(1)

    factory.stop()
  }

  private def items(first: Int, last: Int): List[String] =
    (first to last).map(i => s"item_$i")(collection.breakOut)

  private def getTcpSubscriberFactory(): TcpSubscriberFactory = {

    val firstPort = 2222
    val lastPort = 2252

    @tailrec
    def getOne(port: Int): TcpSubscriberFactory = {
      if (port > lastPort)
        fail(s"unable to find an available port in the range $firstPort-$lastPort")
      Try(TcpSubscriberFactory(2222)) match {
        case Success(s) =>
          s
        case Failure(e: AlreadyBoundException) =>
          getOne(port + 1)
        case Failure(e) =>
          fail(e)
      }
    }

    getOne(2222)
  }

}