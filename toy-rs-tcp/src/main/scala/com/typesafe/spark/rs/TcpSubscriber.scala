package com.typesafe.spark.rs

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.nio.channels.AsynchronousSocketChannel
import java.nio.ByteBuffer
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import scala.annotation.tailrec

/** A reactive stream subscriber, created around a TCP channel
 */
class TcpSubscriber(socket: AsynchronousSocketChannel) extends Subscriber[String] {
  def onComplete(): Unit = {
    socket.close()
  }

  def onError(t: Throwable): Unit = {
    // TODO: do we need to carry some error info?
    socket.close()
  }

  def onNext(t: String): Unit = {
    socket.write(ByteBuffer.wrap(s"$t\n".getBytes))
  }

  def onSubscribe(s: Subscription): Unit = {
    read(s, "")
  }
  
  def read(subscription: org.reactivestreams.Subscription, leftover: String) {

    val buffer = ByteBuffer.allocate(1024)

    val promise = Promise[Integer]

    socket.read(buffer, promise, new CompletionHandlerForPromise[Integer])

    // TODO: support error case
    promise.future foreach {
      byteRead =>
        if (byteRead < 0) {
          // time to close
          socket.close()
          subscription.cancel()
        } else {
          @tailrec
          def readChars(toRead: Int, acc: StringBuilder): String = {
            if (toRead > 0) {
              val b = buffer.get
              println(s"byte: $b")
              val c = b.toChar
              if (c == '\n') {
                // TODO: value check
                subscription.request(acc.toString().toLong)
                readChars(toRead - 1, new StringBuilder)
              } else {
                readChars(toRead - 1, acc.append(c))
              }
            } else {
              acc.toString
            }
          }
          
          buffer.rewind()
          val lo = readChars(byteRead, new StringBuilder(leftover))
          
          read(subscription, lo)
        }
    }
  }
}