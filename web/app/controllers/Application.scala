package controllers

import com.heluna.util.{BlockedUtil, MailboxUtil}
import java.util.Date
import scala.io.Source
import scala.util.Random
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Play
import util.{PubSubUtil, MessageUtil}
import play.api.libs.iteratee.{Concurrent, Iteratee}

object Application extends Controller {

  val adjectives = Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream("adjectives.txt")).getLines().toArray
  val adjectivesLen = adjectives.length
  val nouns = Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream("nouns.txt")).getLines().toArray
  val nounsLen = nouns.length
  val rand = new Random(System.currentTimeMillis())

  def index = Action { Ok(views.html.index(getSuggestion, BlockedUtil.getBlocked)) }

  def getSuggestionJSON = Action { Ok(Json.toJson(Map("suggestion" -> getSuggestion))).as(JSON) }

  def getSuggestion = adjectives(rand.nextInt(adjectivesLen)).capitalize + nouns(rand.nextInt(nounsLen)).capitalize

  def blockedJSON = Action { Ok(Json.toJson(Map("blocked" -> BlockedUtil.getBlocked))).as(JSON) }

  def blockedWS = WebSocket.using[String] { request =>
    val (enum, chan) = Concurrent.broadcast[String]
    val client = PubSubUtil.subscribe(BlockedUtil.CHANNEL, chan)
    val in = Iteratee.foreach[String](s => {}).mapDone(_ => PubSubUtil.unsubscribe("blocked-channel", client))
    (in, enum)
  }

  def postInbox = Action { implicit request =>
    import play.api.Play.current
    val inboxForm = Form(single(
      "mailbox" -> nonEmptyText(minLength = 1, maxLength = Play.configuration.getInt("maildrop.recipient.max-addr-length").getOrElse(80))
    ))
    inboxForm.bindFromRequest.fold(
      formWithErrors => Redirect(routes.Application.inbox("lazy")),
      validForm => Redirect(routes.Application.inbox(validForm.replaceAll("\\s", "").split("@")(0).toLowerCase))
    )
  }

  def inbox(name: String) = Action { Ok(views.html.inbox(name, MailboxUtil list name)).withCookies(new Cookie("inbox", name.toLowerCase, None, "/", None, false, false)) }

  def inboxJSON(name: String) = Action { Ok(Json.toJson(MailboxUtil list name)).as(JSON) }

  def message(inbox: String, id: String) = Action {
    MailboxUtil.get(inbox, id).fold(NotFound(views.html.notfound(routes.Application.message(inbox, id).toString())))(msg => {
      val m = MessageUtil.parseMessage(msg)
      Ok(views.html.message(inbox, id,
        m.getFrom.map(_.toString).headOption.getOrElse("(no sender)"),
        m.getRecipients(javax.mail.Message.RecipientType.TO).map(_.toString).headOption.getOrElse("(no recipient)"),
        Option(m.getSubject).getOrElse("(no subject)"),
        MailboxUtil.renderDate(Option(m.getSentDate).getOrElse(new Date())),
        m.isMimeType("text/html") || m.isMimeType("multipart/*") && !MessageUtil.getParts(m).filter(p => p.isMimeType("text/html")).isEmpty,
        m
      ))
    })
  }

  def messageBody(inbox: String, id: String) = Action {
    MailboxUtil.get(inbox, id).fold(NotFound(views.html.notfound(routes.Application.message(inbox, id).toString())))(msg => {
      val m = MessageUtil.parseMessage(msg)
      Ok(views.html.messagebody(inbox, id,
        m.getFrom.map(_.toString).headOption.getOrElse("(no sender)"),
        m.getRecipients(javax.mail.Message.RecipientType.TO).map(_.toString).headOption.getOrElse("(no recipient)"),
        Option(m.getSubject).getOrElse("(no subject)"),
        MailboxUtil.renderDate(Option(m.getSentDate).getOrElse(new Date())),
        m.isMimeType("text/html") || m.isMimeType("multipart/*") && !MessageUtil.getParts(m).filter(p => p.isMimeType("text/html")).isEmpty,
        m
      )).withHeaders(("Cache-Control", "public,max-age=3600"))
    })
  }

  def messageRaw(inbox: String, id: String) = Action {
    MailboxUtil.get(inbox, id).fold(NotFound("Not found."))(msg => Ok(msg)
      .withHeaders(("Cache-Control", "public,max-age=3600")))
  }

  def messageJSON(inbox: String, id: String) = Action {
    MailboxUtil.get(inbox, id).fold(NotFound("{}").as(JSON))(msg =>
      Ok(Json.toJson(MessageUtil.parseMessage(msg))(MessageUtil.messageWrites)).as(JSON)
        .withHeaders(("Cache-Control", "public,max-age=3600"))
    )
  }

  def delete(inbox: String, id: String) = Action {
    MailboxUtil.del(inbox, id)
    Redirect(routes.Application.inbox(inbox))
  }

  def deleteJSON(inbox: String, id: String) = Action {
    MailboxUtil.del(inbox, id)
    Ok(Json.toJson(Map("deleted" -> true))).as(JSON)
  }

  def how() = Action { Ok(views.html.howitworks()) }

  def contact() = Action { Ok(views.html.contact()) }

  def privacy() = Action { Ok(views.html.privacy()) }

}