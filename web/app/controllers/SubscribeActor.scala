package controllers

import java.net.InetSocketAddress
import com.heluna.maildrop.util.Redis
import play.api.libs.iteratee.Concurrent
import redis.actors.RedisSubscriberActor
import redis.api.pubsub.{PMessage, Message}

/**
 * web controllers
 * User: markbe
 * Date: 9/11/14
 * Time: 11:11 AM
 */

class SubscribeActor(channel: Concurrent.Channel[String])
	extends RedisSubscriberActor(new InetSocketAddress(Redis.hostname, Redis.port), Seq("blocked"), Seq("*")) {

	override def onMessage(m: Message) = channel.push(m.data)
	override def onPMessage(pm: PMessage) = channel.push(pm.data)

}
