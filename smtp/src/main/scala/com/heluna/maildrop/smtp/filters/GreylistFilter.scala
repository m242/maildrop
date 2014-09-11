package com.heluna.maildrop.smtp.filters

import java.net.InetAddress

import akka.actor.ActorSystem
import com.heluna.maildrop.smtp.{Greylist, Continue}
import com.heluna.maildrop.util.{MailDropConfig, Redis}

import scala.concurrent.{duration, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * smtp com.heluna.maildrop.smtp.filters
 * User: markbe
 * Date: 9/10/14
 * Time: 1:17 PM
 */

object GreylistFilter {

	val reason = MailDropConfig("maildrop.sender.greylist.response").getOrElse("Greylisted")
	val ttl = MailDropConfig.getSeconds("maildrop.sender.greylist.time").getOrElse(300L).toInt
	val system = ActorSystem()

	def key(inet: InetAddress, helo: String) = "greylist:" + inet.getHostAddress + "/" + helo

	def apply(inet: InetAddress, helo: String): Future[Product] = {
		val ckey = key(inet, helo)
		Redis.client.get(ckey).map {
			case Some(str) => Continue()
			case None =>
				system.scheduler.scheduleOnce(new FiniteDuration(ttl, duration.SECONDS))({ GreylistFilter.add(inet, helo) })
				Greylist(reason)
		}
	}

	def add(inet: InetAddress, helo: String): Unit = {
		Redis.client.set(key(inet, helo), "ok")
	}

}
