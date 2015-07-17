package com.typesafe.spark.rs

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import java.nio.channels.AsynchronousSocketChannel
import java.net.InetSocketAddress
import java.net.InetAddress
import java.nio.channels.CompletionHandler
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.spark.rs.internal.Subscription
import java.nio.ByteBuffer
import scala.annotation.tailrec

/** Single subscriber publisher
 */

class TcpPublisher(hostname: String, port: Int) extends Publisher[String] {
  
  def subscribe(subscriber: Subscriber[_ >: String]): Unit = {
    val socketChannel = AsynchronousSocketChannel.open()
    
    val voidPromise = Promise[Void]
    
    socketChannel.connect(
        new InetSocketAddress(InetAddress.getByName(hostname), port),
        voidPromise,
        new CompletionHandlerForPromise[Void])

    // TODO: support error case
    voidPromise.future.foreach{ v =>
      val subscription = new Subscription(socketChannel)
      subscriber.onSubscribe(subscription)
      read(socketChannel, subscriber, "")
      
    }
  }
  
  def read(socket: AsynchronousSocketChannel, subscriber: Subscriber[_ >: String], leftover: String) {

    val buffer = ByteBuffer.allocate(1024)

    val promise = Promise[Integer]

    socket.read(buffer, promise, new CompletionHandlerForPromise[Integer])

    // TODO: support error case
    promise.future foreach {
      byteRead =>
        if (byteRead < 0) {
          // time to close
          socket.close()
          subscriber.onComplete()
        } else {
          @tailrec
          def readChars(toRead: Int, acc: StringBuilder): String = {
            if (toRead > 0) {
              val c = buffer.get.toChar
              if (c == '\n') {
                subscriber.onNext(acc.toString())
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
          
          read(socket, subscriber, lo)
        }
    }
  }
}

class CompletionHandlerForPromise[T] extends CompletionHandler[T, Promise[T]] {
  def completed(result: T, promise: Promise[T]): Unit = {
    promise.success(result)
  }
  def failed(e: Throwable, promise: Promise[T]): Unit = {
    promise.failure(e) // need to think about this ...
  }
}