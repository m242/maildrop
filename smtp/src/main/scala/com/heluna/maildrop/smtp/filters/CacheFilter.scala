package com.heluna.maildrop.smtp.filters

import java.net.InetAddress
import com.heluna.maildrop.smtp.{HostEntry, Continue, Reject, Accept}
import com.heluna.maildrop.util.Redis
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * smtp com.heluna.maildrop.smtp.filters
 * User: markbe
 * Date: 9/10/14
 * Time: 2:08 PM
 */

object CacheFilter extends LazyLogging {

	def apply(host: Map[String, String]): Future[Product] = Future {
		host.get("action") match {
			case Some(action) if action == "accept" => Accept()
			case Some(action) if action == "reject" => Reject(host.getOrElse("reason", "Reject."))
			case _ => Continue()
		}
	}

	def add(inet: InetAddress, helo: String, action: Product): Unit = {
		val ckey = HostEntry.key(inet, helo)
		action match {
			case Accept() => Redis.client.hmset(ckey, Map("action" -> "accept"))
			case Reject(reason) => Redis.client.hmset(ckey, Map("action" -> "reject", "reason" -> reason))
			case _ =>
		}
	}

}
