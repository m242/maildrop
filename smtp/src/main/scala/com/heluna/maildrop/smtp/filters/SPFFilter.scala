package com.heluna.maildrop.smtp.filters

import java.net.InetAddress
import com.heluna.maildrop.smtp.{Reject, Continue}
import com.heluna.maildrop.util.MailDropConfig
import org.apache.james.jspf.impl.DefaultSPF
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
 * smtp com.heluna.maildrop.smtp.filters
 * User: markbe
 * Date: 9/10/14
 * Time: 2:37 PM
 */

object SPFFilter {

	val reason = MailDropConfig("maildrop.sender.spf.response").getOrElse("Greylisted")

	def apply(inet: InetAddress, helo: String, sender: String): Future[Product] = Future {
		Try(new DefaultSPF().checkSPF(inet.getHostAddress, sender, helo).getResult.toLowerCase match {
			case "fail" =>
				CacheFilter.add(inet, helo, Reject(reason))
				Reject(reason)
			case _ => Continue()
		}).getOrElse(Continue())
	}

}
