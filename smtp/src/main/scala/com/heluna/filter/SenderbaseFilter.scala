package com.heluna.filter

import com.typesafe.scalalogging.slf4j.Logging
import java.net.InetAddress
import com.heluna.util.MailDropConfig
import org.xbill.DNS._
import scala.Some
import com.heluna.model.{Reject, Continue}
import java.util.Date

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 4:09 PM
 */

object SenderbaseFilter extends Logging {

	def check(inet: InetAddress): Product = {
		val rev = DNSBLFilter.reverse(inet)
		val senderbaseUrl = rev + ".sa.senderbase.org"
		// Run query against Senderbase
		query(senderbaseUrl) match {
			case Some(result) => {
				val fields = getFields(result)
				// If any filters match, reject the message.
				if(newDomainFilter(fields) ||
				  ipVolumeFilter(fields) ||
					domainVolumeFilter(fields) ||
					likelihoodFilter(fields)
				) Reject("Invalid sender - see http://www.senderbase.org/lookup?search_string=" + inet.getHostAddress)
				else Continue()
			}
			case None => Continue()
		}
	}


	def query(hostname: String): Option[String] = {
		logger debug "Checking " + hostname + " SenderbaseFilter"
		try {
			val lookup = new Lookup(hostname, Type.TXT)
			val builder = new StringBuilder()
			val recs: Array[Record] = Option(lookup.run()).getOrElse(Array[Record]())
			recs.foreach(rec => {
				rec.asInstanceOf[TXTRecord].getStrings.toArray.foreach(str => {
					builder.append(str.asInstanceOf[String])
				})
			})
			if (builder.length == 0) None
			else Some(builder.toString())
		} catch {
			case e: Exception => None
		}
	}


	def getFields(result: String): Map[String, String] = {
		val mymap = collection.mutable.HashMap[String, String]()
		result.split("\\|").foreach(r => {
			val kv = r.split("=")
			if(kv.length > 1) mymap.put(kv(0), kv(1))
		})
		mymap.toMap
	}


	def newDomainFilter(fields: Map[String, String]): Boolean = {
		val DAYS_OLD = MailDropConfig.getInt("maildrop.ip.senderbase.new-domain-age").getOrElse(90)
		val MAGNITUDE = MailDropConfig.getDouble("maildrop.ip.senderbase.new-domain-magnitude").getOrElse(2.0)
		val nowSeconds = new Date().getTime / 1000
		nowSeconds - fields.get("25").getOrElse("0").toLong < (86400 * DAYS_OLD) &&
			fields.get("24").getOrElse("10").toDouble > MAGNITUDE
	}

	def ipVolumeFilter(fields: Map[String, String]): Boolean = {
		val MAGNITUDE = MailDropConfig.getDouble("maildrop.ip.senderbase.ip-volume-magnitude").getOrElse(2.5)
		val MAGNITUDE_SPIKE = MailDropConfig.getDouble("maildrop.ip.senderbase.ip-volume-spike").getOrElse(0.7)
		val CUTOFF = MailDropConfig.getDouble("maildrop.ip.senderbase.volume-cutoff").getOrElse(6.0)
		val daily = fields.get("40").getOrElse("0").toDouble
		val monthly = fields.get("41").getOrElse("0").toDouble
		daily - monthly >= MAGNITUDE_SPIKE && monthly >= MAGNITUDE && fields.get("3").getOrElse("0").toDouble < CUTOFF
	}

	def domainVolumeFilter(fields: Map[String, String]): Boolean = {
		val MAGNITUDE = MailDropConfig.getDouble("maildrop.ip.senderbase.domain-volume-magnitude").getOrElse(2.0)
		val MAGNITUDE_SPIKE = MailDropConfig.getDouble("maildrop.ip.senderbase.domain-volume-spike").getOrElse(0.5)
		val CUTOFF = MailDropConfig.getDouble("maildrop.ip.senderbase.volume-cutoff").getOrElse(6.0)
		val daily = fields.get("23").getOrElse("0").toDouble
		val monthly = fields.get("24").getOrElse("0").toDouble
		daily - monthly >= MAGNITUDE_SPIKE && monthly >= MAGNITUDE && fields.get("3").getOrElse("0").toDouble < CUTOFF
	}

	def likelihoodFilter(fields: Map[String, String]): Boolean = {
		val MAGNITUDE = MailDropConfig.getDouble("maildrop.ip.senderbase.likelihood-magnitude").getOrElse(1.5)
		val LIKELIHOOD = MailDropConfig.getDouble("maildrop.ip.senderbase.likelihood-score").getOrElse(0.95)
		val score = fields.get("44").getOrElse("0").toDouble
		val monthly = fields.get("41").getOrElse("0").toDouble
		val avg = fields.get("43").getOrElse("0").toDouble
		score >= LIKELIHOOD && monthly >= MAGNITUDE && avg >= MAGNITUDE
	}

}
