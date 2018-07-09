package edu.dsfn.models

import com.wavesplatform.wavesj.PrivateKeyAccount

final case class MultiSigPrecondition(account: PrivateKeyAccount,
                                      owners: Set[PrivateKeyAccount],
                                      proofCount: Int)
