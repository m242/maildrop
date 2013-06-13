package com.heluna.actor

import akka.actor.Actor
import com.typesafe.scalalogging.slf4j.Logging
import java.util.Date
import com.heluna.cache.Redis
import java.text.SimpleDateFormat
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import com.heluna.util.BlockedUtil

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 1:37 PM
 */

class MetricsActor extends Actor with Redis with Logging {

	def receive = {

		case "connection" => {
			try {
				redis.incr(metricsKey + "/connection")
			} catch {
				case e: Exception => {
					// Redis has gone away, just reschedule the message
					context.system.scheduler.scheduleOnce(1 minute, self, "connection")
				}
			}
		}

		case "message" => {
			try {
				redis.incr(metricsKey + "/message")
			} catch {
				case e: Exception => {
					// Redis has gone away, just reschedule the message
					context.system.scheduler.scheduleOnce(1 minute, self, "message")
				}
			}
		}

		case "blocked" => {
			try {
				val b = redis.incr("blocked").getOrElse(0L)
				redis.incr(metricsKey + "/blocked")
				redis.publish(BlockedUtil.CHANNEL, b.toString)
			} catch {
				case e: Exception =>
					// Redis has gone away, just reschedule the message
					context.system.scheduler.scheduleOnce(1 minute, self, "blocked")
			}
		}

		case msg => logger error "Got unknown message in MetricsActor: " + msg.toString + " " + self.path.name + " at " + new Date().getTime
	}

	def metricsKey = new SimpleDateFormat("yyyy/MM/dd").format(new Date())

}
