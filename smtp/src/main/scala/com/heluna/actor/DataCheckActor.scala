package com.heluna.actor

import akka.actor.Actor
import javax.mail.internet.MimeMessage
import com.heluna.util.MailDropConfig
import com.heluna.model.{Continue, Reject}
import java.util.Date
import com.typesafe.scalalogging.slf4j.Logging
import com.heluna.filter.SubjectFloodFilter

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 11:32 AM
 */

class DataCheckActor extends Actor with Logging {

	def receive = {

		case message: MimeMessage => {

			// Maximum message size check
			message.getSize > MailDropConfig.getBytes("maildrop.data.max-message-size").getOrElse(100000L).toInt match {
				case true => sender ! Reject(MailDropConfig("maildrop.data.invalid-response").getOrElse("Invalid message."))
				case false => {
					// Flood control check
					SubjectFloodFilter.check(message.getSubject) match {
						case Reject(reason) => sender ! Reject(reason)
						case _ => sender ! Continue()
					}
				}
			}
		}

		case msg => logger error "Got unknown message in DataCheckActor: " + msg.toString + " " + self.path.name + " at " + new Date().getTime

	}

}
