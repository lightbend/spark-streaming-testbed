package com.typesafe.test

import org.scalatest.FunSuite
import com.typesafe.spark.rs.TcpSubscriberFactory
import com.typesafe.spark.rs.TcpPublisher

class ToyRsTcpSuite extends FunSuite {
  
  test("Request 10, receive 10, cancel")  {
    val testP = new TestPublisher
    val testS = new TestSubscriber
    
    new TcpSubscriberFactory(2222).bind { tcpS => testP.subscribe(tcpS) }
    
    val tcpP = new TcpPublisher("localhost", 2222)
    
    tcpP.subscribe(testS)
    testS.waitSubscription(3000)
    
    testS.request(10)
    testP.newValues(items(1,15))
    testS.receive(10, 10000)
    testS.waitReceiveNone(1)
    testS.cancel
    
  }
  
  def items(first: Int, last: Int): List[String] =
    (first to last).map(i => s"item_$i")(collection.breakOut)

}