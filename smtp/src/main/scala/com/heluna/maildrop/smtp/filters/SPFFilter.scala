package com.heluna.maildrop.smtp.filters

import java.net.InetAddress
import com.heluna.maildrop.smtp.{Reject, Continue}
import com.heluna.maildrop.util.MailDropConfig
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.james.jspf.core.Logger
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

object SPFFilter extends LazyLogging {

	val reason = MailDropConfig("maildrop.sender.spf.response").getOrElse("Greylisted")

	def apply(inet: InetAddress, helo: String, sender: String): Future[Product] = Future {
		logger.debug("Checking SPF for " + inet.getHostAddress + "/" + helo)
		Try(new DefaultSPF(new SPFLogger()).checkSPF(inet.getHostAddress, sender, helo).getResult.toLowerCase match {
			case "fail" =>
				CacheFilter.add(inet, helo, Reject(reason))
				Reject(reason)
			case _ => Continue()
		}).getOrElse(Continue())
	}

}

class SPFLogger extends Logger with LazyLogging {
	override def debug(p1: String) = logger.debug(p1)

	override def warn(p1: String) = logger.warn(p1)

	override def warn(p1: String, p2: Throwable) = logger.warn(p1, p2)

	override def isErrorEnabled = logger.underlying.isErrorEnabled

	override def isInfoEnabled = logger.underlying.isInfoEnabled

	override def isDebugEnabled = logger.underlying.isDebugEnabled

	override def error(p1: String) = logger.error(p1)

	override def error(p1: String, p2: Throwable) = logger.error(p1, p2)

	override def getChildLogger(p1: String) = this

	override def debug(p1: String, p2: Throwable) = logger.debug(p1, p2)

	override def fatalError(p1: String) = logger.error(p1)

	override def fatalError(p1: String, p2: Throwable) = logger.error(p1, p2)

	override def isWarnEnabled = logger.underlying.isWarnEnabled

	override def isFatalErrorEnabled = logger.underlying.isErrorEnabled

	override def info(p1: String) = logger.info(p1)

	override def info(p1: String, p2: Throwable) = logger.info(p1, p2)
}
