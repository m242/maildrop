package com.heluna.util

import com.heluna.cache.Redis

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/22/13
 * Time: 3:57 PM
 */
object BlockedUtil extends Object with Redis {

  val CHANNEL = "blocked-channel"

  def getBlocked = redis.get("blocked").map(_.toLong).getOrElse(0L)

}
