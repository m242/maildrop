package controllers

import java.io.ByteArrayInputStream
import java.util.{Properties, Date}
import javax.mail.{Multipart, BodyPart}
import javax.mail.internet.MimeMessage
import akka.actor.{PoisonPill, Props}
import com.heluna.maildrop.util.{Mailbox, Metrics}
import play.api.Play
import play.api.data.Form
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.json.Json
import play.api.mvc._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Random
import play.api.data.Forms._

object Application extends Controller {
	import play.api.Play.current

	val adjectives = Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream("adjectives.txt")).getLines().toArray
	val adjectivesLen = adjectives.length
	val nouns = Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream("nouns.txt")).getLines().toArray
	val nounsLen = nouns.length
	val rand = new Random(System.currentTimeMillis())
	val sdf = new java.text.SimpleDateFormat(Play.current.configuration.getString("maildrop.dateformat").getOrElse("MMM d yyyy hh:mm a"))


	def index = Action.async {
		Metrics.getBlocked.map(blocked => Ok(views.html.index(getSuggestion, blocked)))
	}

	def getSuggestion = adjectives(rand.nextInt(adjectivesLen)).capitalize + nouns(rand.nextInt(nounsLen)).capitalize

	def getSuggestionJSON = Action { Ok(Json.toJson(Map("suggestion" -> getSuggestion))).as(JSON) }

	def blockedJSON = Action.async {
		Metrics.getBlocked.map(blocked => Ok(Json.toJson(Map("blocked" -> blocked))).as(JSON))
	}

	def blockedWS = WebSocket.using[String] { request =>
		val (out, chan) = Concurrent.broadcast[String]
		val client = Akka.system.actorOf(Props(classOf[SubscribeActor], chan).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))
		val in = Iteratee.foreach[String](s => {}).map { _ =>
			client ! PoisonPill
		}
		(in, out)
	}

	def how() = Action { Ok(views.html.howitworks()) }

	def contact() = Action { Ok(views.html.contact()) }

	def privacy() = Action { Ok(views.html.privacy()) }

	val inboxForm = Form(
		single("mailbox" -> nonEmptyText(minLength = 1, maxLength = Play.configuration.getInt("maildrop.recipient.max-addr-length").getOrElse(20)))
	)

	def postInbox = Action { implicit request =>
		inboxForm.bindFromRequest.fold(formWithErrors => Redirect(routes.Application.inbox("lazy")),
			validForm => Redirect(routes.Application.inbox(validForm.replaceAll("\\s", "").split("@")(0).toLowerCase)))
	}

	def inbox(name: String) = Action.async {
		Mailbox.list(name).map(messages => Ok(views.html.inbox(name, messages)).withCookies(new Cookie("inbox", name.toLowerCase, None, "/", None, false, false)))
	}

	def inboxJSON(name: String) = Action.async {
		Mailbox.list(name).map(messages => {
			val formatted = messages.map(jsvalue => Json.toJson(Map(
				"id" -> (jsvalue \ "id").asOpt[String].getOrElse(""),
				"sender" -> (jsvalue \ "sender").asOpt[String].getOrElse("(no sender)"),
				"subject" -> (jsvalue \ "subject").asOpt[String].getOrElse("(no subject)"),
				"date" -> sdf.format(new Date((jsvalue \ "date").asOpt[Long].getOrElse(0L)))
			)))
			Ok(Json.toJson(formatted)).as(JSON)
		})
	}

	def message(inbox: String, id: String) = Action.async {
		Mailbox.get(inbox, id).map {
			case Some(message) =>
				val body = (message \ "body").as[String]
				val mimemessage = new MimeMessage(javax.mail.Session.getInstance(new Properties()), new ByteArrayInputStream(body.getBytes("UTF-8")))
				Ok(views.html.message(inbox, id, message, mimemessage))
			case None => NotFound(views.html.notfound(routes.Application.message(inbox, id).toString()))
		}
	}

	def messageRaw(inbox: String, id: String) = Action.async {
		Mailbox.get(inbox, id).map {
			case Some(message) => Ok((message \ "body").as[String])
			case None => NotFound(views.html.notfound(routes.Application.message(inbox, id).toString()))
		}
	}

	def messageBody(inbox: String, id: String) = Action.async {
		Mailbox.get(inbox, id).map {
			case Some(message) =>
				val body = (message \ "body").as[String]
				val mimemessage = new MimeMessage(javax.mail.Session.getInstance(new Properties()), new ByteArrayInputStream(body.getBytes("UTF-8")))
				Ok(views.html.messagebody(mimemessage))
			case None => NotFound(views.html.notfound(routes.Application.message(inbox, id).toString()))
		}
	}

	def messageJSON(inbox: String, id: String) = Action.async {
		Mailbox.get(inbox, id).map {
			case Some(message) => Ok(Json.toJson(message)).as(JSON).withHeaders(("Cache-Control", "public, max-age=3600"))
			case None => NotFound("{}").as(JSON)
		}
	}

	def delete(inbox: String, id: String) = Action {
		Mailbox.delete(inbox, id)
		Redirect(routes.Application.inbox(inbox))
	}

	def deleteJSON(inbox: String, id: String) = Action {
		Mailbox.delete(inbox, id)
		Ok(Json.toJson(Map("deleted" -> id))).as(JSON)
	}

	def getParts(message: MimeMessage): List[BodyPart] = {
		val mpart = message.getContent.asInstanceOf[Multipart]
		val partList = ArrayBuffer[BodyPart]()
		for(i <- 0 to (mpart.getCount-1)) {
			val part = mpart.getBodyPart(i)
			// Go only one more level down if necessary - this isn't Inception
			if(part.isMimeType("multipart/*")) {
				val mmpart = part.getContent.asInstanceOf[Multipart]
				for(j <- 0 to (mmpart.getCount-1)) {
					partList.append(mmpart.getBodyPart(j))
				}
			} else partList.append(part)
		}
		partList.toList
	}

}


