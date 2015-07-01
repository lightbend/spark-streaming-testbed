package com.typesafe.spark

case class Config(
    master: String,
    hostname: String,
    port: Int,
    batchInterval: Int,
    strategy: String,
    streams: Int,
    step: Int)