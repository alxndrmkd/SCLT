import sbt._

object Dependencies {

  lazy val cats = {
    "org.typelevel" %% "cats-core" % Version.cats
  }

  lazy val mtl = {
    "org.typelevel" %% "cats-mtl-core" % Version.mtl
  }

  lazy val effect = {
    "org.typelevel" %% "cats-effect" % Version.effect
  }

  lazy val fs2 = {
    "co.fs2" %% "fs2-core" % Version.fs2
  }

  lazy val refined = Seq(
    "refined",
    "refined-cats",
    "refined-pureconfig"
  ) map ("eu.timepit" %% _ % Version.refined)

  lazy val pureConfig = Seq(
    "pureconfig",
    "pureconfig-cats-effect"
  ) map ("com.github.pureconfig" %% _ % Version.pureConfig)

  lazy val wavesJ = "com.wavesplatform" % "wavesj" % Version.wavesJ

}
