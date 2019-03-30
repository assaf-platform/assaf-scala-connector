package com.wonderkind

import akka.actor.{Actor, Props}
import akka.stream.OverflowStrategy
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.alpakka.amqp.{AmqpConnectorSettings, BindingDeclaration, ExchangeDeclaration, QueueDeclaration}
import akka.stream.scaladsl.Source
import akka.stream.stage.{AsyncCallback, GraphStageLogic}
import akka.util.{ByteString, Timeout}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

trait AmqpConnectorLogic { this: Actor =>

  private var connection: Connection = _
  protected var channel: Channel = _

//  protected lazy val shutdownCallback: AsyncCallback[Throwable] = getAsyncCallback(onFailure)
//  private lazy val shutdownListener = new ShutdownListener {
////    override def shutdownCompleted(cause: ShutdownSignalException): Unit = shutdownCallback.invoke(cause)
//  }

  def settings: AmqpConnectorSettings
  def whenConnected(): Unit
//  def onFailure(ex: Throwable): Unit = failStage(ex)

  final override def preStart(): Unit =
    try {
      connection = settings.connectionProvider.get
      channel = connection.createChannel

//      connection.addShutdownListener(shutdownListener)
//      channel.addShutdownListener(shutdownListener)

      import scala.collection.JavaConverters._

      settings.declarations.foreach {
        case d: QueueDeclaration =>
          channel.queueDeclare(
            d.name,
            d.durable,
            d.exclusive,
            d.autoDelete,
            d.arguments.asJava
          )

        case d: BindingDeclaration =>
          channel.queueBind(
            d.queue,
            d.exchange,
            d.routingKey.getOrElse(""),
            d.arguments.asJava
          )

        case d: ExchangeDeclaration =>
          channel.exchangeDeclare(
            d.name,
            d.exchangeType,
            d.durable,
            d.autoDelete,
            d.internal,
            d.arguments.asJava
          )
      }

      whenConnected()
    } catch {
      case NonFatal(e) => println(e)
    }

  /** remember to call if overriding! */
  override def postStop(): Unit = {
    if ((channel ne null) && channel.isOpen) channel.close()
    channel = null

    if (connection ne null) {
//      connection.removeShutdownListener(shutdownListener)
      settings.connectionProvider.release(connection)
      connection = null
    }
  }
}
class ActorSource(_settings: AmqpConnectorSettings)  extends Actor with AmqpConnectorLogic{

  override def receive: Receive = {
    case _ => println("got something")
      channel.queueBind("scalainitial", "ds-exchange", "")
      channel.basicConsume("scalainitial", false, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String,
                                  envelope: Envelope,
                                  properties: BasicProperties,
                                  body: Array[Byte]): Unit = {
        sender() ! body
      }
    })
  }

  override def settings: AmqpConnectorSettings = _settings

  override def whenConnected(): Unit = println("connected")
}

object ActorSource{
  implicit val to = Timeout.durationToTimeout(FiniteDuration.apply(10, "seconds"))

  def apply(s: akka.actor.ActorSystem, amqpConnectorSettings: AmqpConnectorSettings) = {
    val props = Props(classOf[ActorSource], amqpConnectorSettings)
    val actor = s.actorOf(props)

    actor.tell("bla", actor)
    Source.actorRef[ByteString](1, OverflowStrategy.fail)
      .ask[CommittableReadResult](8)(actor)
  }
}

