package com.apixio.messaging.messages

object DeserTests extends App {

    val jsonStrings: Array[String] = Array[String]("{\"patientUUID\":\"3f0d70db-2dd0-460a-a354-0e7902520cd9\",\"persistedCategory\":\"clinicalActor,documentMeta,demographic\"}", "{\"patientUUID\":\"3f0d70db-2dd0-460a-a354-0e7902520cd9\",\"persistedCategory\":\"all\", \"pdsId\": \"372\"}")
    print(jsonStrings.indexOf(0))

}
