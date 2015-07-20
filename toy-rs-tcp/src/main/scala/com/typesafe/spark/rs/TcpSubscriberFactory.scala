package com.typesafe.spark.rs

import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.AsynchronousServerSocketChannel
import java.net.InetSocketAddress
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

class TcpSubscriberFactory private (val port: Int, server: AsynchronousServerSocketChannel) {

  def onConnect(callback: (TcpSubscriber) => Unit) {
    acceptConnections(callback)
  }

  private def acceptConnections(onConnect: (TcpSubscriber) => Unit) {
    val connectionAcceptedPromise = Promise[AsynchronousSocketChannel]()

    server.accept(connectionAcceptedPromise, new CompletionHandlerForPromise[AsynchronousSocketChannel])

    connectionAcceptedPromise.future.foreach {
      socket =>
        onConnect(new TcpSubscriber(socket))
        acceptConnections(onConnect)
    }
  }

  def stop() {
    server.close()
  }

}

object TcpSubscriberFactory {
  def apply(port: Int): TcpSubscriberFactory = {
    val server = AsynchronousServerSocketChannel.open()

    server.bind(new InetSocketAddress(port))

    new TcpSubscriberFactory(port, server)
  }
}