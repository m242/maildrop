MailDrop
========

MailDrop is an open-source, scalable, high-performance version of Mailinator.
The design goals are to be roughly 90% of the speed of Mailinator, while
adding additional functionality and the ability to horizontally scale
quickly and easily.

Where Mailinator runs within one JVM, MailDrop splits the SMTP and web 
applications into separate JVMs, while using Redis as its main data store.
This allows for a more fluid application architecture, while still retaining
the speed expected for a high-speed mail and web app.

MailDrop is written in Scala, with heavy use of Akka actors and the Play
Framework. Functionality includes:

* Antispam modules contributed from [Heluna](https://heluna.com/) for
senders and data
* 90% of all spam attempts rejected
* Network blacklists
* IP connection and message subject limiting
* Reputation-based blocking
* SPF checking
* Greylisting
* Alternate inbox aliases
* Strip message attachments
* Message size limits
* SMTP configuration done in one file
* Easy-to-modify website, written in LESS and CoffeeScript


Requirements
------------

* [SBT 0.12+](http://www.scala-sbt.org/)
* [PlayFramework 2.10+](http://www.playframework.com/)
* [Redis 2.4+](http://redis.io/)

Installation
------------

(more on this next)
