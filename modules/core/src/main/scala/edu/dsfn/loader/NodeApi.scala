package edu.dsfn.loader

import cats.effect.Async
import com.wavesplatform.wavesj.{Node, PrivateKeyAccount}
import edu.dsfn.utils.{Chain, Fee}

final case class NodeApi[F[_]: Async](node: Node) {
  def setScript(script: String, account: PrivateKeyAccount): F[Unit] = {
    Async[F].delay {
      node.setScript(account, script, Chain.id, Fee.setScript)
    }
  }
  def transfer(from: PrivateKeyAccount, to: String, amount: Long): F[Unit] = {
    Async[F].delay {
      node.transfer(from, to, amount, Fee.transfer, "load testing")
    }
  }
}
