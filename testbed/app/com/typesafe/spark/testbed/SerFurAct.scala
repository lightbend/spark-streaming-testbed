package com.typesafe.spark.testbed

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import akka.actor.ActorRef
import java.net.InetSocketAddress

object Server {

  def apply(newConnectionHandler: ActorRef, port: Int): Server = {

    val serverSocket = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port))

    val server = new Server(serverSocket, newConnectionHandler)

    server.acceptConnections()
    
    server
  }
  
  case class IncomingConnectionMsg(socket: AsynchronousSocketChannel)
  object ConnectionClosedMsg

}

class Server private (serverSocket: AsynchronousServerSocketChannel, newConnectionHandler: ActorRef) {
  
  import Server._

  private def acceptConnections() {
    val connectionAcceptedPromise = Promise[AsynchronousSocketChannel]()

    serverSocket.accept(connectionAcceptedPromise, new CompletionHandlerForPromise[AsynchronousSocketChannel])

    connectionAcceptedPromise.future.foreach {
      socket =>
        newConnectionHandler ! IncomingConnectionMsg(socket)
        acceptConnections()
    }
  }
  
  def close() {
    serverSocket.close()
  }

}

object Connection {

  def apply(socket: AsynchronousSocketChannel, connectionHandler: ActorRef) {
    val conn = new Connection(socket, connectionHandler)
    // Initiating the processing incoming bytes
    conn.process()
  }
}

class Connection(socket: AsynchronousSocketChannel, connectionHandler: ActorRef) {

  def process() {

    val buffer = ByteBuffer.allocate(1024)

    val promise = Promise[Integer]

    socket.read(buffer, promise, new CompletionHandlerForPromise[Integer])

    promise.future foreach {
      byteRead =>
        if (byteRead < 0) {
          // time to close
          socket.close()
          connectionHandler ! Server.ConnectionClosedMsg
        } else {
          // don't do anything on incoming bytes, just wait for the end of the stream
        	process()
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