name := "smtp"

version := "2.0"

lazy val common = RootProject(file("../common"))

val main = Project(id="smtp", base=file(".")).dependsOn(common)

scalaVersion := "2.11.2"

resolvers ++= Seq(
	"rediscala" at "http://dl.bintray.com/etaty/maven",
	"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
	"com.typesafe.akka" % "akka-actor_2.11" % "2.3.6",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"ch.qos.logback" % "logback-classic" % "1.1.2",
	"org.apache.james.jspf" % "apache-jspf-resolver" % "1.0.0",
	"com.typesafe.play" %% "play-json" % "2.3.4",
	"org.subethamail" % "subethasmtp" % "3.1.7",
	"dnsjava" % "dnsjava" % "2.1.6"
)
