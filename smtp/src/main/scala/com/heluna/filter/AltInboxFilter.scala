package com.heluna.filter

import com.typesafe.scalalogging.slf4j.Logging
import com.heluna.model.{Reject, NewTo, Continue}
import com.heluna.util.{AltInboxUtil, MailDropConfig}

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/2/13
 * Time: 5:06 PM
 */

object AltInboxFilter extends Object with Logging {

	val prefix = MailDropConfig("maildrop.data.alt-inbox-prefix").getOrElse("D-").toLowerCase

	def check(to: String): Product = {
		to.startsWith(prefix) match {
			case true => {
				// Get original e-mail address
				val original = AltInboxUtil.getRegularInbox(to)
				// Make sure original e-mail address isn't longer than max length
				original.length > MailDropConfig.getInt("maildrop.recipient.max-addr-length").getOrElse(25) match {
					case true => Reject(MailDropConfig("maildrop.recipient.invalid-response").getOrElse("Bad recipient."))
					case false => NewTo(original)
				}
			}
			case false => Continue()
		}
	}

}
