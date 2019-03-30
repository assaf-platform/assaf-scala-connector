package com
import upickle.default.{macroRW, ReadWriter => RW}

package object wonderkind {

  case class IncomingMessage(body: String, @upickle.implicits.key("request-id") requestId: Int, options: Map[String, Seq[String]])

  object IncomingMessage {
    implicit val rw: RW[IncomingMessage] = macroRW
  }

  case class OutgoingMessage[P](version: Int, profile_name: String, @upickle.implicits.key("request-id") requestId: Int, body: P)

}
