package com.wonderkind

import akka.{Done, NotUsed}
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource, CommittableReadResult}
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future
import com.typesafe.config._

class AMQP(config: Config) {
  def this() {
    this(ConfigFactory.load())
  }
  config.checkValid(ConfigFactory.defaultReference(), "assaf-connect")

  val amqpConnection = AmqpUriConnectionProvider(config.getString("assaf-connect.amqp.rabbit_uri"))

  val incomingQueueName = config.getString("assaf-connect.app_name")
  val incomingExchange = ExchangeDeclaration("ds-exchange", "direct").withDurable(true).withAutoDelete(false)
  val incomingQueue = QueueDeclaration(incomingQueueName).withAutoDelete(false).withExclusive(false).withDurable(true)
  val incomingBindingDeclaration = BindingDeclaration(incomingQueueName, "ds-exchange").withRoutingKey(incomingQueueName)
  val sourceConfig = NamedQueueSourceSettings(amqpConnection, incomingQueueName)
    .withDeclarations(List(incomingQueue, incomingExchange, incomingBindingDeclaration))
  val amqpSource: Source[CommittableReadResult, NotUsed] =
    AmqpSource.committableSource(sourceConfig, config.getInt("assaf-connect.amqp.buffer_size"))

  val outgoingExchangeName = "pubsub"
  val outgoingQueueName = "response_pubsub"

  val outgoingExchange = ExchangeDeclaration(outgoingExchangeName, "topic").withDurable(false).withAutoDelete(true)
  val outgoingQueue = QueueDeclaration(outgoingQueueName).withAutoDelete(true).withExclusive(false).withDurable(false)
  val outgoingBindingDeclaration = BindingDeclaration(outgoingQueueName, outgoingExchangeName)

  val amqpSink: Sink[WriteMessage, Future[Done]] =
    AmqpSink(
      AmqpWriteSettings(amqpConnection)
        .withExchange(outgoingExchangeName)
        .withDeclarations(List(outgoingExchange, outgoingQueue, outgoingBindingDeclaration)))


}