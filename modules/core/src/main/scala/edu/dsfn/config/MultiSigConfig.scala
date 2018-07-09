package edu.dsfn.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._

final case class MultiSigConfig(num: Int Refined Positive, of: Int Refined Positive) {
  require(num <= of)
}
