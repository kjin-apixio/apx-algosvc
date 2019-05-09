package com.apixio.algoservice

import com.apixio.app.manager.DaoService
import com.apixio.bizlogic.patient.logic.PatientAssemblyLockUtil
import com.apixio.algoservice.injection.{ServiceModule, ServiceNames}
import com.apixio.algoservice.manager.DaemonManager
import com.apixio.scala.dw.{ApxCommonResource, ApxConfiguration, ApxServices}
import com.apixio.scala.logging.ApixioLoggable
import com.google.inject.Key
import com.google.inject.name.Names
import com.hubspot.dropwizard.guice.GuiceBundle
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.{Bootstrap, Environment}
import io.swagger.config.ScannerFactory
import io.swagger.jaxrs.config.{BeanConfig, DefaultJaxrsScanner, SwaggerConfigLocator, SwaggerContextService}
import io.swagger.jaxrs.listing.ApiListingResource

import scala.util.{Failure, Success, Try}

class AlgorithmServiceApplication extends Application[ApxConfiguration] with ApixioLoggable {
  // Logger
  setupLog(getClass.getCanonicalName)

  var bootstrap: Bootstrap[ApxConfiguration] = _
  var guiceBundle: GuiceBundle[ApxConfiguration] = _
  var serviceModule: ServiceModule = _

  override def initialize(bootstrap: Bootstrap[ApxConfiguration]) {
    ApxServices.setupObjectMapper(bootstrap.getObjectMapper)
    this.bootstrap = bootstrap
    serviceModule = new ServiceModule

    // Create the bundle for dropwizard-guice integration
    info("Creating DropWizard bundle")
    guiceBundle = GuiceBundle.newBuilder[ApxConfiguration]
      // ServiceModule will instantiate the dependencies and cache them
      .addModule(serviceModule)
      .setConfigClass(classOf[ApxConfiguration])
      .build()

    bootstrap.addBundle(guiceBundle)
    info("DropWizard bundle created")

    //bootstrap.addBundle( new AssetsBundle("/assets/swaggerui", "/doc", "index.html", "swagger-ui"))
  }

  override def run(cfg: ApxConfiguration, env: Environment) {
    try {
      info("Setting up service...")
      ApxServices.init(cfg)
      ApxServices.setupDefaultModels
      info("ApxServices service setting finished.")

      //Initialize DaoServices from Scala Commons
      info("Setting up DaoService...")
      DaoService.withDaoServices(ApxServices.daoServices)
      val daoservice: DaoService = new Object() with DaoService
      info("DaoService setting finished.")

      ApxServices.setupApxLogging()
      setupLog(getClass.getCanonicalName)
      // Instantiate all the dependencies using this init and use @Provides @Named dynamically so no need to bind them
      info("Instantiate serviceModule start...")
      serviceModule.init(cfg, env, daoservice.daoServices)
      // Start all daemon services
      //initSwagger(cfg, env)

      guiceBundle.getInjector.getInstance[DaemonManager](
        Key.get(classOf[DaemonManager], Names.named(ServiceNames.daemonManager))
      ).start
      info("All services started.")

      sys.ShutdownHookThread {
        info("Gracefully stopping AlgoService")
        println("Gracefully stopping AlgoService")

        val patientAssemblyLockUtil = new PatientAssemblyLockUtil(daoservice.daoServices)
        /*AssemblyMergeConsumer.activeLockRegistry.asScala.foreach(l => {
          val lockMetaInfo: Array[String] = l.split(";;;")

          if(lockMetaInfo.length == 3) {
            //
            // Format of lockMetaInfo: lockKey + ";;;" + lockCategory + ";;;" + lock
            //
            Try {
              info(s"unlocking $l")
              println(s"unlocking $l")

              patientAssemblyLockUtil.unlock(lockMetaInfo(1), lockMetaInfo(0), lockMetaInfo(2))
            } match {
              case Failure(ex) => {
                error(s"Error: patientAssemblyLockUtil::unlocking [$l]")
                println(s"Error: patientAssemblyLockUtil::unlocking [$l]")
              }
              case Success(s) =>
            }
          } else {
            error(s"Error: Invalid lockMetaInfo [$l]")
            println(s"Error: Invalid lockMetaInfo [$l]")
          }
        })*/
        info("Completed graceful shutdown of AlgoService! Bye, Have a good day!")
        println("Completed graceful shutdown of AlgoService! Bye, Have a good day!")
      }

      println("Done setting up AlgoService!")
      info("Done setting up AlgoService!")
    }
    catch {
      case e:Throwable =>
        e.printStackTrace()
        error(e.getMessage)
    }
  }
  private def initSwagger(configuration: ApxConfiguration, environment: Environment): Unit = { // Swagger Resource

    environment.jersey.register(new ApiListingResource)
    ScannerFactory.setScanner(new DefaultJaxrsScanner)

    val swaggerConfig = new BeanConfig
    swaggerConfig.setBasePath("/api")

    SwaggerConfigLocator.getInstance.putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig)
  }
}

object AlgorithmServiceApplication {
  /**
    * Main entry point.
    *
    * @param args Command line arguments.
    */
  def main(args: Array[String]) = {
    val app = new AlgorithmServiceApplication()
    app.run(args: _*)
  }
}

