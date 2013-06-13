package com.heluna.util

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 4/30/13
 * Time: 8:51 PM
 */

object MailDropConfig {

	val config = ConfigFactory.load(this.getClass.getClassLoader)

	def getBytes(key: String): Option[Long] = {
		try {
			Some(config getBytes key)
		} catch {
			case e: Exception => None
		}
	}

	def getInt(key: String): Option[Int] = {
		try {
			Some(config getInt key)
		} catch {
			case e: Exception => None
		}
	}

	def getDouble(key: String): Option[Double] = {
		try {
			Some(config getDouble key)
		} catch {
			case e: Exception => None
		}
	}

	def getLong(key: String): Option[Long] = {
		try {
			Some(config getLong key)
		} catch {
			case e: Exception => None
		}
	}

	def getMilliseconds(key: String): Option[Long] = {
		try {
			Some(config getMilliseconds key)
		} catch {
			case e: Exception => None
		}
	}

	def getSeconds(key: String): Option[Int] = {
		getMilliseconds(key) match {
			case Some(ms) => Some(math.round(ms / 1000))
			case None => None
		}
	}

	def getString(key: String): Option[String] = {
		try {
			Some(config getString key)
		} catch {
			case e: Exception => None
		}
	}

	def getStringList(key: String): Option[List[String]] = {
		try {
			Some(config.getStringList(key).toList)
		} catch {
			case e: Exception => None
		}
	}

	def apply(key: String) = getString(key)
}
