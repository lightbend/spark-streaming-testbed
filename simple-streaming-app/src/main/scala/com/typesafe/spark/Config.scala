package com.typesafe.spark

import scala.util.Try

case class Config(
    master: String,
    hostname: String,
    ports: List[Int],
    pid: Boolean,
    reactivePorts: List[Int],
    batchInterval: Int,
    strategy: String,
    step: Int)

