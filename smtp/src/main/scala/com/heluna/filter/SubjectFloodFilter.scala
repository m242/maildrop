package com.heluna.filter

import com.typesafe.scalalogging.slf4j.Logging
import com.heluna.cache.Redis
import com.heluna.util.MailDropConfig
import com.heluna.model.{Continue, Reject}
import java.util.Date
import com.redis.serialization.Parse.Implicits.parseLong

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 5/1/13
 * Time: 7:45 PM
 */

object SubjectFloodFilter extends Object with Redis with Logging {

	def check(subject: String): Product = {
		val lowerSubj = Option(subject).getOrElse("").toLowerCase
		try {
			(isBanned(lowerSubj) || isFlooding(lowerSubj)) match {
				case true => {
					// Ban this IP address, or extend an existing ban.
					ban(lowerSubj)
					Reject(MailDropConfig("maildrop.subject.ban-response").getOrElse("Banned."))
				}
				case false => Continue()
			}
		} catch {
			// Redis is probably down here - just continue.
			case e: Exception => {
				logger error "Exception in SubjectFloodFilter: " + e.getMessage
				Continue()
			}
		}
	}

	def subjectKey(subject: String): String = "subj:" + subject
	def banKey(subject: String): String = "bansubj:" + subject

	def isBanned(subject: String): Boolean = redis.get[String](banKey(subject)).isDefined

	def isFlooding(subject: String): Boolean = {
		val now = new Date().getTime
		val maxMessages = MailDropConfig.getInt("maildrop.subject.flood-messages").getOrElse(20)
		val key = subjectKey(subject)
		redis.lpush(key, now)
		redis.ltrim(key, 0, maxMessages)
		redis.expire(key, MailDropConfig.getSeconds("maildrop.subject.flood-time").getOrElse(120))
		(redis.llen(key).getOrElse(0L).toInt > maxMessages) &&
			((now - redis.rpop[Long](key).getOrElse(0L)) < MailDropConfig.getMilliseconds("maildrop.subject.flood-time").getOrElse(120000L))
	}

	def ban(subject: String) {
		val key = banKey(subject)
		redis.set(key, "y")
		redis.expire(key, MailDropConfig.getSeconds("maildrop.subject.ban-time").getOrElse(300))
	}

}
