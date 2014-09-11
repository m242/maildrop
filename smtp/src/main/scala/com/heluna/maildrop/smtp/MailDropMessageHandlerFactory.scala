package com.heluna.maildrop.smtp

import org.subethamail.smtp.{MessageHandler, MessageContext, MessageHandlerFactory}

/**
 * smtp com.heluna.maildrop.smtp
 * User: markbe
 * Date: 9/10/14
 * Time: 10:34 AM
 */

class MailDropMessageHandlerFactory extends MessageHandlerFactory {

	override def create(ctx: MessageContext): MessageHandler = new MailDropMessageHandler(ctx)

}
