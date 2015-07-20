package com.typesafe.test

import org.reactivestreams.Subscription
import org.reactivestreams.Subscriber
import scala.collection.immutable.Queue

class TestSubscription(subscriber: Subscriber[_ >: String], publisher: TestPublisher) extends Subscription {
  
  var pendingRequests = 0L
  var pendingItems: Queue[String] = Queue()
  val lock = new Object
  
  def cancel(): Unit = {
    publisher.removeSubscription(this)
    lock synchronized {
      pendingRequests = 0
      pendingItems = Queue()
    }
  }

  def request(n: Long): Unit = {
    lock synchronized {
      pendingRequests = n
      pushNexts()
    }
  }

  // internal method
  def newValues(items: List[String]) = {
    lock synchronized {
      pendingItems = pendingItems.enqueue(items)
      pushNexts()
    }
  }
  
  // internal method
  private def pushNexts() {
    val toSend = pendingRequests min pendingItems.size
    (0 until toSend.toInt) foreach { _ =>
       val (item, remainder) = pendingItems.dequeue
       pendingItems = remainder
       subscriber.onNext(item)
     }
     pendingRequests -= toSend
  }

  // internal method
  def complete = {
    subscriber.onComplete()
  }
}