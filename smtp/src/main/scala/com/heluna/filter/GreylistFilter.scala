package com.heluna.filter

import com.typesafe.scalalogging.slf4j.Logging
import com.heluna.cache.Redis
import java.net.InetAddress
import com.heluna.model.{Greylist, Continue}
import com.heluna.util.MailDropConfig

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/10/13
 * Time: 10:51 AM
 */

object GreylistFilter extends Object with Redis with Logging {

	def check(inet: InetAddress, helo: String): Product = {
		try {
			inGreyList(inet, helo) match {
				case true => Continue()
				case false => Greylist(MailDropConfig("maildrop.ip.greylist.response").getOrElse("Greylisted."))
			}
		} catch {
			// Redis is probably down here - just continue.
			case e: Exception => {
				logger error "Exception in GreylistFilter: " + e.getMessage
				Continue()
			}
		}
	}

	def greylistKey(inet: InetAddress, helo: String) = "greylist:" + helo + "/" + inet.getAddress.slice(0, 3).mkString(".")

	def inGreyList(inet: InetAddress, helo: String): Boolean = {
		logger debug "Checking " + helo + " GreylistFilter"
		logger debug "Key: " + greylistKey(inet, helo) + " Redis: " + redis.get(greylistKey(inet, helo)).getOrElse("?")
		redis.get(greylistKey(inet, helo)).getOrElse("?") match {
			case "y" => {
				true
			}
			case _ => false
		}
	}

	def addToGreyList(inet: InetAddress, helo: String) {
		logger debug "Adding " + helo + " to GreylistFilter"
		redis.set(greylistKey(inet, helo), "y")
	}

}
