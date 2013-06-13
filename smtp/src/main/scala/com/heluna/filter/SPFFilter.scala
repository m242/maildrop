package com.heluna.filter

import com.typesafe.scalalogging.slf4j.Logging
import java.net.InetAddress
import org.apache.james.jspf.impl.DefaultSPF
import com.heluna.model.{Continue, Reject}

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 5/1/13
 * Time: 7:15 PM
 */

object SPFFilter extends Logging {

	def check(inet: InetAddress, helo: String, from: String): Product = {
		try {
			logger debug "Checking " + helo + " SPFFilter"
			new DefaultSPF().checkSPF(inet.getHostAddress, from, helo).getResult match {
				case "fail" => Reject("Invalid sender - see http://www.openspf.org/Introduction")
				case _ => Continue()
			}
		} catch {
			case e: Exception => Continue()
		}
	}

}
