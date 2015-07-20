package com.typesafe.test

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.CountDownLatch
import org.scalatest.Assertions._
import scala.collection.immutable.Queue
import scala.annotation.tailrec
import java.util.concurrent.TimeUnit

class TestSubscriber extends Subscriber[String] {

  // NPEs are fine, it should fail the test if the subscription has not been received anyway
  var subscription: Subscription = null
  val subscriptionLock = new Object

  var receivedItems: Queue[String] = Queue()
  val receivedItemsLock = new Object

  val onCompleteLatch = new CountDownLatch(1)

  def onComplete(): Unit = {
    onCompleteLatch.countDown()
  }

  def onError(t: Throwable): Unit = {
    ???
  }

  def onNext(t: String): Unit = {
    receivedItemsLock synchronized {
      receivedItems = receivedItems.enqueue(t)
      receivedItemsLock.notify()
    }
  }

  def onSubscribe(s: Subscription): Unit = {
    subscriptionLock synchronized {
      subscription = s
      subscriptionLock.notify()
    }
  }

  // test method
  def request(n: Long) {
    subscription.request(n)
  }

  // test method
  def receive(n: Int, wait: Long) {
    val endTime = System.currentTimeMillis() + wait * 1000

    @tailrec
    def dequeue(n: Int) {
      if (n > 0) {
        if (receivedItems.isEmpty) {
          val waitTime = endTime - System.currentTimeMillis()
          if (waitTime <= 0) {
            fail("timeout")
          } else {
            receivedItemsLock.wait(waitTime)
            dequeue(n)
          }
        } else {
          val (_, remaider) = receivedItems.dequeue
          receivedItems = remaider
          dequeue(n - 1)
        }

      }
    }

    receivedItemsLock synchronized {
      dequeue(n)
    }
  }

  // test method
  def waitReceiveNone(wait: Long) {
    val endTime = System.currentTimeMillis() + wait * 1000

    @tailrec
    def checkEmpty() {
      val waitTime = endTime - System.currentTimeMillis()
      if (waitTime > 0) {
        if (receivedItems.size > 0) {
          fail("received unexpected messages")
        } else {
          receivedItemsLock.wait(waitTime)
          checkEmpty()
        }
      }
    }

    receivedItemsLock synchronized {
      checkEmpty()
    }
  }

  def waitSubscription(wait: Long) = {
    if (subscription == null)
      subscriptionLock synchronized {
        subscriptionLock.wait(wait * 1000)
        assert(subscription != null)
      }
  }

  // test method
  def cancel = {
    subscription.cancel()
  }

  // test method
  def waitCompleted(wait: Long) = {
    onCompleteLatch.await(wait, TimeUnit.SECONDS)
  }
}