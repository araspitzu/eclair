package fr.acinq.eclair

import java.util.concurrent.TimeUnit

import com.codahale.metrics.{ConsoleReporter, Slf4jReporter}
import grizzled.slf4j.Logging
import nl.grons.metrics4.scala.DefaultInstrumented

object Reporting extends DefaultInstrumented with Logging {
  
    val metricsRegistry = (new DefaultInstrumented{}).metricRegistry
    
    val consoleReporter = ConsoleReporter.forRegistry(metricsRegistry).build()
  
    def start() = {
      logger.info("Starting metrics reporter")
      consoleReporter.start(3, TimeUnit.SECONDS)
      logger.info("...started")
    }
  
}
