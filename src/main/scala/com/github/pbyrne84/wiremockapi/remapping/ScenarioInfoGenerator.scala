package com.github.pbyrne84.wiremockapi.remapping

import com.github.tomakehurst.wiremock.stubbing.Scenario

object ScenarioInfoGenerator {

  /**
    * Scenarios may get more complicated so a custom ScenarioInfoGenerator can be used. For example on the last index
    * maybe we want to set the state to start or something.
    *
    * The state stuff does not seem very async friendly unless the call that changes the nextState is the one after a
    * set of async instructions
    */
  val default: ScenarioInfoGenerator = new ScenarioInfoGenerator {
    override val scenarioName: String = "auto-generated-scenario"

    def createScenarioInfo(scenarioName: String, index: Int, maxIndex: Int): ScenarioInfo = {
      if (index == 0) {
        ScenarioInfo(scenarioName = scenarioName, expectedCurrentState = Scenario.STARTED, nextState = createName(1))
      } else {
        ScenarioInfo(
          scenarioName = scenarioName,
          expectedCurrentState = createName(index),
          nextState = createName(index + 1)
        )
      }
    }

    private def createName(index: Int): String =
      s"wiremock-step-$index"

  }
}

trait ScenarioInfoGenerator {
  val scenarioName: String
  def createScenarioInfo(scenarioName: String, index: Int, maxIndex: Int): ScenarioInfo
}
