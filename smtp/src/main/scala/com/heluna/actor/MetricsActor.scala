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
import com.heluna.actor.MetricsActor._

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 1:37 PM
 */

class MetricsActor extends Actor with Redis with Logging {

  def receive = {
    case Connection => resendOnException(Connection) {
      redis.incr(metricsKey + "/connection")
    }
    case Message => resendOnException(Message) {
      redis.incr(metricsKey + "/message")
    }
    case Blocked => resendOnException(Blocked) {
      val b = redis.incr("blocked").getOrElse(0L)
      redis.incr(metricsKey + "/blocked")
      redis.publish(BlockedUtil.CHANNEL, b.toString)
    }
    case msg => logger error "Got unknown message in MetricsActor: " + msg.toString + " " + self.path.name + " at " + new Date().getTime
  }

  private def resendOnException(event: Event)(block: => Unit) =
    try {
      block
    } catch {
      case e: Exception =>
        // Redis has gone away, just reschedule the message
        context.system.scheduler.scheduleOnce(1 minute, self, event)
    }

  private val formatter = new SimpleDateFormat("yyyy/MM/dd")
  private def metricsKey = formatter.format(new Date())
}

object MetricsActor {
  sealed trait Event
  case object Connection extends Event
  case object Message extends Event
  case object Blocked extends Event
}
