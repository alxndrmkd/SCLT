package edu.dsfn.models

import com.wavesplatform.wavesj.PrivateKeyAccount

sealed trait Request

object Request {
  final case class SetScriptRequest(account: PrivateKeyAccount, script: String)                  extends Request
  final case class TransferRequest(from: PrivateKeyAccount, to: PrivateKeyAccount, amount: Long) extends Request
}
