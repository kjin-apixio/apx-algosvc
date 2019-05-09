package com.apixio.algoservice.producer

import akka.actor.ActorRef
import com.apixio.algoservice.util.JaksonUtils
import com.apixio.app.manager.actor.kafka.KafkaProducerActor.SendString
import com.apixio.signalmanager.eventhandler.messages.DocumentAddressMessage
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class MessageAndStatusProducer(kafkaProducer: ActorRef)(implicit executionContext: ExecutionContext) {
  val logger = LoggerFactory.getLogger(getClass.getName)

  def producerMessageAndStatusIntoKafka(msg: DocumentAddressMessage, isSuccessForPredictionAndMetaDataStored: Boolean, predictionStatusTopic: String) {
    val messageWithStatus: String = JaksonUtils.messageAndStatusToString(msg, isSuccessForPredictionAndMetaDataStored)
    Try {
      this.kafkaProducer ! SendString(predictionStatusTopic, messageWithStatus)
    } // try
    match {
      case Success(success) => {
        logger.info(s"Message succeeded sent: $messageWithStatus.")
      }
      case Failure(ex) => {
        logger.warn(s"Message failed been sent. Error: ${ex.getMessage}")
      }
    } // match
  }
}
