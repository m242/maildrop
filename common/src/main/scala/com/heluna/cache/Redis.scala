package com.heluna.cache

import com.heluna.util.MailDropConfig
import com.redis.RedisClient

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 4/30/13
 * Time: 8:29 PM
 */

trait Redis {

	val redis = new RedisClient(
		MailDropConfig("maildrop.redis.host").getOrElse("localhost"),
		MailDropConfig.getInt("maildrop.redis.port").getOrElse(6379)
	)

}
