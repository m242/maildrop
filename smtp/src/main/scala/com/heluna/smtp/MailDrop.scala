package com.heluna.smtp

import org.subethamail.smtp.server.SMTPServer
import com.heluna.util.MailDropConfig
import java.net.InetAddress
import com.typesafe.scalalogging.slf4j.Logging
import java.util.Date

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 1:42 PM
 */

object MailDrop extends Logging {

	def main(args: Array[String]) {
		val smtpServer = new SMTPServer(new MailDropMessageHandlerFactory)
		smtpServer setConnectionTimeout MailDropConfig.getMilliseconds("maildrop.server.connection-timeout").getOrElse(60000L).toInt
		smtpServer setMaxConnections MailDropConfig.getInt("maildrop.server.max-connections").getOrElse(200)
		smtpServer setMaxRecipients 1
		smtpServer setMaxMessageSize MailDropConfig.getBytes("maildrop.data.max-message-size").getOrElse(100000L).toInt
		smtpServer setBindAddress InetAddress.getByName(MailDropConfig("maildrop.server.bind-address").getOrElse("127.0.0.1"))
		smtpServer setPort MailDropConfig.getInt("maildrop.server.bind-port").getOrElse(25000)
		smtpServer setSoftwareName "MailDrop"

		logger info "MailDrop startup at " + new Date().getTime
		smtpServer.start()
	}

}
