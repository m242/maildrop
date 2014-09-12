package com.heluna.maildrop.smtp

import java.util.Date
import com.heluna.maildrop.util.MailDropConfig
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.subethamail.smtp.server.SMTPServer

/**
 * smtp com.heluna.maildrop.smtp
 * User: markbe
 * Date: 9/10/14
 * Time: 9:50 AM
 */

object MailDropSMTP extends App with LazyLogging {

	val server = new SMTPServer(new MailDropMessageHandlerFactory)

	server.setConnectionTimeout(MailDropConfig.getMilliseconds("maildrop.server.connection-timeout").getOrElse(60000L).toInt)
	server.setMaxConnections(MailDropConfig.getInt("maildrop.server.max-connections").getOrElse(200))
	server.setMaxRecipients(1)
	server.setMaxMessageSize(MailDropConfig.getBytes("maildrop.data.max-message-size").getOrElse(100000L).toInt)
	server.setBindAddress(java.net.InetAddress.getByName(MailDropConfig("maildrop.server.bind-address").getOrElse("127.0.0.1")))
	server.setPort(MailDropConfig.getInt("maildrop.server.bind-port").getOrElse(25000))
	server.setSoftwareName("MailDrop")

	logger.info("MailDrop SMTP startup at " + new Date().getTime)
	server.start()

}
