package com.heluna.maildrop.smtp.filters

import java.net.InetAddress
import com.heluna.maildrop.smtp.{Continue, Reject, Accept}
import com.heluna.maildrop.util.{MailDropConfig, Redis}
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * smtp com.heluna.maildrop.smtp.filters
 * User: markbe
 * Date: 9/10/14
 * Time: 2:08 PM
 */

object CacheFilter extends LazyLogging {

	val ttl = MailDropConfig.getSeconds("maildrop.sender.cache-time").getOrElse(300L)

	def key(inet: InetAddress, helo: String) = "cache:" + inet.getHostAddress + "/" + helo

	def apply(inet: InetAddress, helo: String): Future[Product] = {
		val ckey = key(inet, helo)
		Redis.client.hgetall[String](ckey).map(m => m.get("action") match {
			case Some(action) if action == "accept" => Accept()
			case Some(action) if action == "reject" => Reject(m.getOrElse("reason", "Reject."))
			case _ => Continue()
		})
	}

	def add(inet: InetAddress, helo: String, action: Product): Unit = {
		val ckey = key(inet, helo)
		action match {
			case Accept() => Redis.client.hmset(ckey, Map("action" -> "accept"))
			case Reject(reason) => Redis.client.hmset(ckey, Map("action" -> "reject", "reason" -> reason))
			case _ =>
		}
		Redis.client.expire(ckey, ttl)
	}

}
