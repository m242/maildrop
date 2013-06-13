package com.heluna.filter

import com.typesafe.scalalogging.slf4j.Logging
import com.heluna.cache.Redis
import java.net.InetAddress
import com.heluna.util.MailDropConfig
import com.heluna.model.{Continue, Reject}
import java.util.Date
import com.redis.serialization.Parse.Implicits.parseLong

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 5/1/13
 * Time: 7:45 PM
 */

object IPFloodFilter extends Object with Redis with Logging {

	def check(inet: InetAddress): Product = {
		try {
			(isBanned(inet) || isFlooding(inet)) match {
				case true => {
					// Ban this IP address, or extend an existing ban.
					ban(inet)
					Reject(MailDropConfig("maildrop.ip.ban-response").getOrElse("Banned."))
				}
				case false => Continue()
			}
		} catch {
			// Redis is probably down here - just continue.
			case e: Exception => {
				logger error "Exception in IPFloodFilter: " + e.getMessage
				Continue()
			}
		}
	}

	def ipKey(inet: InetAddress): String = "ip:" + inet.getHostAddress
	def banKey(inet: InetAddress): String = "banip:" + inet.getHostAddress

	def isBanned(inet: InetAddress): Boolean = redis.get[String](banKey(inet)).isDefined

	def isFlooding(inet: InetAddress): Boolean = {
		val now = new Date().getTime
		val maxMessages = MailDropConfig.getInt("maildrop.ip.flood-messages").getOrElse(20)
		val key = ipKey(inet)
		redis.lpush(key, now)
		redis.ltrim(key, 0, maxMessages)
		redis.expire(key, MailDropConfig.getSeconds("maildrop.ip.flood-time").getOrElse(120))
		(redis.llen(key).getOrElse(0L).toInt > maxMessages) &&
			((now - redis.rpop[Long](key).getOrElse(0L)) < MailDropConfig.getMilliseconds("maildrop.ip.flood-time").getOrElse(120000L))
	}

	def ban(inet: InetAddress) {
		val key = banKey(inet)
		redis.set(key, "y")
		redis.expire(key, MailDropConfig.getSeconds("maildrop.ip.ban-time").getOrElse(300))
	}

}
