package com.typesafe.spark

import scala.util.Try

case class Config(
    master: String,
    hostname: String,
    ports: List[Int],
    pid: Option[PIDConfig],
    reactivePorts: List[Int],
    batchInterval: Int,
    strategy: String,
    step: Int)

case class PIDConfig(
  proportional: Double,
  integral: Double,
  derivative: Double)

object PIDConfig {

  private val RAW_REGEX = "([^,]*),([^,]*),([^,]*)".r

  def parse(raw: String): Either[String, PIDConfig] = {
    raw match {
      case RAW_REGEX(pString, iString, dString) =>
        val t = Try {
          val p = pString.toDouble
          val i = iString.toDouble
          val d = dString.toDouble
          PIDConfig(p, i, d)
        }
        t.map(Right(_)) getOrElse (Left(s"'$raw' is not valid PID parameters: <p>,<i>,<d>"))
    }
  }
}