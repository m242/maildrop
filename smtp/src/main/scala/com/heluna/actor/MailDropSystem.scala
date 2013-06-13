package com.heluna.actor

import akka.actor.{Props, ActorSystem}
import akka.routing.FromConfig

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 4/30/13
 * Time: 9:23 PM
 */

object MailDropSystem {

	val system = ActorSystem("MailDropSystem")
	val senderCheckActor = system.actorOf(Props[SenderCheckActor].withRouter(FromConfig()), "sender")
	val recipientCheckActor = system.actorOf(Props[RecipientCheckActor].withRouter(FromConfig()), "recipient")
	val dataCheckActor = system.actorOf(Props[DataCheckActor].withRouter(FromConfig()), "data")
	val saveMessageActor = system.actorOf(Props[SaveMessageActor].withRouter(FromConfig()), "save")
	val metricsActor = system.actorOf(Props[MetricsActor].withRouter(FromConfig()), "metrics")

}
