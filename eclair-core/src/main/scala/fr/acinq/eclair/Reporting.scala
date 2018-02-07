package fr.acinq.eclair

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import com.izettle.metrics.influxdb.{InfluxDbHttpSender, InfluxDbReporter}
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import nl.grons.metrics4.scala.DefaultInstrumented
import scala.concurrent.duration._
import com.sun.management.OperatingSystemMXBean

object Reporting extends DefaultInstrumented with Logging {
  
  private lazy val metricsRegistry = (new DefaultInstrumented{}).metricRegistry
  
  private lazy val metricsJvmMemory = metrics.histogram("jvm.UsedPhysicalMemory")
  private lazy val metricsCPULoad = metrics.histogram("jvm.CpuLoad")
  
  private lazy val osBean = ManagementFactory.getPlatformMXBean(classOf[OperatingSystemMXBean])
  
  def start(conf: Config, actorSystem: ActorSystem) = {
    implicit val executor = actorSystem.dispatcher
    
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
      
      actorSystem.scheduler.schedule(3 seconds, 3 seconds) {
        val usedMemory = osBean.getFreePhysicalMemorySize / osBean.getTotalPhysicalMemorySize * 100
        logger.info(s"reporting used memory: $usedMemory")
        metricsJvmMemory += osBean.getFreePhysicalMemorySize / osBean.getTotalPhysicalMemorySize * 100
        metricsCPULoad += osBean.getProcessCpuLoad.toLong * 100L
      }
      
      
      val influxDbReporter = InfluxDbReporter.forRegistry(metricRegistry).build(influxDbSender)
      influxDbReporter.start(3, TimeUnit.SECONDS)

    }
    
  }
  
}
