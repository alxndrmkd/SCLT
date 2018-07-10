package edu.dsfn

object MultiSigScriptGenerator {
  def apply(accounts: List[String], num: Int): String = {
    val accountsWithIndexes = accounts zipWithIndex
    val keyLets =
      accountsWithIndexes map {
        case (acc, i) =>
          s"let accountPK$i = base58'$acc'"
      } mkString "\n"

    val signedLets =
      accountsWithIndexes map {
        case (_, i) =>
          s"let accountSigned$i = if(sigVerify(tx.bodyBytes, tx.proofs[$i], accountPK$i)) then 1 else 0"
      } mkString "\n"

    val proofSum = accountsWithIndexes map {
      case (_, ind) =>
        s"accountSigned$ind"
    } mkString ("let proofSum = ", " + ", "")

    val finalStatement = s"proofSum >= $num"

    s"""
       |$keyLets
       |
       |$signedLets
       |
       |$proofSum
       |
       |$finalStatement
     """.stripMargin
  }
}
