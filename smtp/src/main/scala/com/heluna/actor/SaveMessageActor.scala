package com.heluna.actor

import akka.actor.Actor
import javax.mail.internet.MimeMessage
import com.typesafe.scalalogging.slf4j.Logging
import java.util.Date
import javax.mail.{Multipart, BodyPart}
import scala.collection.mutable.ArrayBuffer
import java.io.ByteArrayOutputStream
import com.heluna.util.{MailboxUtil, CompressUtil}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 1:37 PM
 */

class SaveMessageActor extends Actor with Logging {

	def receive = {

		case (sender: String, recipient: String, message: MimeMessage) => {
			// Remove all non-text attachments from message
			val cleanMessage = stripAttachments(message)
			// Compress message
			val compressed = compressMessage(cleanMessage)
			logger debug "Compressed length: " + compressed.length
			// Add message to mailbox list
			try {
				MailboxUtil.add(sender, recipient, message.getSubject, Option(message.getSentDate).getOrElse(new Date()), compressed)
			} catch {
				// On Redis exception, re-schedule message for a minute from now
				case e: Exception => {
					logger error "Exception saving message at " + new Date().getTime
					context.system.scheduler.scheduleOnce(1 minute, context.parent, (recipient, message))
				}
			}
		}

		case msg => logger error "Got unknown message in SaveMessageActor: " + msg.toString + " " + self.path.name + " at " + new Date().getTime
	}


	def stripAttachments(message: MimeMessage): MimeMessage = {
		val newmsg = new MimeMessage(message)
		if(newmsg.isMimeType("multipart/*")) {
			val mpart = newmsg.getContent.asInstanceOf[Multipart]
			val partList = ArrayBuffer[BodyPart]()
			for(i <- 0 to (mpart.getCount-1)) {
				val part = mpart.getBodyPart(i)
				logger debug "part " + i + ": " + part.getContentType
				// Go only one more level down if necessary - this isn't Inception
				if(part.isMimeType("multipart/*")) {
					val mmpart = part.getContent.asInstanceOf[Multipart]
					val subpartList = ArrayBuffer[BodyPart]()
					for(j <- 0 to (mmpart.getCount-1)) {
						val subpart = mmpart.getBodyPart(j)
						logger debug "subpart " + j + ": " + subpart.getContentType
						subpartList.append(subpart)
					}
					subpartList.filter(p => !p.isMimeType("text/*")).foreach(p => mmpart.removeBodyPart(p))
					part.setContent(mmpart)
				}
				else partList.append(part)
			}
			partList.filter(p => { !p.isMimeType("text/*") && !p.isMimeType("multipart/*") })
				.foreach(p => mpart.removeBodyPart(p))
			newmsg.setContent(mpart)
			newmsg.saveChanges()
			newmsg
		} else if(newmsg.isMimeType("text/*")) {
			newmsg
		} else {
			newmsg.setContent("[removed attachment]", "text/plain")
			newmsg.saveChanges()
			newmsg
		}
	}


	def compressMessage(msg: MimeMessage): String = {
		val baos = new ByteArrayOutputStream()
		msg.writeTo(baos)
		CompressUtil.compress(baos.toString("UTF-8"))
	}

}
