package com.typesafe.test

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class TestPublisher extends Publisher[String] {
  
  var subscriptions: List[TestSubscription] = Nil
  var subscriptionsLock = new Object
  
  def subscribe(s: Subscriber[_ >: String]): Unit = {
    subscriptionsLock synchronized {
      val subscription = new TestSubscription(s, this)
      subscriptions ::= subscription
      
      s.onSubscribe(subscription)
    }
  }

  // test method
  def newValues(items: List[String]) = {
    subscriptions synchronized {
      subscriptions.foreach { _.newValues(items) }
    }
  }

  // test method
  def complete() {
    subscriptions synchronized{
      subscriptions.foreach { _.complete }
      subscriptions = Nil
    }
  }

  // internal method
  def removeSubscription(subscription: TestSubscription) = {
    subscriptionsLock synchronized {
      subscriptions = subscriptions.filterNot { _ == subscription }
    }
  }

}