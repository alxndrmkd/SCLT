package edu.dsfn.models

import cats.effect.{IO, Timer}
import com.wavesplatform.wavesj.{Asset, PrivateKeyAccount, Transaction}
import edu.dsfn.config.LoadConfig
import edu.dsfn.models.Request.{BroadcastRequest, TransferRequest}
import fs2.Stream
import cats.implicits._
import edu.dsfn.utils._

final case class LoaderState(config: LoadConfig, counter: Int, requestAccumulator: Stream[IO, Request], finished: Boolean) {
  import LoaderState._

  def next(msp: MultiSigPrecondition)(implicit t: Timer[IO]): IO[LoaderState] = {
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

}

object LoaderState {
  private def mkLoadTransfers(msp: MultiSigPrecondition, txCount: Int): Stream[IO, Request] = {

    val sender  = msp.account
    val signers = msp.owners.take(msp.proofCount)

    val recipients: Stream[IO, PrivateKeyAccount] =
      Stream
        .emits(msp.owners.toSeq)
        .repeat

    recipients
      .map { recipient =>
        val tx = Transaction
          .makeTransferTx(sender, recipient.getAddress, 1.waves, Asset.WAVES, 0.5.waves, Asset.WAVES, "load test")

        val proofs = signers.toList.map(_.sign(tx))

        BroadcastRequest(sign(tx, proofs))
      }
      .take(txCount)

  }

  def sign(tx: Transaction, proofs: List[String]): Transaction = {
    def _sign(t: Transaction, p: List[String], c: Int): Transaction = {
      p match {
        case Nil     => t
        case x :: xs => _sign(t.withProof(c, x), xs, c + 1)
      }
    }

    _sign(tx, proofs, 0)
  }
}
