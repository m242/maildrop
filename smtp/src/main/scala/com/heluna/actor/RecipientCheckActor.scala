package com.heluna.actor

import akka.actor.Actor
import com.heluna.util.MailDropConfig
import com.heluna.model.{NewTo, Continue, Reject}
import java.util.Date
import com.typesafe.scalalogging.slf4j.Logging
import com.heluna.filter.AltInboxFilter

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 11:32 AM
 */

class RecipientCheckActor extends Actor with Logging {

	def receive = {

		case addr: String => {
			// Alt Inbox check
			AltInboxFilter.check(addr) match {
				case Reject(reason) => sender ! Reject(reason)
				case NewTo(a) => sender ! NewTo(a)
				case Continue() => {
					// Maximum recipient address size is maildrop.max-recipient-length
					addr.length > MailDropConfig.getInt("maildrop.recipient.max-addr-length").getOrElse(25) match {
						case true => sender ! Reject(MailDropConfig("maildrop.recipient.invalid-response").getOrElse("Bad recipient."))
						case false => sender ! Continue()
					}
				}
			}

		}

		case msg => logger error "Got unknown message in RecipientCheckActor: " + msg.toString + " " + self.path.name + " at " + new Date().getTime
	}

}
