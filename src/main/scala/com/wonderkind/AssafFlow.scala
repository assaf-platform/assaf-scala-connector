package com.wonderkind

import akka.stream.alpakka.amqp.WriteMessage
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import upickle.default.{macroRW, ReadWriter => RW}

import scala.concurrent.Future

class AssafFlow[Q](func: String => Q)(implicit evidence: RW[Q]) {
  implicit val rw: RW[OutgoingMessage[Q]] = macroRW

  def apply(body: Q, requestId: Int, request: String): OutgoingMessage[Q] = OutgoingMessage[Q](1, request, requestId, body)


  def decodeAmqpMessage(crr: CommittableReadResult) = crr.message.bytes.decodeString("utf-8")
  def connect[T](amqpSource: Source[CommittableReadResult, T], amqpSink: Sink[WriteMessage, Future[Done]]) =
    amqpSource
      .map{ crr =>

        val assfMsg = upickle.default.read[IncomingMessage](decodeAmqpMessage(crr))
        (crr, assfMsg)
      }
      .map { v =>
        val crr = v._1
        val res = func(v._2.body)
        (crr, OutgoingMessage[Q](1, v._2.body, v._2.requestId, res))
      }
      .map { v =>
        v._1.ack()
        v._2
      }
      .map{(v: OutgoingMessage[Q]) =>
        (v.requestId, upickle.default.writeJs(v))
      }
      .map(v => (v._1, ByteString(v._2.render())))
      .map(v => WriteMessage(v._2).withRoutingKey(s"${v._1}"))
      .map { v =>
        println(v)
        v
      }
      .to(amqpSink)
}
