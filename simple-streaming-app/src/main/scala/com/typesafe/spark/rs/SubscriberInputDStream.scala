package com.typesafe.spark.rs

import org.apache.spark.streaming.StreamingContext
import org.apache.spark.storage.StorageLevel
import scala.reflect.ClassTag
import org.apache.spark.streaming.receiver.Receiver
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.apache.spark.Logging
import org.apache.spark.streaming.dstream.ReceiverInputDStream

class SubscriberInputDStream[T: ClassTag](
  @transient ssc_ : StreamingContext,
  storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2)(
    publisherFactory: () => Publisher[T])
  extends ReceiverInputDStream[T](ssc_) {

  def getReceiver(): Receiver[T] = {
    new SubscriberReceiver(storageLevel, publisherFactory)
  }

}

private[rs] class SubscriberReceiver[T](
  storageLevel: StorageLevel,
  publisherFactory: () => Publisher[T])
  extends Receiver[T](storageLevel) {

  var internalSubscriber: InternalSubscriber[T] = _

  def onStart(): Unit = {
    val publisher = publisherFactory()
    internalSubscriber = new InternalSubscriber(this)

    publisher.subscribe(internalSubscriber)
  }

  def onStop(): Unit = {
    internalSubscriber.disconnect
  }

  override def useRateLimiterInReceiver: Boolean = true

  override def updateRateLimit(eps: Long): Unit = {
    if (eps > 0)
      internalSubscriber.updateRateLimit(eps)
  }
}

private[rs] class InternalSubscriber[T](subscriber: SubscriberReceiver[T]) extends Subscriber[T] with Logging {

  // TODO: make it configurable
  var elementPerSecond: Long = 100000
  var sliceSize: Long = elementPerSecond / 100

  // initialized on onSubscribe
  var secondStart: Long = 0
  var elementInSecond: Long = 0
  var requestedElements: Long = 0

  val lock = new Object()

  var subscription: Subscription = _

  def onComplete(): Unit = {
    subscriber.restart("Stream completed")
  }

  def onError(e: Throwable): Unit = {
    logWarning("Error receiving data", e)
    subscriber.restart("Error receiving data", e)
  }

  def onNext(t: T): Unit = {
    subscriber.store(t)
    lock synchronized {
      elementInSecond += 1
      requestedElements -= 1
      if (requestedElements < 0) {
        subscription.cancel()
        subscriber.restart("Bad producer, too many items")
      }
      computeNewRequests(withDelay = true)
    }
  }

  def onSubscribe(sub: Subscription): Unit = {
    subscription = sub
    lock synchronized {
      secondStart = System.currentTimeMillis()
      requestedElements = sliceSize * 2
      requestItems(sliceSize * 2)
    }
  }

  def disconnect(): Unit = {
    subscription.cancel()
  }

  private def computeNewRequests(withDelay: Boolean): Unit = {
    if (requestedElements <= sliceSize) {
      val elapsedTimeInSecond = System.currentTimeMillis() - secondStart
      if (elapsedTimeInSecond > 1000) {
        secondStart += elapsedTimeInSecond % 1000 * 1000
        val toRequest = sliceSize * 2 - requestedElements
        requestedElements += toRequest
        requestItems(toRequest)
      } else {
        val expectedExhauctionTime = 1000D * elementInSecond / elementPerSecond
        if (requestedElements == 0) {
          if (withDelay == true)
            // TODO: bad wait. Should be pushed on some clock
            lock.wait((expectedExhauctionTime - elapsedTimeInSecond).toInt)
            requestItems(sliceSize)
        } else {
          if (expectedExhauctionTime > elapsedTimeInSecond) {
            // do nothing, data is already coming too fast
          } else {
            val toRequest = (elementPerSecond - elementInSecond) min sliceSize
            requestedElements += toRequest
            requestItems(toRequest)
          }
        }
      }
    }
  }
  
  private def requestItems(number: Long): Unit = {
    logInfo(s"requesting $number items")
    subscription.request(number)
  }

  def updateRateLimit(eps: Long): Unit = {
    lock synchronized {
      elementPerSecond = eps
      sliceSize = elementPerSecond / 100
      computeNewRequests(withDelay = false)
    }
  }
}
