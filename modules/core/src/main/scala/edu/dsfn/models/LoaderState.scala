package edu.dsfn.models

import cats.effect.{IO, Timer}
import com.wavesplatform.wavesj.PrivateKeyAccount
import edu.dsfn.config.LoadConfig
import edu.dsfn.models.Request.TransferRequest
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
