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
    dbLib,
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

lazy val dbLib = project
  .in(file("db-lib"))
  .settings(settingsForSubprojectCalled("db-lib"))
  .settings(
    libraryDependencies += "org.typelevel"       %% "cats-effect"         % catsEffectVersion,
    libraryDependencies += "org.postgresql"       % "postgresql"          % "42.2.5",
    libraryDependencies += "co.fs2"              %% "fs2-core"            % fs2Version,
    libraryDependencies += "com.zaxxer"           % "HikariCP"            % "5.0.1",
    libraryDependencies += "au.id.tmm.tmm-utils" %% "tmm-utils-cats"      % tmmUtilsVersion, // TODO this might be excessive when this is its own library
    libraryDependencies += "org.slf4j"            % "slf4j-simple"        % slf4jVersion % Runtime,
    libraryDependencies += "org.xerial"           % "sqlite-jdbc"         % "3.36.0.3"   % Test,
    libraryDependencies += "org.typelevel"       %% "munit-cats-effect-3" % "1.0.5"      % Test,
  )

lazy val awsTextract = project
  .in(file("aws-textract"))
  .settings(settingsForSubprojectCalled("aws-textract"))
  .settings(
    libraryDependencies += "org.typelevel"                   %% "cats-effect"                    % "2.2.0",
    libraryDependencies += "co.fs2"                          %% "fs2-core"                       % "2.5.0",
    libraryDependencies += "au.id.tmm.digest4s"              %% "digest4s-core"                  % "0.0.1",
    libraryDependencies += "au.id.tmm.tmm-scala-collections" %% "tmm-scala-collections-core"     % tmmCollectionsVersion,
    libraryDependencies += "au.id.tmm.tmm-scala-collections" %% "tmm-scala-collections-cats"     % tmmCollectionsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-syntax"               % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-errors"               % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.tmm-utils"             %% "tmm-utils-cats"                 % tmmUtilsVersion,
    libraryDependencies += "au.id.tmm.digest4s"              %% "digest4s-core"                  % "0.0.1",
    libraryDependencies += "com.softwaremill.sttp.client3"   %% "core"                           % "3.0.0-RC7",
    libraryDependencies += "com.softwaremill.sttp.client3"   %% "async-http-client-backend-cats" % "3.0.0-RC7",
    libraryDependencies += "software.amazon.awssdk"           % "textract"                       % "2.15.33",
    libraryDependencies += "software.amazon.awssdk"           % "s3"                             % "2.15.33",
    libraryDependencies += "software.amazon.awssdk"           % "dynamodb"                       % "2.15.33",
    libraryDependencies += "me.xdrop"                         % "fuzzywuzzy"                     % "1.3.1",
    libraryDependencies += "org.slf4j"                        % "slf4j-api"                      % "1.7.30",
    libraryDependencies += "org.slf4j"                        % "slf4j-simple"                   % "1.7.30" % Runtime,
  )
