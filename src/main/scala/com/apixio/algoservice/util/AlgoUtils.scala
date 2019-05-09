package com.apixio.algoservice.util

import java.nio.charset.StandardCharsets._
import java.nio.file.{Files, Paths}
import java.util

import com.apixio.signalmanager.messaging.util.ObjectMapProvider
import org.apache.kafka.clients.consumer.ConsumerRecord
import com.apixio.signalmanager.corelib.model.SignalGeneratorDetail
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import scala.util.{Failure, Success, Try}


object AlgoUtils {
  final val MSGWithStatusTopic: String = "DocumentAddressMessageAndStatus"
  val logger = LoggerFactory.getLogger(getClass.getName)
  def recordInfo(record: ConsumerRecord[Array[Byte], String]): String = {
    s"topic: ${record.topic()} .partition: ${record.partition()}. offset:${record.offset()}"
  }

  def toLoggable(message: Any): String = ObjectMapProvider.toLoggable(message)

  def readFile(file: String): String = Try(new String(Files.readAllBytes(Paths.get(file)), UTF_8)) match {
    case Success(text) => text
    case Failure(ex) =>
      logger.error(s"Unable to read file $file error: ${ex.getMessage}")
      throw ex
  }

  def loadActiveSigGens(filename: String): util.List[SignalGeneratorDetail] = {
    val json: String = readFile(filename)
    val mapper: ObjectMapper = new ObjectMapper
    val listType: TypeReference[util.List[SignalGeneratorDetail]] = new TypeReference[util.List[SignalGeneratorDetail]]() {}
    mapper.readValue(json, listType)
  }

}
