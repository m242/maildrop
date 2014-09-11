package com.heluna.maildrop.smtp.filters

import com.heluna.maildrop.smtp.{Continue, NewTo, Reject}
import com.heluna.maildrop.util.{AltInbox, MailDropConfig}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * smtp com.heluna.maildrop.smtp.filters
 * User: markbe
 * Date: 9/10/14
 * Time: 3:45 PM
 */

object AltInboxFilter {

	val prefix = MailDropConfig("maildrop.data.alt-inbox-prefix").getOrElse("D-").toLowerCase
	val maxRecipientLength = MailDropConfig.getInt("maildrop.recipient.max-addr-length").getOrElse(25)
	val reason = MailDropConfig("maildrop.recipient.response").getOrElse("Invalid recipient.")

	def apply(recipient: String): Future[Product] = Future {
		recipient.startsWith(prefix) match {
			case true =>
				// Get original e-mail address
				val original = AltInbox.getRegularInbox(recipient)
				// Make sure original e-mail address isn't longer than max length
				original.length > maxRecipientLength match {
					case true => Reject(reason)
					case false => NewTo(original)
				}
			case false => Continue()
		}
	}

}
