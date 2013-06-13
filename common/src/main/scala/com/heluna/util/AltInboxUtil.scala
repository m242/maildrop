package com.heluna.util

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/15/13
 * Time: 9:41 AM
 */

object AltInboxUtil {

	val modifier = BigInt(MailDropConfig.getLong("maildrop.data.alt-inbox-modifier").getOrElse(0L).toString)
	val prefix = MailDropConfig.getString("maildrop.data.alt-inbox-prefix").getOrElse("D-")

	def fromShort(shortId: String) = BigInt(shortId, 36)

	// Simple algorithm to determine alt inbox.
	// Warning -- this won't stop any dedicated attackers at all.
	// - Strip non alpha-numeric characters
	// - Convert the regular inbox to a long
	// - Reverse the digits and prepend a 1
	// - Add the private modifier
	// - Convert back to base36
	// - Prepend prefix

	def getAltInbox(regularInbox: String): String = {
		val regularBigInt = fromShort(regularInbox.toLowerCase.replaceAll("[^A-Za-z0-9]", ""))
		prefix + (BigInt("1" + regularBigInt.toString().reverse) + modifier).toString(36)
	}

	// Simple algorithm to determine regular inbox.
	// - Strip prefix
	// - Convert the alt inbox to a long
	// - Subtract the private modifier
	// - Remove the 1 prefix and reverse the digits
	// - Convert back to base36

	def getRegularInbox(altInbox: String): String = {
		val altBigInt = fromShort(altInbox.toLowerCase.replaceFirst(prefix.toLowerCase, ""))
		BigInt((altBigInt - modifier).toString().substring(1).reverse).toString(36)
	}

}
