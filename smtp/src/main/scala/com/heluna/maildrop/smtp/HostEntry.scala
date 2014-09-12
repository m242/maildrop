package com.heluna.maildrop.smtp

import java.net.InetAddress
import com.heluna.maildrop.util.{MailDropConfig, Redis}
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * smtp com.heluna.maildrop.smtp
 * User: markbe
 * Date: 9/12/14
 * Time: 8:48 AM
 */

object HostEntry extends LazyLogging {

	val ttl = MailDropConfig.getSeconds("maildrop.sender.cache-time").getOrElse(86400L)

	def key(inet: InetAddress, helo: String) = "host/" + inet.getHostAddress + "/" + helo

	def apply(inet: InetAddress, helo: String) = {
		val hkey = key(inet, helo)
		logger.debug("loading " + hkey)
		Redis.client.hgetall[String](hkey)
	}

	def touch(inet: InetAddress, helo: String) = {
		Redis.client.expire(key(inet, helo), ttl)
	}

}
