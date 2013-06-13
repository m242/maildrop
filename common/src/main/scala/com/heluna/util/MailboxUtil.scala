package com.heluna.util

import com.heluna.cache.Redis
import java.util.Date
import java.text.SimpleDateFormat
import org.json4s.NoTypeHints
import org.json4s.native.Serialization._
import org.json4s.native.Serialization
import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 5/9/13
 * Time: 7:48 PM
 */

object MailboxUtil extends Object with Redis {

	val sdf = new SimpleDateFormat(MailDropConfig("maildrop.dateformat").getOrElse("MMM d yyyy hh:mm a"))

	def add(sender: String, recipient: String, subject: String, date: Date, compressedMessage: String) {
		implicit val formats = Serialization.formats(NoTypeHints)
		val lkey = listKey(recipient)
		val mkey = mailboxKey(recipient)
		val id = shortId
		redis.lpush(lkey, write(Map("id" -> id, "sender" -> sender, "recipient" -> recipient, "subject" -> subject, "date" -> sdf.format(date))))
		redis.lpush(mkey, write(Map("id" -> id, "message" -> compressedMessage)))
		redis.ltrim(lkey, 0, MailDropConfig.getInt("maildrop.mailbox.max-length").getOrElse(10) - 1)
		redis.ltrim(mkey, 0, MailDropConfig.getInt("maildrop.mailbox.max-length").getOrElse(10) - 1)
		redis.expire(lkey, MailDropConfig.getSeconds("maildrop.mailbox.expires").getOrElse(86400))
		redis.expire(mkey, MailDropConfig.getSeconds("maildrop.mailbox.expires").getOrElse(86400))
	}

	def list(recipient: String): List[Map[String, String]] = {
		implicit val formats = Serialization.formats(NoTypeHints)
		val l: List[Option[String]] = redis.lrange[String](listKey(recipient), 0, MailDropConfig.getInt("maildrop.mailbox.max-length")
			.getOrElse(10)).getOrElse(List[Option[String]]())
		l.map(item => read[Map[String, String]](item.getOrElse("{}"))).toList
	}

  def get(recipient: String, id: String): Option[String] = {
    getMessages(recipient).find(_.get("id").getOrElse("") == id).getOrElse(Map[String, String]())
      .get("message").map(str => CompressUtil.decompress(str))
  }

	def del(recipient: String, num: Int) {
		redis.lset(listKey(recipient), num, "__deleted__")
		redis.lrem(listKey(recipient), 0, "__deleted__")
		redis.lset(mailboxKey(recipient), num, "__deleted__")
		redis.lrem(mailboxKey(recipient), 0, "__deleted__")
	}

  def del(recipient: String, id: String) {
    var i = 0
    for(msg <- getMessages(recipient)) {
      msg.get("id").getOrElse("") == id match {
        case true => del(recipient, i)
        case false => {}
      }
      i = i + 1
    }
  }

  def getMessages(recipient: String): List[Map[String, String]] = {
    implicit val formats = Serialization.formats(NoTypeHints)
    val range = redis.lrange[String](mailboxKey(recipient), 0, 1000).getOrElse(List[Option[String]]())
    range.map(opt => read[Map[String, String]](opt.getOrElse("{}")))
  }

  def len(recipient: String): Int = redis.llen(mailboxKey(recipient)).getOrElse(0L).toInt

	def mailboxKey(recipient: String) = "mailbox:" + recipient.toLowerCase
	def listKey(recipient: String) = "mailboxlist:" + recipient.toLowerCase

  def shortId = Integer.toString(new Random().nextInt(100000),36)

  def renderDate(date: Date) = sdf.format(date)

}
