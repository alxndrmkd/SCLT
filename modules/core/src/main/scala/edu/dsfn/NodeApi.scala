package edu.dsfn

import cats.effect.Sync
import cats.implicits._
import com.wavesplatform.wavesj.{Node, PrivateKeyAccount, Transaction}
import edu.dsfn.models.Request
import edu.dsfn.models.Request.{BroadcastRequest, SetScriptRequest, TransferRequest}
import edu.dsfn.utils.{Chain, Fee}

final case class NodeApi[F[_]: Sync](node: Node) {
  private def setScript(script: String, account: PrivateKeyAccount): F[Unit] = {
    Sync[F].delay {
      node.setScript(account, script, Chain.id, Fee.setScript)
    }
  }
  private def transfer(from: PrivateKeyAccount, to: String, amount: Long): F[Unit] = {
    Sync[F].delay {
      node.transfer(from, to, amount, Fee.transfer, "load testing")
    }
  }

  private def broadcast(tx: Transaction): F[Unit] = {
    Sync[F].delay {
      node.send(tx)
    }
  }

  def runRequest(req: Request): F[Unit] = {
    req match {
      case SetScriptRequest(account, script) => log(s"setting script $script on $account") *> setScript(script, account)
      case TransferRequest(from, to, amount) => log(s"transfering $amount from $from to $to") *> transfer(from, to.getAddress, amount)
      case BroadcastRequest(tx)              => log(s"broadcasting $tx") *> broadcast(tx)
    }
  }

  private def compile(src: String): F[String] = {
    Sync[F].delay {
      node.compileScript(src)
    }
  }

  private def log(msg: String): F[Unit] = Sync[F].delay { println(msg) }
}
