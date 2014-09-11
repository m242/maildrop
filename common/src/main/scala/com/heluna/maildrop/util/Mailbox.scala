package com.heluna.maildrop.util

import java.util.Date
import play.api.libs.json.{JsValue, Json}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

/**
 * smtp com.heluna.maildrop.util
 * User: markbe
 * Date: 9/10/14
 * Time: 11:53 AM
 */

object Mailbox {

	val maxLength = MailDropConfig.getInt("maildrop.mailbox.max-length").getOrElse(10)
	val expiry = MailDropConfig.getSeconds("maildrop.mailbox.expires").getOrElse(86400L)

	def key(recipient: String) = "mailbox:" + recipient.toLowerCase

	def add(sender: String, recipient: String, subject: String, date: Date = new Date(), body: String): Unit = {
		val id = shortId
		val rkey = key(recipient)
		val json = Json.obj(
			"id" -> id,
			"sender" -> sender,
			"recipient" -> recipient,
			"subject" -> subject,
			"date" -> date.getTime,
			"body" -> body
		)
		Redis.client.lpush(rkey, Json.stringify(json))
		Redis.client.ltrim(rkey, 0, maxLength - 1)
		Redis.client.expire(rkey, expiry)
	}

	def list(recipient: String): Future[List[JsValue]] =
		Redis.client.lrange[String](key(recipient), 0, maxLength).map(seq => seq.map(json => Json.parse(json)).toList)

	def get(recipient: String, id: String): Future[Option[JsValue]] =
		list(recipient).map(messages => messages.find(json => (json \ "id").as[String] == id))

	def delete(recipient: String, id: String): Unit = {
		val rkey = key(recipient)
		list(recipient).foreach(messages => {
			val idx = messages.indexWhere(json => (json \ "id").as[String] == id)
			Redis.client.lset(rkey, idx, "__deleted__")
			Redis.client.lrem(rkey, 0, "__deleted__")
		})
	}

	def shortId = Random.alphanumeric.take(6).mkString

}
