package com.heluna.maildrop.util

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * smtp com.heluna.maildrop.util
 * User: markbe
 * Date: 9/10/14
 * Time: 11:26 AM
 */

object Metrics {

	val BLOCKEDCHANNEL = "blocked-channel"
	val sdf = new SimpleDateFormat("yyyy/MM/dd")
	def key = sdf.format(new Date())

	def connection(): Unit = {
		Redis.client.incr(key + "/connection")
	}

	def message(): Unit = {
		Redis.client.incr(key + "/message")
	}

	def blocked(): Unit = {
		Redis.client.incr(key + "/blocked")
		Redis.client.incr("blocked") onSuccess {
			case blocked => Redis.client.publish(BLOCKEDCHANNEL, blocked.toString)
		}
	}

	def getBlocked = Redis.client.get("blocked").map(_.map(_.utf8String.toLong).getOrElse(0L))

}
