package edu.dsfn

package object utils {

  implicit class doubleOps(a: Double) {
    def waves = (a * 100000000l).toLong
  }

}
