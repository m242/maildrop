package com.heluna.maildrop.util

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import redis.RedisClient
import redis.actors.RedisSubscriberActor
import redis.api.pubsub.{Message, PMessage}

/**
 * smtp com.heluna.maildrop.util
 * User: markbe
 * Date: 9/10/14
 * Time: 11:09 AM
 */

object Redis {

	implicit val redisSystem = ActorSystem()

	val hostname = MailDropConfig("maildrop.redis.host").getOrElse("localhost")
	val port = MailDropConfig.getInt("maildrop.redis.port").getOrElse(6379)
	val password = MailDropConfig("maildrop.redis.password")
	val db = MailDropConfig.getInt("maildrop.redis.db")

	val client = RedisClient(hostname, port, password, db)

}
