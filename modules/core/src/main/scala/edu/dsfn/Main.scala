package edu.dsfn

import cats.effect
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.wavesplatform.wavesj.{Base58, Node, PrivateKeyAccount}
import edu.dsfn.config.{AppConfig, LoadConfig}
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

  final case class LoaderState(config: LoadConfig, counter: Int, requestAccumulator: Stream[IO, Request], finished: Boolean) {
    def next(msp: MultiSigPrecondition): IO[LoaderState] = {
      val txCount  = config.initial.value + counter * config.step.value
      val requests = mkLoadTransfers(msp, txCount)

      IO.sleep(config.interval) *> IO {
        copy(
          counter = counter + 1,
          requestAccumulator = requestAccumulator ++ requests,
          finished = txCount >= config.`final`.value
        )
      }
    }

    private def mkLoadTransfers(msp: MultiSigPrecondition, txCount: Int): Stream[IO, Request] = {
      val senders: Stream[IO, PrivateKeyAccount] =
        Stream
          .emits(msp.owners.toSeq.reverse)
          .repeat

      val recipients: Stream[IO, PrivateKeyAccount] =
        Stream
          .emits(msp.owners.toSeq)
          .repeat

      (senders zip recipients)
        .map {
          case (sender, recipient) =>
            TransferRequest(sender, recipient, 1.waves)
        }
        .take(txCount)

    }
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
          val script   = MultiSigScriptGenerator(ownerPKs, msp.proofCount)

          SetScriptRequest(msp.account, script)
        }

    val transfers =
      configF
        .map(_.load)
        .flatMap(loadConfig => {
          val initialState: LoaderState = LoaderState(loadConfig, 0, Stream.empty, false)

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

    ((initialTransfers ++ scripts ++ transfers) zip nodePool)
      .evalMap { case (req, node) => IO { println(req) }/* node.runRequest(req) */ }
      .compile
      .drain
      .map(_ => ExitCode.Success)
  }
}
