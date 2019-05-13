package com.apixio.algoservice.injection

import java.net.URI
import java.nio.file.{Path, Paths}
import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.apixio.algoservice.producer.MessageAndStatusProducer
import com.apixio.algoservice.consumer.DocumentAddressMessageConsumer
import com.apixio.algoservice.manager.DaemonManager
import com.apixio.app.manager.actor.kafka.KafkaProducerActor
import com.apixio.algoservice.algo.Combiner
import com.apixio.dao.utility.DaoServices
import com.apixio.mcs.client.combiner.CombinerMaterializer
import com.apixio.scala.dw.ApxConfiguration
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class ServiceModule extends AbstractModule {
  var daoServices: DaoServices = _
  val servicesCache = mutable.HashMap[String, Any]()
  var apxConfiguration: ApxConfiguration = _
  var kafkaProducer: ActorRef = _
  val logger = LoggerFactory.getLogger(getClass.getName)
  val catalog_service_rest_endpoint = "catalog_service_rest_endpoint"
  val catalog_service_local_cache_dir = "catalog_service_local_cache_dir"
  val logS3Ops = "logS3Ops"

  implicit val system = ActorSystem("KafkaConsumer")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withInputBuffer(initialSize = 32, maxSize = 32))(system)
  implicit val ingestPool: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  override def configure(): Unit = {
  }

  /**
    * Initial all services to be provided and place them in the cache for look up later
    */
  def init(config: ApxConfiguration, env: Environment, dao: DaoServices) = {
    daoServices = dao
    apxConfiguration = config
    kafkaProducer = system.actorOf(KafkaProducerActor.props(config.kafka), KafkaProducerActor.name)

    logger.info("Init ServiceModule...")
  }

  @Provides
  @Named(ServiceNames.messageAndStatusProducer)
  def getProjectManager : MessageAndStatusProducer = {
    synchronized[MessageAndStatusProducer] {
      servicesCache.getOrElse(ServiceNames.messageAndStatusProducer, {
        val messageAndStatusProducer = new MessageAndStatusProducer(kafkaProducer)
        servicesCache.put(ServiceNames.messageAndStatusProducer, messageAndStatusProducer)
        messageAndStatusProducer
      }).asInstanceOf[MessageAndStatusProducer]
    }
  }

  @Provides
  @Named(ServiceNames.combiner)
  def getCombiner(@Named(ServiceNames.combinerMaterializer) combinerMaterializer: CombinerMaterializer): Combiner = {
    synchronized[Combiner] {
      servicesCache.getOrElse(ServiceNames.combiner, {
        val logS3OpsVal: String = apxConfiguration.propertyHelperConfig.getOrElse(logS3Ops, "")
        var logS30psInBoolean = false
        if(!logS3OpsVal.isEmpty() && logS3OpsVal.equals("true")) logS30psInBoolean = true
        val combiner = new Combiner(combinerMaterializer, daoServices, logS30psInBoolean)
        servicesCache.put(ServiceNames.combiner, combiner)
        combiner
      }).asInstanceOf[Combiner]
    }
  }

  @Provides
  @Named(ServiceNames.combinerMaterializer)
  def getCombinerMaterializer(): CombinerMaterializer = synchronized[CombinerMaterializer] {
    servicesCache.getOrElse(ServiceNames.combiner, {
      val propertyHelperConfig: Map[String, String] = apxConfiguration.propertyHelperConfig
      val endpoint: String = propertyHelperConfig.getOrElse(catalog_service_rest_endpoint, "")
      val mcsServiceUri = new URI(endpoint)

      val dir: String = propertyHelperConfig.getOrElse(catalog_service_local_cache_dir, "")
      val homeDir: Path = Paths.get(dir)

      val combinerMaterializer: CombinerMaterializer = CombinerMaterializer.createCombinerMaterializer(mcsServiceUri: URI, this.daoServices: DaoServices, homeDir: Path)

      servicesCache.put(ServiceNames.combinerMaterializer, combinerMaterializer)
      combinerMaterializer
    }).asInstanceOf[CombinerMaterializer]
  }

  @Provides
  @Named(ServiceNames.documentAddressMessageConsumer)
  def getDocumentAddressMessageConsumer(@Named(ServiceNames.combiner) combiner: Combiner,
                                        @Named(ServiceNames.messageAndStatusProducer) messageAndStatusProducer: MessageAndStatusProducer)
  : DocumentAddressMessageConsumer = {
    synchronized[DocumentAddressMessageConsumer] {
      servicesCache.getOrElse(ServiceNames.documentAddressMessageConsumer, {
        val topicConfig = apxConfiguration.application("documentAddressMessageConsumer").asInstanceOf[Map[String, Object]]
        val brokerList = topicConfig("brokerList").asInstanceOf[String]
        val consumerGroup = topicConfig("consumerGroup").asInstanceOf[String]
        val topic = topicConfig("topic").asInstanceOf[String]
        val consumerParallelismFactor = topicConfig("consumerParallelismFactor").asInstanceOf[Int]
        val commitParallelismFactor = topicConfig("commitParallelismFactor").asInstanceOf[Int]
        val numRetries = topicConfig("numRetries").asInstanceOf[Int]
        val batchSize = topicConfig("batchSize").asInstanceOf[Int]
        val maxInterval: FiniteDuration = new FiniteDuration(topicConfig("maxIntervalInSecs").asInstanceOf[Int], TimeUnit.SECONDS)
        val auditorPredictionTopicPrefix = topicConfig("auditorPredictionTopicPrefix").asInstanceOf[String]
        val maPredictionTopicPrefix = topicConfig("maPredictionTopicPrefix").asInstanceOf[String]


        val documentAddressMessageConsumer = new DocumentAddressMessageConsumer(brokerList, consumerGroup, topic, consumerParallelismFactor, commitParallelismFactor,
          numRetries, batchSize, maxInterval, auditorPredictionTopicPrefix, maPredictionTopicPrefix, combiner, messageAndStatusProducer)
        servicesCache.put(ServiceNames.documentAddressMessageConsumer, documentAddressMessageConsumer)
        documentAddressMessageConsumer
      }).asInstanceOf[DocumentAddressMessageConsumer]
    }
  }




  @Provides
  @Named(ServiceNames.daemonManager)
  def getDaemonManager(@Named(ServiceNames.documentAddressMessageConsumer) documentAddressMessageConsumer: DocumentAddressMessageConsumer
                       ): DaemonManager = {
    synchronized[DaemonManager] {
      servicesCache.getOrElse(ServiceNames.daemonManager, {
        val daemonManager = new DaemonManager
        daemonManager.documentAddressMessageConsumer = documentAddressMessageConsumer
        servicesCache.put(ServiceNames.daemonManager, daemonManager)
        daemonManager
      }).asInstanceOf[DaemonManager]
    }
  }
}

/**
  * Names of all the services in this module
  */
object ServiceNames {
  // final val appSignalCombinerPool = "appSignalCombinerPool"
  final val daemonManager = "daemonManager"
  final val combiner = "combiner"
  final val documentAddressMessageConsumer = "documentAddressMessageConsumer"
  final val messageAndStatusProducer = "messageAndStatusProducer"
  final val combinerMaterializer = "combinerMaterializer"
}
