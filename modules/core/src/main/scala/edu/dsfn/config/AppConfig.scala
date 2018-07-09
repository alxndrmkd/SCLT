package edu.dsfn.config

final case class AppConfig(accounts: Set[MultiSigConfig],
                           load: LoadConfig,
                           network: NetworkConfig)
