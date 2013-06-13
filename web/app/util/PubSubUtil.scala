package util

import play.api.libs.iteratee.Concurrent
import com.heluna.cache.Redis
import com.redis.{M, U, S, E}

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 5/22/13
 * Time: 7:56 PM
 */

class PubSubUtil extends Object with Redis {

}

object PubSubUtil {

  def subscribe(name: String, channel: Concurrent.Channel[String]): PubSubUtil = {
    val client = new PubSubUtil()
    client.redis.subscribe(name)(message => message match {
      case E(ex) => {}
      case S(chan, num) => {}
      case U(chan, num) => {}
      case M(chan, msg) => channel push msg
    })
    client
  }

  def unsubscribe(name: String, client: PubSubUtil) {
    client.redis.unsubscribe(name)
  }

}
