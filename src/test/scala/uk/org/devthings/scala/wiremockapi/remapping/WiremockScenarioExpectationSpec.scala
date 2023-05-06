package uk.org.devthings.scala.wiremockapi.remapping

import io.circe.Json
import uk.org.devthings.scala.wiremockapi.BaseSpec

class WiremockScenarioExpectationSpec extends BaseSpec {

  import sttp.client3._

  before {
    reset()
  }

  // https://github.com/wiremock/wiremock/issues/456  (parallel request in scenarios (stateful) - race condition)
  // So I cannot really test what I want, 2 parallel identical calls returning different results concatenated
  // into a get request path
  "scenario" should {
    import WiremockExpectation.ops._

    "handle sequential calls to an api so we can mimic things like retrying" in {

      val (expectedJsonResponseBody1, expectation1) = generateAnyExpectation(0)
      val (expectedJsonResponseBody2, expectation2) = generateAnyExpectation(1)
      val (expectedJsonResponseBody3, expectation3) = generateAnyExpectation(2)

      List(
        expectation1,
        expectation2,
        expectation3
      ).applyAsScenario(wireMock.server)

      val request = basicRequest.get(uri"${wireMock.baseRequestUrl}/")
      val response1 = request.send(sttpBackend)
      val response2 = request.send(sttpBackend)
      val response3 = request.send(sttpBackend)

      response1.code.code shouldBe 200
      response2.code.code shouldBe 201
      response3.code.code shouldBe 202

      response1.body.flatMap(json.parse) shouldBe Right(expectedJsonResponseBody1)
      response2.body.flatMap(json.parse) shouldBe Right(expectedJsonResponseBody2)
      response3.body.flatMap(json.parse) shouldBe Right(expectedJsonResponseBody3)

    }

    def generateAnyExpectation(index: Int): (Json, WiremockExpectation) = {
      // language=json
      val jsonResponseBody = json.unsafeParse(s"""
          |{
          |   "body" :  "body-$index"
          |}
          |""".stripMargin)

      jsonResponseBody -> WiremockExpectation.default
        .withResponse(
          WiremockResponse.emptySuccess
            .withStatus(200 + index)
            .withResponseBody(jsonResponseBody.spaces2.asJsonResponse)
        )
    }
  }

}
