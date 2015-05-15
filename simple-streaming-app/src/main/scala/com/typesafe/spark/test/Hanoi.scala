package com.typesafe.spark.test

import scala.annotation.tailrec
import Hanoi._
import scala.collection.immutable.Stream.consWrapper

object Hanoi {

  def solve(size: Int) {
    @tailrec
    def loop(state: State, steps: Stream[Move]) {
      if (steps.isEmpty) {
      } else {
        val move = steps.head
        loop(state.move(move.from, move.to), steps.tail)
      }
    }

    loop(State((1 to size).to[List], Nil, Nil), moveTower(size, A, C))
  }

  private sealed trait Tower
  private case object A extends Tower
  private case object B extends Tower
  private case object C extends Tower

  private def otherTower(n: Tower, m: Tower): Tower = {
    if ((n == B || n == C) && (m == B || m == C)) {
      A
    } else if ((n == A || n == C) && (m == A || m == C)) {
      B
    } else if ((n == A || n == B) && (m == A || m == B)) {
      C
    } else {
      throw new Exception("impossible state")
    }
  }

  private def moveTower(depth: Int, from: Tower, to: Tower): Stream[Move] = {
    if (depth == 1)
      Move(from, to) #:: Stream.empty
    else {
      val other = otherTower(from, to)
      moveTower(depth - 1, from, other) ++ Move(from, to) #:: moveTower(depth - 1, other, to)
    }
  }

  private case class Move(from: Tower, to: Tower)

  private case class State(a: List[Int], b: List[Int], c: List[Int]) {
    def move(from: Tower, to: Tower): State = {
      (from, to) match {
        case (A, B) =>
          State(a.tail, a.head :: b, c)
        case (A, C) =>
          State(a.tail, b, a.head :: c)
        case (B, A) =>
          State(b.head :: a, b.tail, c)
        case (B, C) =>
          State(a, b.tail, b.head :: c)
        case (C, A) =>
          State(c.head :: a, b, c.tail)
        case (C, B) =>
          State(a, c.head :: b, c.tail)
        case _ =>
          throw new Exception("impossible state")
      }
    }

    override def toString: String = {
      s"Tower( ${a.headOption.getOrElse(0)} , ${b.headOption.getOrElse(0)} , ${c.headOption.getOrElse(0)} )"
    }
  }
}

