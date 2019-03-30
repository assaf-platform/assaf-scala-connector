package com.wonderkind

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import upickle.default.{macroRW, ReadWriter => RW}

object Main extends App {
  implicit val system = ActorSystem("assaf-connector")
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.getDispatcher

  implicit val rw: RW[OutgoingMessage[String]] = macroRW

  val amqp = new AMQP()
  val flow = new AssafFlow(v => {
    1.0
  })
  flow.connect(amqp.amqpSource, amqp.amqpSink).run()
  //system.terminate()
}
