package com.typesafe.spark

case class Config(
    master: String,
    hostname: String,
    ports: List[Int],
    batchInterval: Int,
    strategy: String,
    step: Int)