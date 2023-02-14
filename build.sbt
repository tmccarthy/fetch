name := "fetch"

ThisBuild / tlBaseVersion := "0.6"

Sonatype.SonatypeKeys.sonatypeProfileName := "au.id.tmm"
ThisBuild / organization := "au.id.tmm.fetch"
ThisBuild / organizationName := "Timothy McCarthy"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("tmccarthy", "Timothy McCarthy"),
)

val Scala213 = "2.13.8"
ThisBuild / scalaVersion := Scala213
ThisBuild / crossScalaVersions := Seq(
  Scala213,
  "3.2.1",
)

ThisBuild / githubWorkflowJavaVersions := List(
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
)

ThisBuild / tlCiHeaderCheck := false
ThisBuild / tlCiScalafmtCheck := true
ThisBuild / tlCiMimaBinaryIssueCheck := false
ThisBuild / tlFatalWarnings := true

addCommandAlias("check", ";githubWorkflowCheck;scalafmtSbtCheck;+scalafmtCheckAll;+test;+IntegrationTest/test")
addCommandAlias("fix", ";githubWorkflowGenerate;+scalafmtSbt;+scalafmtAll")

val circeVersion          = "0.15.0-M1"
val awsSdkVersion         = "2.20.3"
val tmmUtilsVersion       = "0.10.0"
val tmmCollectionsVersion = "0.2.0"
val fs2Version            = "3.6.1"
val sttpVersion           = "3.8.11"
val catsEffectVersion     = "3.4.6"
val slf4jVersion          = "2.0.5"
val mUnitVersion          = "0.7.29"

lazy val root = tlCrossRootProject.aggregate(core, cache, aws, awsTextract, awsDynamodb)

lazy val core = project
  .in(file("core"))
  .settings(name := "fetch-core")
  .settings(
    libraryDependencies += "au.id.tmm.tmm-utils"           %% "tmm-utils-cats"                 % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"           %% "tmm-utils-errors"               % tmmUtilsVersion,
    libraryDependencies += "org.typelevel"                 %% "cats-effect"                    % catsEffectVersion,
    libraryDependencies += "co.fs2"                        %% "fs2-core"                       % fs2Version,
    libraryDependencies += "co.fs2"                        %% "fs2-io"                         % fs2Version,
    libraryDependencies += "com.github.tototoshi"          %% "scala-csv"                      % "1.3.10",
    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpVersion,
    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2"  % sttpVersion,
    libraryDependencies += "io.github.resilience4j"         % "resilience4j-ratelimiter"       % "2.0.2",
    libraryDependencies += "commons-io"                     % "commons-io"                     % "2.11.0",
    libraryDependencies += "io.circe"                      %% "circe-core"                     % circeVersion,
    libraryDependencies += "io.circe"                      %% "circe-parser"                   % circeVersion,
    libraryDependencies += "com.softwaremill.sttp.client3" %% "circe"                          % sttpVersion,
    libraryDependencies += "org.slf4j"                      % "slf4j-simple"                   % slf4jVersion % Runtime,
  )
  .settings(
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies += "org.scalameta" %% "munit" % mUnitVersion % Test,
  )

lazy val cache = project
  .in(file("cache"))
  .settings(name := "fetch-cache")
  .dependsOn(core)
  .settings(
    libraryDependencies += "au.id.tmm.scala-db" %% "scala-db-core" % "0.1.0",
    libraryDependencies += "org.slf4j"           % "slf4j-simple"  % slf4jVersion % Runtime,
  )
  .settings(
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies += "org.scalameta" %% "munit"               % mUnitVersion % Test,
    libraryDependencies += "org.xerial"     % "sqlite-jdbc"         % "3.40.1.0"   % Test,
    libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.7"      % Test,
    libraryDependencies += "org.scalameta" %% "munit-scalacheck"    % "0.7.29"     % Test,
  )

lazy val aws = project
  .in(file("aws/core"))
  .settings(name := "fetch-aws")
  .dependsOn(cache) // TODO this dependency tree brings in way too much given what is needed. Might need reconsidering
  .settings(
    // Crashes in 3.x
    Compile / doc / sources := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, 0 | 1 | 2)) => Nil
      case Some(_) | None       => (Compile / doc / sources).value
    }),
  )
  .settings(
    libraryDependencies += "au.id.tmm.digest4s"              %% "digest4s-core"              % "1.0.0",
    libraryDependencies += "au.id.tmm.tmm-scala-collections" %% "tmm-scala-collections-core" % tmmCollectionsVersion,
    libraryDependencies += "au.id.tmm.tmm-scala-collections" %% "tmm-scala-collections-cats" % tmmCollectionsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-syntax"           % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-circe"            % tmmUtilsVersion,
    libraryDependencies += "software.amazon.awssdk"           % "s3"                         % awsSdkVersion,
    libraryDependencies += "org.slf4j"                        % "slf4j-api"                  % slf4jVersion,
    libraryDependencies += "org.slf4j"                        % "slf4j-simple"               % slf4jVersion % Runtime,
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / parallelExecution := false,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies += "org.scalameta"         %% "munit"                             % mUnitVersion % "it,test",
    libraryDependencies += "org.typelevel"         %% "munit-cats-effect-3"               % "1.0.7"      % "it,test",
    libraryDependencies += "com.github.docker-java" % "docker-java-core"                  % "3.2.14"     % "it",
    libraryDependencies += "com.github.docker-java" % "docker-java-transport-httpclient5" % "3.2.14"     % "it",
  )

lazy val awsTextract = project
  .in(file("aws/textract"))
  .settings(name := "fetch-aws-textract")
  .dependsOn(aws)
  .settings(
    // Crashes in 3.x
    Compile / doc / sources := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, 0 | 1 | 2)) => Nil
      case Some(_) | None       => (Compile / doc / sources).value
    }),
  )
  .settings(
    libraryDependencies += "software.amazon.awssdk" % "textract"   % awsSdkVersion,
    libraryDependencies += "me.xdrop"               % "fuzzywuzzy" % "1.4.0",
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / parallelExecution := false,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies += "org.scalameta"         %% "munit"                             % mUnitVersion % "it,test",
    libraryDependencies += "org.typelevel"         %% "munit-cats-effect-3"               % "1.0.7"      % "it,test",
    libraryDependencies += "com.github.docker-java" % "docker-java-core"                  % "3.2.14"     % "it",
    libraryDependencies += "com.github.docker-java" % "docker-java-transport-httpclient5" % "3.2.14"     % "it",
  )

lazy val awsDynamodb = project
  .in(file("aws/dynamodb"))
  .settings(name := "fetch-aws-dynamodb")
  .dependsOn(aws)
  .settings(
    // Crashes in 3.x
    Compile / doc / sources := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, 0 | 1 | 2)) => Nil
      case Some(_) | None       => (Compile / doc / sources).value
    }),
  )
  .settings(
    libraryDependencies += "software.amazon.awssdk" % "dynamodb" % awsSdkVersion,
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / parallelExecution := false,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies += "org.scalameta"         %% "munit"                             % mUnitVersion % "it,test",
    libraryDependencies += "org.typelevel"         %% "munit-cats-effect-3"               % "1.0.7"      % "it,test",
    libraryDependencies += "com.github.docker-java" % "docker-java-core"                  % "3.2.14"     % "it",
    libraryDependencies += "com.github.docker-java" % "docker-java-transport-httpclient5" % "3.2.14"     % "it",
  )

ThisBuild / githubWorkflowBuild += WorkflowStep.Sbt(List("+IntegrationTest/test"), name = Some("Integration test"))
