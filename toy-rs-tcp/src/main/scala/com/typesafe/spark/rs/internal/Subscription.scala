package com.typesafe.spark.rs.internal

import java.nio.channels.AsynchronousSocketChannel
import java.nio.ByteBuffer

class Subscription(socket: AsynchronousSocketChannel) extends org.reactivestreams.Subscription {
  def cancel(): Unit = {
    socket.close()
  }

  def request(n: Long): Unit = {
    socket.write(ByteBuffer.wrap(s"$n\n".getBytes))
  }
}