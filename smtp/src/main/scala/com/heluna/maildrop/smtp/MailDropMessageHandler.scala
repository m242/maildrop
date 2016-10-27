package com.heluna.maildrop.smtp

import java.io.{ByteArrayOutputStream, InputStream}
import java.net.{InetAddress, InetSocketAddress}
import java.util.{Properties, Date}
import javax.mail.{Multipart, BodyPart, Session}
import javax.mail.internet.MimeMessage
import com.heluna.maildrop.smtp.filters._
import com.heluna.maildrop.util.{Mailbox, Metrics, MailDropConfig}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.subethamail.smtp.{RejectException, DropConnectionException, MessageHandler, MessageContext}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
 * smtp com.heluna.maildrop.smtp
 * User: markbe
 * Date: 9/10/14
 * Time: 10:35 AM
 */

class MailDropMessageHandler(ctx: MessageContext) extends MessageHandler with LazyLogging {

	val inet = ctx.getRemoteAddress.asInstanceOf[InetSocketAddress].getAddress
	val ip = inet.getHostAddress
	var sender: String = _
	var recipient: String = _
	var helo: String = _
	val timestamp = new Date().getTime

	override def from(addr: String): Unit = {
		logger.debug("Starting connection from " + ip)
		helo = Option(ctx.getHelo).getOrElse(ip).toLowerCase
		sender = Option(addr).getOrElse("").toLowerCase.trim

		// Log our connection
		Metrics.connection()

		// Wait maildrop.command-delay seconds
		Thread.sleep(MailDropMessageHandler.threadDelay)

		if (sender.length == 0) {
			logger.info("Sender " + ip + " " + sender + " rejected: no address")
			Metrics.blocked()
			throw new DropConnectionException("No sender email address.")
		}

		val future = MailDropMessageHandler.senderFilter(inet, helo, sender)
		Await.result(future, 2.minutes) match {
			case Greylist(reason) =>
				logger.info("Sender " + ip + " " + sender + " greylisted: " + reason)
				Metrics.blocked()
				throw new DropConnectionException(421, reason)
			case Reject(reason) =>
				logger.info("Sender " + ip + " " + sender + " rejected: " + reason)
				Metrics.blocked()
				CacheFilter.add(inet, helo, Reject(reason))
				HostEntry.touch(inet, helo)
				throw new DropConnectionException(reason)
			case _ =>
		}

	}

	override def recipient(addr: String): Unit = {
		recipient = Option(addr).getOrElse("").toLowerCase.split("@")(0)

		// Wait maildrop.command-delay seconds
		Thread.sleep(MailDropMessageHandler.threadDelay)

		// Run the recipient filter, either Reject, Continue, or NewTo
		val future = MailDropMessageHandler.recipientFilter(recipient)
		Await.result(future, 2.minutes) match {
			case Reject(reason) =>
				logger.info("Recipient from " + ip + " " + sender + " to " + recipient + " rejected: " + reason)
				Metrics.blocked()
				throw new RejectException(reason)
			case NewTo(newAddr) => recipient = newAddr
			case _ =>
		}

	}

	override def data(data: InputStream): Unit = {

		// Wait maildrop.command-delay seconds
		Thread.sleep(MailDropMessageHandler.threadDelay)

		// Create MimeMessage from stream
		val session = Session.getInstance(new Properties())
		Try(new MimeMessage(session, data)).toOption match {
			case Some(message) =>
				// Run the data filter, either Reject or Continue
				val future = MailDropMessageHandler.dataFilter(message)
				Await.result(future, 2.minutes) match {
					case Reject(reason) =>
						logger.info("Data from " + ip + " " + sender + " to " + recipient + " rejected: " + reason)
						Metrics.blocked()
						throw new RejectException(reason)
					case Continue() =>
						// Save the message
						Try(MailDropMessageHandler.saveMessage(sender, recipient, message)).toOption match {
							case Some(x) =>
								logger.info("Message saved from " + ip + " " + sender + " to " + recipient)
								// Cache this sender
								CacheFilter.add(inet, helo, Accept())
								HostEntry.touch(inet, helo)
								Metrics.message()
							case None =>
								logger.info("Data from " + ip + " " + sender + " to " + recipient + " rejected: no attachments")
								Metrics.blocked()
								throw new RejectException("No attachments allowed.")
						}
				}
			case _ =>
		}

	}

	override def done(): Unit = {
		logger.debug("Ending connection from " + ip)
	}


}


object MailDropMessageHandler extends LazyLogging {
	import scala.concurrent.ExecutionContext.Implicits.global

	val threadDelay = MailDropConfig.getMilliseconds("maildrop.server.command-delay").getOrElse(2000L)
	val maxRecipientLength = MailDropConfig.getInt("maildrop.recipient.max-addr-length").getOrElse(25)
	val maxRecipientReason = MailDropConfig("maildrop.recipient.response").getOrElse("Invalid recipient.")
	val maxMessageSize = MailDropConfig.getBytes("maildrop.data.max-message-size").getOrElse(100000L).toInt
	val maxMessageReason = MailDropConfig("maildrop.data.max-message-reason").getOrElse("Message too large.")

	def senderFilter(inet: InetAddress, helo: String, sender: String): Future[Product] = {
		for {
			host <- HostEntry(inet, helo)
			cache <- trySenderFilter(Continue(), { CacheFilter(host) })
			greylist <- trySenderFilter(cache, { GreylistFilter(host, inet, helo) })
			spf <- trySenderFilter(greylist, { SPFFilter(inet, helo, sender) })
			dnsbl <- trySenderFilter(spf, { DNSBLFilter(inet, helo) })
		} yield dnsbl
	}

	def trySenderFilter(prevResult: Product, filter: => Future[Product]): Future[Product] = {
		prevResult match {
			case Continue() => filter
			case result => Future.successful(result)
		}
	}

	def recipientFilter(recipient: String): Future[Product] = {
		AltInboxFilter(recipient) flatMap {
			case Continue() => Future {
				recipient.length > maxRecipientLength match {
					case true => Reject(maxRecipientReason)
					case false => Continue()
				}
			}
			case result => Future.successful(result)
		}
	}

	def dataFilter(message: MimeMessage): Future[Product] = Future {
		message.getSize > maxMessageSize match {
			case true => Reject(maxMessageReason)
			case false => Continue()
		}
	}

	def saveMessage(sender: String, recipient: String, message: MimeMessage): Boolean = {
		// Strip attachments from the message
		val newmessage = stripAttachments(message)
		val baos = new ByteArrayOutputStream()
		newmessage.writeTo(baos)
		// Add the message to the mailbox
		Mailbox.add(sender, recipient, Option(message.getSubject).getOrElse("(no subject)"), Option(message.getSentDate).getOrElse(new Date()), baos.toString("UTF-8"))
		true
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

}


case class Accept()
case class Continue()
case class Greylist(reason: String)
case class Reject(reason: String)
case class NewTo(recipient: String)
