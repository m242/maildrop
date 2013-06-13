package com.heluna.smtp

import org.subethamail.smtp.{MessageHandler, MessageContext, MessageHandlerFactory}

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/1/13
 * Time: 1:47 PM
 */

class MailDropMessageHandlerFactory extends Object with MessageHandlerFactory {

	def create(ctx: MessageContext): MessageHandler = new MailDropMessageHandler(ctx)

}
