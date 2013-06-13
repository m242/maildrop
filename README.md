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

Requirements
------------

* [SBT 0.12+](http://www.scala-sbt.org/)
* [PlayFramework 2.10+](http://www.playframework.com/)
* [Redis 2.4+](http://redis.io/)

Installation
------------

(more on this next)
