package com.apixio.algoservice.algo

import java.util

import com.apixio.mcs.client.combination.CombinerModelCombination
import scala.collection.JavaConverters._
import com.apixio.XUUID
import com.apixio.algoservice.common.EnvConfig._
import com.apixio.dao.utility.{DaoServices, DaoServicesSet}
import com.apixio.ensemble.ifc._
import com.apixio.messaging.messages.PredictionMessage
import com.apixio.model.event.EventType
import com.apixio.restbase.config.ConfigSet
import com.apixio.signalmanager.SignalObjectContainer
import com.apixio.signalmanager.bizlogic.SignalLogic
import com.apixio.signalmanager.eventhandler.predictions.{AppExecutionContext, AppSignalCombinationParams}
import com.apixio.signalmanager.eventhandler.util.Utils.DEFAULT_SENT_DT
import com.apixio.algoservice.util.AlgoUtils._
import com.apixio.signalmanager.eventhandler.messages.DocumentAddressMessage
import org.slf4j.LoggerFactory
import com.apixio.mcs.client.combiner.{CombinerExecutionContext, CombinerMaterializer}
import org.joda.time.DateTime
import com.apixio.mcs.client.context.ExecutionContextReporter
import com.apixio.mcs.client.context.PlatformServicesFactory
import scala.util.{Failure, Success, Try}

class Combiner (val combinerMaterializer: CombinerMaterializer, val daoServices: DaoServices, val logS3Ops: Boolean){
  var combiner: ManagedLifecycle[SignalCombinationParams, util.List[EventType]] = _
  var signalLogic: SignalLogic = _

  initializeCombiners()

  def initializeCombiners(): Unit = {
    val mcId = "B_3693aa6b-f92a-4bb5-8494-9d1eadac3890"
    val modelCombination: CombinerModelCombination = combinerMaterializer.resolveLocalCombination(mcId)
    combiner = combinerMaterializer.createCombiner(modelCombination);
    val platformServicesFactory: PlatformServicesFactory = new PlatformServicesFactory(daoServices, logS3Ops)
    combiner.setup(CombinerExecutionContext.builder
      .configuration(modelCombination.configurationData)
      .reporter(new ExecutionContextReporter())
      .platformServices(platformServicesFactory.platformServices)
      .submitTime(new DateTime)
      .patientDataSet("0000")
      .workName("").build)

    val logger = LoggerFactory.getLogger(classOf[Combiner])
    val daoConfig = ConfigSet.fromMap(daos)
    this.signalLogic = new SignalLogic(daoServices, daoConfig, null)
    signalLogic.setOverrideActiveSignalGenList(loadActiveSigGens("siggens_cr.pretty.json"))
/*
    val logger = LoggerFactory.getLogger(classOf[Combiner])
    // create combinerNums number of combiner, currently create one combiner for test
    val daoConfig: ConfigSet = ConfigSet.fromMap(daos)
    val daoServices: DaoServices = DaoServicesSet.createDaoServices(daoConfig)
    this.signalLogic = new SignalLogic(daoServices, daoConfig, null)
    val patientLogic = signalLogic.getPatientLogic
    signalLogic.setOverrideActiveSignalGenList(loadActiveSigGens("siggens_cr.pretty.json"))

    lazy val factory: SignalCombinerFactory = new SignalCombinerFactory
    val generator: Generator = new Generator
    generator.setName("OneCombinerToRuleThemAll")
    generator.setVersion("1.0.0")
    generator.setClassName("com.apixio.ensemble.impl.combiners.OneCombinerToRuleThemAll")
    generator.setModelVersion("0000")
    val config = ensembleProperties.asScala.mapValues(x => String.valueOf(x)).asJava
    val context = new AppExecutionContext(null, null, daoServices.getS3Ops, signalLogic.getPatientLogic, config)
    val comStart = System.currentTimeMillis
    this.combiner = factory.create(generator, context.getConfiguration)
    val comEnd = System.currentTimeMillis
    this.combiner.setup(context)
    val combinerSetupTime = comEnd - comStart
    logger.info(s"Setup combiner object in ${combinerSetupTime}ms")
    */

  }

  def process(msg: DocumentAddressMessage): Boolean = Try {
    val messageStart = System.currentTimeMillis

    val events = doPrediction(msg)

    writeMetaData(msg, events._1, events._2, events._3, events._4)

    val messageEnd = System.currentTimeMillis

    val messageTime = messageEnd - messageStart
    logger.info(s"Processed message in ${messageTime}ms")
  } match {
    case Success(success) => {
      logger.info(s"Message succeeded predicted.")
      true
    }
    case Failure(ex) => {
      logger.warn(s"Message failed predicted. Error: ${ex.getMessage}")
      false
    }
  }


  private def doPrediction(msg: DocumentAddressMessage) = {
    val docUuid = msg.getDocumentUuid
    val patUuid = msg.getPatientUuid
    val docsetUuid = msg.getDocsetUuid

    // build signals
    val allSignalsTriple = buildSignals(msg)

    val docSignalReadTime = allSignalsTriple._2
    val patSignalReadTime = allSignalsTriple._3

    val allSignals = allSignalsTriple._1

    // generate predictions
    val eventsPair = generatePredictions(msg, allSignals)
    val events = eventsPair._1
    val predGenTime = eventsPair._2

    (events, docSignalReadTime, patSignalReadTime, predGenTime)
  }

  private def writeMetaData(msg: DocumentAddressMessage, events: util.List[EventType], patSignalReadTime: Long, docSignalReadTime: Long, predGenTime: Long): Unit = {
    val docUuid = msg.getDocumentUuid
    val patUuid = msg.getPatientUuid
    val docsetUuid = msg.getDocsetUuid
    val pdsUuid = msg.getPdsUuid

    val projectUuid = signalLogic.getDocset(docsetUuid).projectId
    val predictionMessage = new PredictionMessage(docsetUuid, docUuid, patUuid, pdsUuid, projectUuid, "",
      System.currentTimeMillis, DEFAULT_SENT_DT, patSignalReadTime, docSignalReadTime, predGenTime)
    Try(signalLogic.writePredictions(predictionMessage, events)) match {
      case Success(result) =>
        logger.info(s"Saved predictions for for document: $docUuid and patient: $patUuid")
      case Failure(ex) =>
        logger.error(s"Unable to save predictions for for document: $docUuid and patient: $patUuid ${ex.getMessage}")
    }
  }


  def generatePredictions(msg: DocumentAddressMessage, allSignals: List[Signal]): (util.List[EventType], Long) ={
    val docUuid = msg.getDocumentUuid
    val patUuid = msg.getPatientUuid
    val docsetUuid = msg.getDocsetUuid

    val params = new AppSignalCombinationParams(docUuid, patUuid, docsetUuid, allSignals.asJava)
    val predStart = System.currentTimeMillis
    this.combiner.synchronized {
      val events = combiner.process(params)
      val predEnd = System.currentTimeMillis
      val predGenTime = predEnd - predStart
      logger.info(s"Generated ${events.size} predictions in ${predGenTime}ms for document: $docUuid and patient: $patUuid")
      return (events, predGenTime)
    }
  }


  def buildSignals(msg: DocumentAddressMessage):  (List[Signal], Long, Long) = {
    val docUuid = msg.getDocumentUuid
    val patUuid = msg.getPatientUuid
    val docsetUuid = msg.getDocsetUuid

    val docSignalsPair = readDocumentSignals(docUuid, docsetUuid)
    val docSignalReadTime = docSignalsPair._2

    val patSignalsPair = readPatientSignals(docUuid, patUuid, docsetUuid)
    val patSignalReadTime = patSignalsPair._2

    val allSignals = docSignalsPair._1 ++ patSignalsPair._1
    (allSignals, docSignalReadTime, patSignalReadTime)
  }


  def readDocumentSignals(docUuid: XUUID, docsetUuid: XUUID): (List[Signal], Long) = {
    val docStart = System.currentTimeMillis
    val docSerialized =
      signalLogic.readDocumentSignals(docUuid, docsetUuid).asScala.mapValues(_.asScala.toList).toMap
    logger.info("doc signal keys: " + docSerialized.keySet.mkString(","))
    val docSignals = docSerialized.flatMap{ case (key, list) => getSignals(key, list, None)}.toList
    val docEnd = System.currentTimeMillis
    val docSignalReadTime = docEnd - docStart
    logger.info(s"Read document $docUuid signals in ${docSignalReadTime}ms")
    (docSignals, docSignalReadTime)
  }


  def readPatientSignals(docUuid: XUUID, patUuid: XUUID, docsetUuid: XUUID): (List[Signal], Long) = {
    // read patient signals
    val patStart = System.currentTimeMillis
    val patSerialized =
      signalLogic.readPatientSignals(patUuid, docsetUuid).asScala.mapValues(_.asScala.toList).toMap
    logger.info("pat signal keys: " + patSerialized.keySet.mkString(","))
    val patSignals = patSerialized.flatMap{ case (key, list) => getSignals(key, list, Some(docUuid))}.toList
    val patEnd = System.currentTimeMillis
    val patSignalReadTime = patEnd - patStart
    logger.info(s"Read patient $patUuid signals in ${patSignalReadTime}ms")
    (patSignals, patSignalReadTime)
  }


  def getSignals(name: String, signals: List[Array[Byte]], docUuid: Option[XUUID]): List[Signal] = {
    val deserialized = signals
      .flatMap(bytes => SignalObjectContainer.deserialize(bytes).iterator.asScala)
      .filter(signal => validSignal(signal, docUuid))
    logger.info(s"Retrieved ${deserialized.size} signals for key: $name")
    deserialized
  }


  def validSignal(signal: Signal, docUuid: Option[XUUID]): Boolean = {
    val valid = docUuid match {
      case None => true // document signal
      case Some(doc) => signal.getSource match { // patient signal, check if it belongs to the document
        case ds: DocumentSource => doc.equals(ds.getDocumentId)
        case pws: PageWindowSource => doc.equals(pws.getDocumentId)
        case ps: PageSource => doc.equals(ps.getDocumentId)
        case _ => true
      }
    }
    if (!valid) logger.info(s"Skipping signal: $signal since it doesn't belong to doc: $docUuid")
    valid
  }
}
