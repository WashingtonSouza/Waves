package scorex.settings

import scorex.consensus._

/*
  System constants here.
 */

object Constants {

  val Product = "Scorex"
  val Release = "Lagonaki"
  val Version = (0, 0, 3)

  val AgentName = s"$Product - $Release v. ${Version._1}.${Version._2}.${Version._3}"

  //Change to ConsensusModuleNxt to have Nxt-like forging
  val ConsensusAlgo: ConsensusModule = ConsensusModuleNxt
}