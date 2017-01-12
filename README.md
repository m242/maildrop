MailDrop
========

See [MailDrop in action](http://maildrop.cc).

MailDrop is an open-source, scalable, high-performance version of Mailinator,
a "temporary inbox" that you can give out when you don't want to give out
your real e-mail address. MailDrop inboxes are designed to be quick and
disposable.

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

* [SBT 0.13+](http://www.scala-sbt.org/)
* [PlayFramework 2.3.4+](http://www.playframework.com/)
* [Redis 2.6+](http://redis.io/)

For hardware, an Amazon EC2 small instance should be good to begin.
MailDrop should take 512M of RAM for the SMTP module, 512M of RAM for
the web module, and 512M of RAM for the Redis datastore. In practice,
CPU is not an issue; MailDrop spends most of its time waiting on disk
or network IO.


Installation
------------

### Clone the repository into a local directory

This will give you three subdirectories, "common" is the shared set of utility
classes and models, "smtp" is the mail transfer agent, and "web" is the
website.

### Create the SMTP server

Go into "smtp" and run "sbt compile". This will create a jar of the MailDrop
SMTP server. If you like, you can create a single jar with all dependencies
included by running "sbt assembly".

To run the SMTP server, use "java -jar (jarfile) MailDrop". If you want to use a
custom configuration, use "java -Dconfig.file=/path/to/your/application.conf
-jar (jarfile) MailDrop".

### Create the web server

Go into "web" and run "activator dist". This will create a zipfile of the MailDrop
website. (More information about how to run a Play web app is located on the
[Play Framework site](http://www.playframework.com/))

To run the web server, simply use the "start" command inside the zipfile, or
again to specify a custom configuration use start 
-Dconfig.file=/path/to/your/application.conf


Changelog
---------

Version 2.0 is a rewrite of the internals of MailDrop. The site itself has
the same look and feel, but the way messages are dealt with is slightly
different.

The primary change is a switch to
[rediscala](https://github.com/etaty/rediscala) to connect to the Redis
instance. rediscala is a Reactive driver and performs much better than
the old Redis client.

Next, many of the actors and supervisors have been tossed in favor of
simple Scala futures. This has simplified the codebase while still
allowing for high performance-- MailDrop just won't be able to be as
finely tuned (custom dispatchers, guaranteed message delivery, etc).

Messages are now stored in Redis in json format instead of as serialized
ByteStrings. The overhead of json parsing on the website is minimal and
allows for a better overall view of the data inside Redis.

Last, Play has been updated to the 2.3 branch, along with automatic
asset versioning and a much cleaner way to cache assets. Website
performance should be increased vs. the previous version of MailDrop.

