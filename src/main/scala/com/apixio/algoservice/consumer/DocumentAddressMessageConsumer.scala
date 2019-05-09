package com.apixio.algoservice.consumer

import java.util.concurrent.TimeUnit

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, DelayOverflowStrategy, Supervision}
import com.apixio.signalmanager.eventhandler.messages.DocumentAddressMessage
import com.apixio.algoservice.util.JaksonUtils._
import com.apixio.algoservice.util.AlgoUtils._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import com.apixio.algoservice.algo.Combiner
import com.apixio.algoservice.producer.MessageAndStatusProducer
import scala.util.{Failure, Success, Try}

case class DocumentAddressMessageConsumer(brokers: String,
                                          consumerGroup: String,
                                          topic: String,
                                          consumerParallelismFactor: Int = 5,
                                          commitParallelismFactor: Int = 3,
                                          numRetries: Int = 5,
                                          batchSize: Int = 10,
                                          maxInterval: FiniteDuration = 60.seconds,
                                          auditorPredictionTopicPrefix: String,
                                          maPredictionTopicPrefix: String,
                                          combiner: Combiner,
                                          messageAndStatusProducer: MessageAndStatusProducer
                                    )
                                    (implicit system: ActorSystem, materializer: ActorMaterializer, ingestPool: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(brokers)
    .withGroupId(consumerGroup)
    //    .withMaxWakeups(maxWakeUp)
    //    .withWakeupTimeout(maxWakeupDuration)
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withCommitRefreshInterval(Duration.apply(15, TimeUnit.MINUTES))

  def consume(): Future[Done] = {
    // consumer message

    // process the message, do prediction


    // generate the processed message into Kafka



    val result: Source[ConsumerMessage.CommittableOffset, NotUsed] = RestartSource.withBackoff(minBackoff = 3.seconds, maxBackoff = 30.seconds, randomFactor = 0.2) { () =>
      Consumer.committableSource(consumerSettings, Subscriptions.topicPattern(topic))
        .mapAsync(consumerParallelismFactor) { msg: ConsumerMessage.CommittableMessage[Array[Byte], String] =>
          process(msg).map(_ => msg.committableOffset)
        }.withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
        .mapMaterializedValue(_ => akka.NotUsed)
    }

    result.recoverWithRetries(numRetries, {
      case e: Throwable => result.delay(5.seconds, DelayOverflowStrategy.backpressure)
    }).groupedWithin(batchSize, maxInterval)
      .map(batch => {
        logger.info(s"Processing a batch of ${batch.size} elements")
        batch.foldLeft(CommittableOffsetBatch.empty) { (batch, elem) => batch.updated(elem) }
      })
      .mapAsync(commitParallelismFactor)(
        _.commitScaladsl()
      ).runWith(Sink.ignore)
  }

  private def process(message: ConsumerMessage.CommittableMessage[Array[Byte], String]): Future[Done] = {
    val data: String = message.record.value
    logger.debug(s"Got Message: $data")

    // recover the data from String to DocumentAddressMessage and produce the message
    Try(fromStringToDocumentAddressMessage(data)) match {
      case Success(msg: DocumentAddressMessage) => {
        val isSuccessForPredictionAndMetaDataStored: Boolean = this.combiner.process(msg)
        this.messageAndStatusProducer.producerMessageAndStatusIntoKafka(msg, isSuccessForPredictionAndMetaDataStored, MSGWithStatusTopic)
      }
      case Failure(e: Throwable) => {
        logger.error("Error consuming record [" + recordInfo(message.record) + "]", e)
      }
    }
    Future.successful(Done)
  }
}

