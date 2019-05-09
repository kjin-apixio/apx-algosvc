import sbt.ExclusionRule


val appPlatformVersion = "0.2.0"
val appBizlogic = "0.2.5"

libraryDependencies ++= Seq(
  "org.scalatest"     %% "scalatest"  % "3.0.5" % Test,
  "org.scalacheck"    %% "scalacheck" % "1.12.6" % Test,
  "org.pegdown" % "pegdown" % "1.6.0" % Test,
  "org.mockito" % "mockito-all"     % "1.10.19" % "test",
  "com.hubspot.dropwizard" % "dropwizard-guice" % "1.0.6.0",
  "com.google.inject" % "guice" % "4.0",
  "org.json" % "json" % "20180130",
  "io.swagger" % "swagger-jaxrs" % "1.5.17"
    exclude("commons-logging", "commons-logging")
    exclude("javax.ws.rs", "jsr311-api")
    excludeAll(
      ExclusionRule(organization = "com.fasterxml.jackson.core"),
      ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
      ExclusionRule(organization = "com.fasterxml.jackson.module"),
      ExclusionRule(organization = "com.fasterxml.jackson.jaxrs")
  ),
  "io.swagger" %% "swagger-scala-module" % "1.0.4"
    excludeAll(
      ExclusionRule(organization = "com.fasterxml.jackson.core"),
      ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
      ExclusionRule(organization = "com.fasterxml.jackson.module"),
      ExclusionRule(organization = "com.fasterxml.jackson.jaxrs")
  ),

   "apixio" % "apixio-signalmanager-bizlogic" % "1.0.5"
    excludeAll (
      ExclusionRule(organization = "com.fasterxml.jackson.core"),
      ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
      ExclusionRule(organization = "com.fasterxml.jackson.module"),
      ExclusionRule(organization = "com.fasterxml.jackson.jaxrs"),
      ExclusionRule(organization = "org.hamcrest"),
      ExclusionRule("org.slf4j", "slf4j-api"),
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("org.mockito", "mockito-core"),
      ExclusionRule("org.mockito", "mockito-all"),
      ExclusionRule("javax.ws.rs", "jsr311-api"),
      ExclusionRule(organization = "com.sun.jersey"),
      ExclusionRule("com.sun.jersey", "jersey-client"),
      ExclusionRule("com.sun.jersey", "jersey-server"),
      ExclusionRule("apixio", "apixio-bizlogic"),
      ExclusionRule("apixio", "apixio-dao"),
      ExclusionRule("apixio", "apixio-datasource"),
      ExclusionRule(organization = "io.netty"),
      ExclusionRule(organization = "com.codahale.metrics"),
      ExclusionRule(organization = "io.dropwizard.metrics"),
      ExclusionRule(organization = "commons-beanutils"),
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.hamcrest"),
      ExclusionRule(organization = "junit"),
      ExclusionRule(organization = "org.springframework")
  ),
  "apixio" %% "application-dropwizard-base" % "0.5.18"
    exclude("javax.inject","javax.inject")
    exclude("commons-io", "commons-io")
    exclude("commons-logging", "commons-logging")
    exclude("com.codahale.metrics", "metrics-core")
    exclude("net.minidev", "asm")
    exclude("org.springframework.security", "spring-security-core")
    exclude("org.springframework", "spring-aop")
    exclude("org.springframework", "spring-web")
    exclude("org.springframework", "spring-context")
    exclude("org.springframework", "spring-jdbc")
    exclude("org.springframework", "spring-tx")
    exclude("org.springframework", "spring-beans")
    // from scala-common?
    exclude("apixio", "apixio-model-converter")
    exclude("junit", "junit")
    exclude("org.hamcrest", "hamcrest-core")
    exclude("commons-beanutils", "commons-beanutils-core")
    exclude("com.codahale.metrics", "metrics-annotation")
    exclude("net.jpountz.lz4", "lz4"),
  "apixio" %% "app-platform" % appPlatformVersion
    excludeAll(
    ExclusionRule("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("log4j", "log4j"),
  ),
  "apixio" %% "app-platform-bizlogic" % appBizlogic
    excludeAll(
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("log4j", "log4j"),
    ),
  "apixio" % "apixio-model"  % "1.23.0",
  "apixio" % "apixio-signalmanager-event-handler"  % "1.2.8",
  "apixio" %% "scala-common" % "2.8.0"
    excludeAll(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
    ExclusionRule(organization = "com.fasterxml.jackson.module"),
    ExclusionRule(organization = "com.fasterxml.jackson.jaxrs"),
    ExclusionRule(organization = "org.hamcrest"),
    ExclusionRule(organization = "javax.ws.rs"),
    ExclusionRule("javax.ws.rs", "jsr311-api"),
    ExclusionRule(organization = "com.sun.jersey"),
    ExclusionRule("com.sun.jersey","jersey-client"),
    ExclusionRule("com.sun.jersey","jersey-server"),
    ExclusionRule("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("log4j", "log4j"),
    ExclusionRule("org.slf4j", "slf4j-log4j"),
    ExclusionRule("org.slf4j", "slf4j-api"),
    ExclusionRule("org.apache.logging.log4j","log4j-api"),
    ExclusionRule("org.slf4j", "log4j-over-slf4j"),
    ExclusionRule("log4j","apache-log4j-extras")
  ),
  "org.mockito" % "mockito-all" % "1.8.4",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.4",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.9.4",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.9.4",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.4",
  "com.fasterxml.jackson.module" % "jackson-module-afterburner" % "2.9.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.9.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.9.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.9.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.4",
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % "2.9.4",
  "log4j" % "log4j" % "1.2.17" force(),
  "org.slf4j" % "slf4j-log4j12" % "1.7.25" force(),
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.13",
  "apixio" % "apixio-mcs-client-cli" % "0.2.4-SNAPSHOT"
    excludeAll(
      ExclusionRule("apixio", "model")
  ),
  "apixio" % "apixio-mcs-client-common" % "0.2.4-SNAPSHOT",
  "apixio" % "apixio-mcs-client-marshal" % "0.2.4-SNAPSHOT",
  "apixio" % "apixio-bizlogic" % "4.2.11"
    excludeAll(
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("log4j", "log4j"),
      ExclusionRule("org.apache.logging.log4j","log4j-api")
    ),
  "apixio" % "apixio-dao" % "4.2.11"
    excludeAll(
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("log4j", "log4j"),
      ExclusionRule("org.apache.logging.log4j","log4j-api")
    ),
  "apixio" % "apixio-messaging" % "1.0.16"
    excludeAll(
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("log4j", "log4j"),
      ExclusionRule("org.apache.logging.log4j","log4j-api"),
      ExclusionRule("com.typesafe.akka", "akka-stream-kafka")

    ),
  "org.springframework" % "spring-beans" % "5.0.6.RELEASE",
  "org.springframework" % "spring-core" % "5.0.6.RELEASE",
  "org.apache.logging.log4j" % "log4j-core" % "2.11.0" force(),
  "log4j" % "apache-log4j-extras" % "1.2.17" force(),
  "io.gatling" %% "jsonpath" % "0.6.10"
)


excludeDependencies ++= Seq(
  ExclusionRule("javax.ws.rs", "jsr311-api"),
  ExclusionRule("org.hamcrest", "hamcrest-core"),
//  ExclusionRule("org.springframework", "spring-core"),
  ExclusionRule("org.springframework", "spring-aop"),
//  ExclusionRule("org.springframework", "spring-beans"),
  ExclusionRule("org.slf4j", "slf4j-log4j13"),
  ExclusionRule("commons-beanutils", "commons-beanutils-core"),
  ExclusionRule("com.codahale.metrics", "metrics-annotation"),
  ExclusionRule("com.codahale.metrics", "metrics-core"),
  ExclusionRule("net.jpountz.lz4", "lz4"),
  ExclusionRule("org.springframework", "spring-web"),
  ExclusionRule("org.springframework", "spring-context-support"),
  ExclusionRule("net.minidev", "asm"),
  ExclusionRule("xml-apis", "xml-apis"),
  ExclusionRule("javax.servlet.jsp", "jsp-api"),
  ExclusionRule("javax.ws.rs", "jsr311-api"),
  ExclusionRule("com.sun.jersey", "jersey-core"),
  ExclusionRule("com.sun.jersey","jersey-server"),
  ExclusionRule("org.apache.logging.log4j","log4j-api"),
  ExclusionRule("org.slf4j", "slf4j-log4j12"),
  ExclusionRule("org.slf4j", "log4j-over-slf4j"),
//  ExclusionRule("log4j","apache-log4j-extras"),
//  ExclusionRule("org.slf4j", "slf4j-api"),
//  ExclusionRule("log4j", "log4j"),
  ExclusionRule("com.sun.jersey","jersey-client"),
  ExclusionRule(organization = "com.sun.jersey"),
  ExclusionRule(organization = "jline"),
  ExclusionRule("javax.el", "javax.el-api"),
  ExclusionRule("org.glassfish.web", "javax.el"),
  ExclusionRule("org.apache.hadoop", "hadoop-common"),
  ExclusionRule("org.apache.avro", "avro"),
  ExclusionRule("org.apache.hadoop", "hadoop-auth"),
  ExclusionRule("com.amazonaws", "aws-java-sdk-core"),
  ExclusionRule("org.mockito", "mockito-all"),
  ExclusionRule("org.ow2.asm", "asm"),
  ExclusionRule("com.esotericsoftware.kryo", "kryo"),
  ExclusionRule("com.amazonaws", "aws-java-sdk"),
  ExclusionRule("org.apache.hadoop", "hadoop-yarn-api"),
  ExclusionRule("io.get-coursier", "coursier_2.11"),
  ExclusionRule("org.apache.htrace", "htrace-core4"),
  ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
  ExclusionRule("org.apache.hadoop", "hadoop-aws"),
  ExclusionRule("org.apache.hadoop", "hadoop-hdfs"),

  ExclusionRule("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.2_spec")
  //ExclusionRule("apixio", "apixio-ensemble-extractors_2.11"),
  //ExclusionRule("apixio", "apixio-ensemble-generators_2.11"),
  //ExclusionRule("apixio", "apixio-ensemble-common-impl_2.11"),
  //ExclusionRule("apixio", "apixio-ensemble-combiners_2.11"),
  //ExclusionRule("apixio", "apixio-ensemble-interface_2.11")

)

dependencyOverrides ++= Seq(
  "org.slf4j" % "slf4j-log4j12" % "1.7.25",
  "log4j" % "log4j" % "1.2.17",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "apixio" % "apixio-messaging" % "1.0.16",
  "log4j" % "apache-log4j-extras" % "1.2.17",
  "org.apache.kafka" % "kafka-clients" % "1.1.1",
  "com.typesafe.akka" %% "akka-stream-kafka_2.11" % "1.0"
)

