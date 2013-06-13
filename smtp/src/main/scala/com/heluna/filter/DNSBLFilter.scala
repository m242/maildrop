package com.heluna.filter

import com.typesafe.scalalogging.slf4j.Logging
import java.net.InetAddress
import com.heluna.util.MailDropConfig
import org.xbill.DNS._
import scala.Some
import com.heluna.model.{Reject, Continue}
import scala.util.control.Breaks._

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 4:09 PM
 */

object DNSBLFilter extends Logging {

	def check(inet: InetAddress): Product = {
		val rev = reverse(inet)
		// Run query against DNSBLFilter list
		val dnsbls = MailDropConfig.getStringList("maildrop.ip.dnsbl").getOrElse(List("zen.spamhaus.org"))
		var res: Option[String] = None
		breakable {
			for(dnsbl <- dnsbls) {
				val addr = rev + "." + dnsbl
				query(addr) match {
					case Some(a) => {
						res = Some(reason(dnsbl).getOrElse("Invalid IP."))
						break()
					}
					case _ => {}
				}
			}
		}
		res match {
			case Some(r) => Reject("Invalid sender - " + r)
			case None => Continue()
		}
	}


	// Query the DNSBLFilter with a reversed hostname
	def query(hostname: String): Option[InetAddress] = {
		logger debug "Checking " + hostname + " DNSBLFilter"
		try {
			Some(Address getByName hostname)
		} catch {
			case e: Exception => None
		}
	}


	// Get the reason for a DNSBLFilter match
	def reason(hostname: String): Option[String] = {
		try {
			val lookup = new Lookup(hostname, Type.TXT)
			val builder = new StringBuilder()
			val recs: Array[Record] = Option(lookup.run()).getOrElse(Array[Record]())
			recs.foreach(rec => {
				rec.asInstanceOf[TXTRecord].getStrings.toArray.foreach(str => {
					builder.append(str.asInstanceOf[String] + " ")
				})
			})
			if (builder.length == 0) {
				None
			} else {
				Some(builder.toString())
			}
		} catch {
			case e: Exception => None
		}
	}


	// Reverse an IP address
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
}
