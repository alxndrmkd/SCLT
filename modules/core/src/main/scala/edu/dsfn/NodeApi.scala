package edu.dsfn

import cats.effect.Async
import cats.implicits._
import com.wavesplatform.wavesj.{Node, PrivateKeyAccount}
import edu.dsfn.models.Request
import edu.dsfn.models.Request.{SetScriptRequest, TransferRequest}
import edu.dsfn.utils.{Chain, Fee}

final case class NodeApi[F[_]: Async](node: Node) {
  private def setScript(script: String, account: PrivateKeyAccount): F[Unit] = {
    Async[F].delay {
      node.setScript(account, script, Chain.id, Fee.setScript)
    }
  }
  private def transfer(from: PrivateKeyAccount, to: String, amount: Long): F[Unit] = {
    Async[F].delay {
      node.transfer(from, to, amount, Fee.transfer, "load testing")
    }
  }

  def runRequest(req: Request): F[Unit] = {
    req match {
      case SetScriptRequest(account, script) =>
        for {
          compiled <- Async[F].delay { node.compileScript(script) }
          _ <- setScript(compiled, account)
        } yield ()
      case TransferRequest(from, to, amount) => transfer(from, to.getAddress, amount)
    }
  }

  private def compile(src: String): F[String] = {
    Async[F].delay {
      node.compileScript(src)
    }
  }
}
