package com.apixio.algoservice.manager

import com.apixio.algoservice.consumer.DocumentAddressMessageConsumer


class DaemonManager {
  var documentAddressMessageConsumer: DocumentAddressMessageConsumer = _

  def start = {
    documentAddressMessageConsumer.consume()
  }
}