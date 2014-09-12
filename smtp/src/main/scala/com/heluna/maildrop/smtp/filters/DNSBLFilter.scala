package com.heluna.maildrop.smtp.filters

import java.net.InetAddress
import com.heluna.maildrop.smtp.{Reject, Continue}
import com.heluna.maildrop.util.MailDropConfig
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.xbill.DNS.{TXTRecord, Type, Lookup, Address}
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * smtp com.heluna.maildrop.smtp.filters
 * User: markbe
 * Date: 9/10/14
 * Time: 2:30 PM
 */

object DNSBLFilter extends LazyLogging {
	import scala.collection.JavaConversions._

	val dnsbls = MailDropConfig.getStringList("maildrop.sender.dnsbl")

	def apply(inet: InetAddress, helo: String): Future[Product] = {
		logger.debug("Checking DNSBL for " + inet.getHostAddress + "/" + helo)
		val revip = reverse(inet)
		val futures = dnsbls.map(dnsbl => {
			val hostname = revip + "." + dnsbl
			logger.debug("Checking DNSBL " + hostname)
			query(hostname).map {
				case Some(addr) =>
					logger.debug("DNSBL match " + hostname)
					Some(reason(hostname).getOrElse("Invalid IP: " + hostname))
				case None => None
			}
		})
		val results = Future.sequence(futures)
		results.map(_.find(_.isDefined).flatten match {
			case Some(reason) =>
				logger.debug("Rejecting " + inet.getHostAddress + "/" + helo + ": " + reason)
				Reject(reason)
			case _ => Continue()
		})
	}

	def reverse(inet: InetAddress): String = {
		val buffer = new StringBuilder()
		for (octet <- inet.getAddress) {
			val oInt = octet & 0xFF
			if (buffer.length != 0) {
				buffer.insert(0, '.')
			}
			buffer.insert(0, oInt)
		}
		buffer.toString()
	}

	def query(hostname: String): Future[Option[InetAddress]] = Future { Try(Address.getByName(hostname)).toOption }

	def reason(hostname: String): Option[String] = {
		Try(new Lookup(hostname, Type.TXT).run().map(_.asInstanceOf[TXTRecord].getStrings.mkString(" ")).mkString(" ")).toOption match {
			case Some(str) if str.length == 0 => None
			case Some(str) => Some(str)
			case None => None
		}
	}

}
