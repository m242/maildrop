import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "MailDropWeb"
  val appVersion      = "1.0"

  val appDependencies = Seq(
    // Add your project dependencies here,
		"net.debasishg" % "redisclient_2.10" % "2.10",
		"com.typesafe" % "config" % "1.0.0",
		"org.json4s" % "json4s-native_2.10" % "3.2.2",
    "javax.mail" % "mail" % "1.4.7",
    "com.typesafe.play.extras" %% "iteratees-extras" % "1.0.1"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
		unmanagedSourceDirectories in Compile <+= baseDirectory{ _ / "../common/src/main/scala" }
  )

}
