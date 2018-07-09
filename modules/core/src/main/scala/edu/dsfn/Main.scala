package edu.dsfn

import cats.effect.IO
import com.wavesplatform.wavesj.{Node, PrivateKeyAccount}
import edu.dsfn.config.AppConfig
import edu.dsfn.loader.NodeApi
import edu.dsfn.models.MultiSigPrecondition
import edu.dsfn.utils.SeedWords
import eu.timepit.refined.pureconfig.refTypeConfigConvert
import fs2._
import pureconfig.module.catseffect._

import scala.util.Random

object Main extends App {

  val configF =
    Stream
      .eval(loadConfigF[IO, AppConfig]("sclt"))

  def createAccount(): PrivateKeyAccount = {
    val seed = Random.shuffle(SeedWords.words).take(16).mkString

    PrivateKeyAccount
      .fromSeed(
        seed,
        0,
        'D'.toByte
      )
  }

  val nodePool =
    configF
      .map(conf => conf.network.nodes.toSeq)
      .flatMap(urls => {
        val nodes = urls.map(url => NodeApi[IO](new Node(url.value)))
        Stream.emits(nodes)
      }).repeat

  val multiSigPrecons =
    configF
      .flatMap(conf => Stream.emits(conf.accounts.toSeq))
      .map(msc => {
        val accounts = List.fill(msc.of.value)(createAccount())

        MultiSigPrecondition(accounts.head, accounts.tail.toSet, msc.of.value)
      })

}
