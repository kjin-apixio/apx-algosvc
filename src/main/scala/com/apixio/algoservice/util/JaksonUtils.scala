package com.apixio.algoservice.util

import com.apixio.XUUID
import com.apixio.signalmanager.eventhandler.messages.DocumentAddressMessage
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.slf4j.LoggerFactory


object JaksonUtils {
  // when use kafka to generate the message, keep the same style
  // when deconstruct the DocumentAddressMessageIn, it need to construct in the same way
  case class DocumentAddressMessageInJsonString(documentUuid: String, pdsUuid: String, patientUuid: String, docsetUuid: String)
  //case class XUUIDInJsonString (xuuid_type: String, uuid_in_string: String, typeIncludesSeparator: String, obeyTypeCase: String)
  case class DocumentAddressMessageWithStatus(documentUuid: String, pdsUuid: String, patientUuid: String, docsetUuid: String, status: String)

  val logger = LoggerFactory.getLogger(getClass.getName)
  val objectMapper = new ObjectMapper() with ScalaObjectMapper
  objectMapper.registerModule(DefaultScalaModule)


  def fromStringToDocumentAddressMessage(data: String): DocumentAddressMessage = {
    val msgInJson: DocumentAddressMessageInJsonString = getDocumentAddressMessageInJsonString(data)

    val documentUuid: XUUID = XUUID.fromString(msgInJson.documentUuid)
    val pdsUuid: XUUID = XUUID.fromString(msgInJson.pdsUuid)
    val patientUuid: XUUID = XUUID.fromString(msgInJson.patientUuid)
    val docsetUuid: XUUID = XUUID.fromString(msgInJson.docsetUuid)
    logger.debug("documentUuid: "+documentUuid.toString)
    logger.debug("pdsUuid: "+pdsUuid.toString)
    logger.debug("patientUuid: "+patientUuid.toString)
    logger.debug("docsetUuid: "+docsetUuid.toString)

    // parameters sequence: (@JsonProperty("document_uuid") XUUID documentUuid, @JsonProperty("patient_uuid") XUUID patientUuid, @JsonProperty("pds_uuid") XUUID pdsUuid, @JsonProperty("document_set_uuid") XUUID docsetUuid) {
    return new DocumentAddressMessage(documentUuid, patientUuid, pdsUuid, docsetUuid)
  }


  def getDocumentAddressMessageInJsonString(data: String): DocumentAddressMessageInJsonString = {
    val documentAddressMessageInJsonString = objectMapper.readValue(data, classOf[DocumentAddressMessageInJsonString])
    return documentAddressMessageInJsonString
  }


  def messageAndStatusToString(msg: DocumentAddressMessage, isSuccessForPredictionAndMetaDataStored: Boolean): String = {
    val documentUuid: XUUID = msg.getDocumentUuid
    val pdsUuid: XUUID = msg.getPdsUuid
    val patientUuid: XUUID = msg.getPatientUuid
    val docsetUuid: XUUID = msg.getDocsetUuid
    val status: String = {
      if(isSuccessForPredictionAndMetaDataStored == true) "Success"
      else "False"
    }

    val msgWithStatus = new DocumentAddressMessageWithStatus(documentUuid.toString, pdsUuid.toString, patientUuid.toString, docsetUuid.toString, status)
    return objectMapper.writeValueAsString(msgWithStatus)
  }
}
