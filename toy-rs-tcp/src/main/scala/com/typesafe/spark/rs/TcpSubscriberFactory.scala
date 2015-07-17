package com.typesafe.spark.rs

import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.AsynchronousServerSocketChannel
import java.net.InetSocketAddress
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

class TcpSubscriberFactory (port: Int) {
  
  def bind(onConnect: (TcpSubscriber) => Unit) {
    val server = AsynchronousServerSocketChannel.open()

    server.bind(new InetSocketAddress(port))
    acceptConnections(server, onConnect)
  }
  
  private def acceptConnections(server: AsynchronousServerSocketChannel, onConnect: (TcpSubscriber) => Unit) {
    val connectionAcceptedPromise = Promise[AsynchronousSocketChannel]()

    server.accept(connectionAcceptedPromise, new CompletionHandlerForPromise[AsynchronousSocketChannel])

    connectionAcceptedPromise.future.foreach {
      socket =>
        onConnect(new TcpSubscriber(socket))
        acceptConnections(server, onConnect)
    }
  }
}
