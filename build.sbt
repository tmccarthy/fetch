import au.id.tmm.sbt.DependencySettings

ThisBuild / sonatypeProfile := "au.id.tmm"
ThisBuild / baseProjectName := "fetch"
ThisBuild / githubProjectName := "fetch"
ThisBuild / githubWorkflowJavaVersions := List("adopt@1.11")

val circeVersion          = "0.15.0-M1"
val awsSdkVersion         = "2.15.33"
val tmmUtilsVersion       = "0.9.1"
val tmmCollectionsVersion = "0.0.5"
val fs2Version            = "3.2.7"
val sttpVersion           = "3.5.2"
val catsEffectVersion     = "3.2.9"
val slf4jVersion          = "2.0.0-alpha1"

lazy val root = project
  .in(file("."))
  .settings(settingsForRootProject)
  .settings(console := (core / Compile / console).value)
  .aggregate(
    core,
    cache,
    awsTextract,
  )

lazy val core = project
  .in(file("core"))
  .settings(settingsForSubprojectCalled("core"))
  .settings(
    libraryDependencies += "au.id.tmm.tmm-utils"           %% "tmm-utils-cats"                 % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"           %% "tmm-utils-errors"               % tmmUtilsVersion,
    libraryDependencies += "org.typelevel"                 %% "cats-effect"                    % catsEffectVersion,
    libraryDependencies += "co.fs2"                        %% "fs2-core"                       % fs2Version,
    libraryDependencies += "co.fs2"                        %% "fs2-io"                         % fs2Version,
    libraryDependencies += "com.github.tototoshi"          %% "scala-csv"                      % "1.3.10",
    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpVersion,
    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2"  % sttpVersion,
    libraryDependencies += "io.github.resilience4j"         % "resilience4j-ratelimiter"       % "1.7.1",
    libraryDependencies += "commons-io"                     % "commons-io"                     % "2.11.0",
    libraryDependencies += "io.circe"                      %% "circe-core"                     % circeVersion,
    libraryDependencies += "io.circe"                      %% "circe-parser"                   % circeVersion,
    libraryDependencies += "com.softwaremill.sttp.client3" %% "circe"                          % sttpVersion,
    libraryDependencies += "org.slf4j"                      % "slf4j-simple"                   % slf4jVersion % Runtime,
  )

lazy val cache = project
  .in(file("cache"))
  .settings(settingsForSubprojectCalled("cache"))
  .dependsOn(core)
  .settings(
    libraryDependencies += "au.id.tmm.scala-db" %% "scala-db-core"       % "0.0.1",
    libraryDependencies += "org.slf4j"           % "slf4j-simple"        % slf4jVersion % Runtime,
    libraryDependencies += "org.xerial"          % "sqlite-jdbc"         % "3.36.0.3"   % Test,
    libraryDependencies += "org.typelevel"      %% "munit-cats-effect-3" % "1.0.5"      % Test,
    libraryDependencies += "org.scalameta"      %% "munit-scalacheck"    % "0.7.29"     % Test,
  )

lazy val awsTextract = project
  .in(file("aws-textract"))
  .settings(settingsForSubprojectCalled("aws-textract"))
  .dependsOn(cache) // TODO this dependency tree brings in way too much given what is needed. Might need reconsidering
  .settings(
    resolvers += "aws-dynamodb-local" at "https://s3-us-west-2.amazonaws.com/dynamodb-local/release/",
  )
  .settings(
    libraryDependencies += "org.typelevel"                   %% "cats-effect"                    % catsEffectVersion,
    libraryDependencies += "co.fs2"                          %% "fs2-core"                       % fs2Version,
    libraryDependencies += "au.id.tmm.digest4s"              %% "digest4s-core"                  % "0.0.1",
    libraryDependencies += "au.id.tmm.tmm-scala-collections" %% "tmm-scala-collections-core"     % tmmCollectionsVersion,
    libraryDependencies += "au.id.tmm.tmm-scala-collections" %% "tmm-scala-collections-cats"     % tmmCollectionsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-syntax"               % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-errors"               % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-cats"                 % tmmUtilsVersion,
    libraryDependencies += "com.softwaremill.sttp.client3"   %% "core"                           % sttpVersion,
    libraryDependencies += "com.softwaremill.sttp.client3"   %% "async-http-client-backend-cats" % sttpVersion,
    libraryDependencies += "software.amazon.awssdk"           % "textract"                       % awsSdkVersion,
    libraryDependencies += "software.amazon.awssdk"           % "s3"                             % awsSdkVersion,
    libraryDependencies += "software.amazon.awssdk"           % "dynamodb"                       % awsSdkVersion,
    libraryDependencies += "me.xdrop"                         % "fuzzywuzzy"                     % "1.3.1",
    libraryDependencies += "org.slf4j"                        % "slf4j-api"                      % slf4jVersion,
    libraryDependencies += "org.slf4j"                        % "slf4j-simple"                   % slf4jVersion % Runtime,
    libraryDependencies += "app.cash.tempest"                 % "tempest2-testing-jvm"           % "1.5.2",
//    libraryDependencies += "com.amazonaws"                    % "DynamoDBLocal"                  % "1.15.0"     % Test,
    libraryDependencies += "org.typelevel"                   %% "munit-cats-effect-3"            % "1.0.5"      % Test,
  )
