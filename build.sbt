import sbt.Package.ManifestAttributes
import sbtassembly.AssemblyKeys.assemblyShadeRules

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion     := "3.1.0"
ThisBuild / organization     := "uk.co.danielrendall"
ThisBuild / organizationName := "xslt-as-a-service"

githubOwner := "danielrendall"
githubRepository := "XsltAsAService"
githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

lazy val root = (project in file("."))
  .settings(
    name := "xslt-as-a-service",
    libraryDependencies ++= Seq(
      "uk.co.danielrendall" %% "services-as-a-service-interfaces" % "0.0.1",
      "net.sf.saxon" % "Saxon-HE" % "10.5",
      "org.nanohttpd" % "nanohttpd" % "2.3.1" % Provided
    ),
    packageOptions := Seq(ManifestAttributes(
      ("Serviceable-Class", "uk.co.danielrendall.saas.xslt.XsltService"))),
    assemblyShadeRules := Seq(
      ShadeRule.zap("scala.**").inLibrary("org.scala-lang" % "scala3-library" % "3.1.0")
    )
  )
