name := "web"

version := "2.0"

lazy val common = RootProject(file("../common"))

lazy val root = Project(id="web", base=file(".")).dependsOn(common).enablePlugins(PlayScala)

pipelineStages := Seq(uglify, digest, gzip)

scalaVersion := "2.10.4"

resolvers ++= Seq(
	"rediscala" at "http://dl.bintray.com/etaty/maven",
	"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  filters,
	"com.etaty.rediscala" %% "rediscala" % "1.3.1",
	"javax.mail" % "mail" % "1.4.7"
)

LessKeys.compress in Assets := true

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
