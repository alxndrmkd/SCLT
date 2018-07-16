package edu.dsfn.config

import eu.timepit.refined.types.string.NonEmptyString

final case class AppConfig(faucetPrivateKey: NonEmptyString, accounts: Set[MultiSigConfig], load: LoadConfig, network: NetworkConfig)
