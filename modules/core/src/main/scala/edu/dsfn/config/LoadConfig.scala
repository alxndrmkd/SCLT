package edu.dsfn.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

import scala.concurrent.duration.FiniteDuration

final case class LoadConfig(interval: FiniteDuration,
                            initial: Int Refined Positive,
                            step: Int Refined Positive,
                            `final`: Int Refined Positive)
