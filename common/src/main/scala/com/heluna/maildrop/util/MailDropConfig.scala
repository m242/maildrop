package com.heluna.maildrop.util

import java.io.File
import java.util.concurrent.TimeUnit
import com.typesafe.config.{Config, ConfigFactory}
import scala.util.Try
import scala.collection.JavaConversions._

/**
 * common com.heluna.maildrop
 * User: markbe
 * Date: 9/10/14
 * Time: 9:39 AM
 */

object MailDropConfig {

	val config = Option(System.getProperty("config.file")).fold[Config](ConfigFactory.load())(f => ConfigFactory.parseFile(new File(f)).withFallback(ConfigFactory.load()))

	def getBytes(key: String): Option[Long] = Try(Option(config.getBytes(key).toLong)).toOption.flatten

	def getInt(key: String): Option[Int] = Try(Option(config.getInt(key))).toOption.flatten

	def getLong(key: String): Option[Long] = Try(Option(config.getLong(key))).toOption.flatten

	def getMilliseconds(key: String): Option[Long] = Try(Option(config.getDuration(key, TimeUnit.MILLISECONDS))).toOption.flatten

	def getSeconds(key: String): Option[Long] = Try(Option(config.getDuration(key, TimeUnit.SECONDS))).toOption.flatten

	def getString(key: String): Option[String] = Try(Option(config.getString(key))).toOption.flatten

	def getStringList(key: String): List[String] = Try(config.getStringList(key).toList).toOption.getOrElse(List[String]())

	def apply(key: String) = getString(key)

}
