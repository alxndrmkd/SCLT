package edu.dsfn

import cats.effect
import cats.effect.{ExitCode, IO, IOApp}
import com.wavesplatform.wavesj._
import edu.dsfn.config.AppConfig
import edu.dsfn.models.Request.{SetScriptRequest, TransferRequest}
import edu.dsfn.models.{LoaderState, MultiSigPrecondition}
import edu.dsfn.utils.{Chain, SeedWords, _}
import eu.timepit.refined.pureconfig.refTypeConfigConvert
import fs2._
import pureconfig.module.catseffect._
import cats.implicits._

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

    val multiSigPrecons =
      configF
        .flatMap(conf => Stream.emits(conf.accounts.toSeq))
        .map(msc => {
          val accounts = List.fill(msc.of.value + 1)(createAccount())

          MultiSigPrecondition(accounts.head, accounts.tail.toSet, msc.num.value)
        })

    val initialTransfers =
      configF
        .map(conf => PrivateKeyAccount.fromPrivateKey(conf.faucetPrivateKey.value, Chain.id))
        .flatMap { faucet =>
          multiSigPrecons
            .map(_.account)
            .map { recipient =>
              TransferRequest(faucet, recipient, 10000.waves)
            }
        }

    val scripts =
      multiSigPrecons
        .map { msp =>
          val ownerPKs = msp.owners.map(o => Base58.encode(o.getPublicKey)).toList
          val script   = MultiSigScriptGenerator(ownerPKs, msp.proofCount)

          SetScriptRequest(msp.account, script)
        }

    val transfers =
      configF
        .map(_.load)
        .flatMap(loadConfig => {
          val initialState = LoaderState(loadConfig, 0, Stream.empty, false)

          multiSigPrecons
            .flatMap { msp =>
              Stream
                .emit(msp)
                .repeat
                .evalScan(initialState) { case (state, prec) => state.next(prec) }
                .takeWhile(st => !st.finished)
                .flatMap(_.requestAccumulator)
            }
        })

    ((initialTransfers ++ scripts ++ transfers) zip nodePool.repeat)
      .evalMap { case (req, node) => node.runRequest(req) }
      .compile
      .drain
      .map(_ => ExitCode.Success)
  }
}
