package com.heluna.smtp

import org.subethamail.smtp.{RejectException, DropConnectionException, MessageHandler, MessageContext}
import com.typesafe.scalalogging.slf4j.Logging
import java.io.InputStream
import java.net.InetSocketAddress
import java.util.{Properties, Date}
import com.heluna.util.MailDropConfig
import com.heluna.actor.MailDropSystem
import akka.pattern.ask
import com.heluna.model.{Greylist, NewTo, Reject, Continue}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await
import javax.mail.Session
import javax.mail.internet.MimeMessage

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 1:47 PM
 */

class MailDropMessageHandler(ctx: MessageContext) extends Object with MessageHandler with Logging {

	val inet = ctx.getRemoteAddress.asInstanceOf[InetSocketAddress].getAddress
	val ip = inet.getHostAddress
	var sender: Option[String] = None
	var recipient: Option[String] = None
	val timestamp = new Date().getTime
	val delay = MailDropConfig.getMilliseconds("maildrop.server.command-delay").getOrElse(10000L)

	def from(addr: String) {
		val helo = Option(ctx.getHelo).getOrElse(ip).toLowerCase
		val from = addr.toLowerCase
		logger debug "Starting"

		// Metrics actor log connection
		MailDropSystem.metricsActor ! "connection"

		// Wait maildrop.command-delay seconds
		Thread sleep delay
		logger debug "From: " + from

		// Sender Check - Continue or Reject
		implicit val timeout = Timeout(2 minutes)
		val future = MailDropSystem.senderCheckActor ? (inet, helo, from)
		Await.result(future, 2 minutes) match {
			case Continue() => sender = Some(from)
			case Reject(reason) => {
				logger debug "Reject ip: " + ip + " helo: " + helo + " from: " + from + " reason: " + reason
				MailDropSystem.metricsActor ! "blocked"
				throw new DropConnectionException(reason)
			}
			case Greylist(reason) => {
				MailDropSystem.metricsActor ! "blocked"
				throw new DropConnectionException(421, reason)
			}
			case _ => throw new Exception("Error in processing.")
		}
	}


	def recipient(addr: String) {
		val to = addr.toLowerCase.split("@")(0)

		// Wait maildrop.command-delay seconds
		Thread sleep delay
		logger debug "To: " + to

		// Recipient Check - Continue or Reject
		implicit val timeout = Timeout(2 minutes)
		val future = MailDropSystem.recipientCheckActor ? to
		Await.result(future, 2 minutes) match {
			case NewTo(newTo) => recipient = Some(newTo)
			case Continue() => recipient = Some(to)
			case Reject(reason) => {
				logger debug "Reject recipient ip: " + ip + " to: " + to + " reason: " + reason
				MailDropSystem.metricsActor ! "blocked"
				throw new RejectException(reason)
			}
		}

	}


	def data(data: InputStream) {
		// Wait maildrop.command-delay seconds
		Thread sleep delay

		// Create MimeMessage from stream
		val session = Session.getInstance(new Properties())
		val message = new MimeMessage(session, data)

		// Data Check - Continue or Reject
		implicit val timeout = Timeout(2 minutes)
		val future = MailDropSystem.dataCheckActor ? message
		Await.result(future, 2 minutes) match {
			case Continue() => {
				// Save message to cache
				MailDropSystem.saveMessageActor ! (sender.getOrElse(""), recipient.getOrElse(""), message)

				// Metrics actor log message
				MailDropSystem.metricsActor ! "message"
			}
			case Reject(reason) => {
				logger debug "Reject data ip: " + ip + " reason: " + reason
				MailDropSystem.metricsActor ! "blocked"
				throw new RejectException(reason)
			}
		}
	}


	def done() {
		logger debug "Finished at " + new Date().getTime
	}
}
