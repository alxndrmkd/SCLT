package edu.dsfn

import cats.effect
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.wavesplatform.wavesj.{Base58, Node, PrivateKeyAccount}
import edu.dsfn.config.{AppConfig, LoadConfig}
import edu.dsfn.loader.NodeApi
import edu.dsfn.models.Request.{SetScriptRequest, TransferRequest}
import edu.dsfn.models.{MultiSigPrecondition, Request}
import edu.dsfn.utils.{Chain, SeedWords, _}
import eu.timepit.refined.pureconfig.refTypeConfigConvert
import fs2._
import pureconfig.module.catseffect._

import scala.util.Random

object Main extends IOApp {

  def createAccount(seed: String = Random.shuffle(SeedWords.words).take(16).mkString): PrivateKeyAccount = {
    PrivateKeyAccount
      .fromSeed(
        seed,
        0,
        Chain.id
      )
  }

  def run(args: List[String]): IO[effect.ExitCode] = {

    val configF =
      Stream
        .eval(loadConfigF[IO, AppConfig]("sclt"))


    val nodePool =
      configF
        .map(conf => conf.network.nodes.toSeq)
        .flatMap(urls => {
          val nodes = urls.map(url => NodeApi[IO](new Node(url.value)))
          Stream.emits(nodes)
        })
        .repeat

    val multiSigPrecons =
      configF
        .flatMap(conf => Stream.emits(conf.accounts.toSeq))
        .map(msc => {
          val accounts = List.fill(msc.of.value + 1)(createAccount())

          println(s"${msc.num.value} of ${msc.of.value}")

          MultiSigPrecondition(accounts.head, accounts.tail.toSet, msc.num.value)
        })

    val initialTransfers =
      configF
        .map(conf => createAccount(conf.faucetSeed.value))
        .repeat
        .zip(multiSigPrecons.map(_.account))
        .map { case (sender, recipient) => TransferRequest(sender, recipient, 10000.waves) }

    val scripts =
      multiSigPrecons
        .map { msp =>
          val ownerPKs = msp.owners.map(o => Base58.encode(o.getPublicKey)).toList
          val script = MultiSigScriptGenerator(ownerPKs, msp.proofCount)

          SetScriptRequest(msp.account, script)
        }

    def loadTransfers(msp: MultiSigPrecondition, txCount: Int): Stream[IO, Request] = {
      val senders: Stream[IO, PrivateKeyAccount] =
        Stream
          .emits(msp.owners.toSeq.reverse)
          .repeat
      val recipients: Stream[IO, PrivateKeyAccount] =
        Stream
          .emits(msp.owners.toSeq)
          .repeat

      (senders zip recipients)
        .map { case (sender, recipient) =>
            TransferRequest(sender, recipient, 1.waves)
        }.take(txCount)

    }

    ((initialTransfers ++ scripts) zip nodePool)
        .evalMap { case (req, node) => node.runRequest(req) }
        .compile
        .drain
        .map(_ => ExitCode.Success)
  }
}
