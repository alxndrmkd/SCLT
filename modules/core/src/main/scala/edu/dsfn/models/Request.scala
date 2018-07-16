package edu.dsfn.models

import cats.Show
import com.wavesplatform.wavesj.{PrivateKeyAccount, Transaction}

sealed trait Request

object Request {
  final case class SetScriptRequest(account: PrivateKeyAccount, script: String)                  extends Request
  final case class TransferRequest(from: PrivateKeyAccount, to: PrivateKeyAccount, amount: Long) extends Request
  final case class BroadcastRequest(tx: Transaction)                                             extends Request

  implicit val pp = new Show[Request] {
    override def show(t: Request): String = t match {
      case SetScriptRequest(account, script) => s"SetScript { account: $account, script: $script }"
      case TransferRequest(from, to, amount) => s" Transfer { from: $from, to: $to, amount: $amount }"
      case BroadcastRequest(tx)              => s"Broadcast { ${tx.getJson} }"
    }
  }
}
