package util

import com.heluna.util.MailboxUtil
import java.io.ByteArrayInputStream
import java.util.{Properties, Date}
import javax.mail.internet.MimeMessage
import javax.mail.{Multipart, BodyPart, Message}
import play.api.libs.json.{Json, JsValue, Writes}
import scala.collection.mutable.ArrayBuffer

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/14/13
 * Time: 2:16 PM
 */

object MessageUtil {

  val messageWrites = new Writes[MimeMessage] {
    def writes(m: MimeMessage): JsValue = {
      Json.obj(
        "from" -> m.getFrom.headOption.fold("(no sender")(_.toString),
        "to" -> m.getRecipients(Message.RecipientType.TO).headOption.fold("(no recipient)")(_.toString),
        "subject" -> Json.toJson(Option(m.getSubject).getOrElse("(no subject)")),
        "date" -> MailboxUtil.renderDate(Option(m.getSentDate).getOrElse(new Date())),
        "contentType" -> Json.toJson(Option(m.getContentType).getOrElse("text/plain")),
        "content" -> {
          m.isMimeType("multipart/*") match {
            case true => Json.toJson(getParts(m))(partListWrites)
            case false => m.getContent.asInstanceOf[String]
          }
        }
      )
    }
  }

  val partListWrites = new Writes[List[BodyPart]] {
    def writes(bp: List[BodyPart]) = Json.toJson(bp.map(p => Json.toJson(p)(partWrites)))
  }

  val partWrites = new Writes[BodyPart] {
    def writes(p: BodyPart): JsValue = {
      Json.obj(
        "contentType" -> Json.toJson(Option(p.getContentType).getOrElse("text/plain")),
        "content" -> Json.toJson(p.getContent.asInstanceOf[String])
      )
    }
  }

  def parseMessage(msg: String): MimeMessage =
    new MimeMessage(javax.mail.Session.getInstance(new Properties()), new ByteArrayInputStream(msg.getBytes("UTF-8")))

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
