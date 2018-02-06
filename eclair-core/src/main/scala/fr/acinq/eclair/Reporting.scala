package fr.acinq.eclair

import java.util.concurrent.TimeUnit
import com.izettle.metrics.influxdb.{InfluxDbHttpSender, InfluxDbReporter}
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import nl.grons.metrics4.scala.DefaultInstrumented

object Reporting extends DefaultInstrumented with Logging {
  
  private lazy val metricsRegistry = (new DefaultInstrumented{}).metricRegistry
  
  
  def start(conf: Config) = {
    
    val isMonitoringEnabled = conf.getBoolean("monitoring.enabled")
    logger.info(s"Monitoring enabled:${isMonitoringEnabled}")
    
    lazy val influxDbSender = new InfluxDbHttpSender(
      conf.getString("monitoring.influxDb.protocol"),
      conf.getString("monitoring.influxDb.host"),
      conf.getInt("monitoring.influxDb.port"),
      conf.getString("monitoring.influxDb.database"),
      conf.getString("monitoring.influxDb.authstring"),
      TimeUnit.MILLISECONDS,
      1000,
      1000,
      ""
    )

    if(isMonitoringEnabled){
      logger.info("Starting metrics reporter")
      val influxDbReporter = InfluxDbReporter.forRegistry(metricRegistry).build(influxDbSender)
      influxDbReporter.start(3, TimeUnit.SECONDS)

    }
    
  }
  
}
