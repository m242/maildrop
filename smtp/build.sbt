import AssemblyKeys._

assemblySettings

name := "maildropsmtp"

version := "1.0"

scalaVersion := "2.10.0"

resolvers += "JBoss Thirdparty Uploads" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-uploads/"

unmanagedSourceDirectories in Compile += file("../common/src")

libraryDependencies ++= Seq(
	"com.typesafe" % "config" % "1.0.0",
	"com.typesafe" % "scalalogging-slf4j_2.10" % "1.0.1",
	"com.typesafe.akka" % "akka-actor_2.10" % "2.1.2",
	"net.debasishg" % "redisclient_2.10" % "2.10",
	"org.apache.james.jspf" % "apache-jspf-resolver" % "1.0.0",
	"org.json4s" % "json4s-native_2.10" % "3.2.2",
	"org.slf4j" % "slf4j-api" % "1.7.5",
	"org.slf4j" % "slf4j-log4j12" % "1.7.5",
	"org.subethamail" % "subethasmtp" % "3.1.7",
	"org.xbill" % "dnsjava" % "2.0.8"
)

mainClass in assembly := Some("com.heluna.smtp.MailDrop")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case "application.conf" => MergeStrategy.concat
    case "log4j.properties" => MergeStrategy.first
    case x => old(x)
  }
}
