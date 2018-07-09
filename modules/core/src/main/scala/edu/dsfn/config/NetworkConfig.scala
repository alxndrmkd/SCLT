package edu.dsfn.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url

final case class NetworkConfig(nodes: Set[String Refined Url])
