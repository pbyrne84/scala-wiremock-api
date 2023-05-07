package uk.org.devthings.scala.wiremockapi.remapping

import uk.org.devthings.scala.wiremockapi.BaseSpec
import uk.org.devthings.scala.wiremockapi.remapping.RequestMethod.Post

class LooseWireMockExpectationTightVerificationExampleSpec extends BaseSpec {

  before {
    reset()
  }

  // Simple example where 404's could lead to confusion.
  "Using a loose expectation and tight verification" should {
    import WireMockExpectation.ops._

    "create a test we can be sure is correct easily" in {

      // language=json
      val expectedBody1 =
        """
          |{"payload-1" : "value-1"}
          |""".stripMargin

      // language=json
      val expectedBody2 =
        """
          |{"payload-2" : "value-2"}
          |""".stripMargin

      // We do not care about header checks and body checks at this point, body checks are pretty troublesome
      // as people can use the string expectation instead of the json expectation meaning the expectation is not
      // json formatting safe.
      val firstCallLooseExpectation =
        WireMockExpectation.willRespondOk
          .expectsMethod(Post) // we could skip the method here and add it later but method is pretty hard to fail on
          .expectsUrl("/api-path-1".asUrlPathEquals)

      val secondCallLooseExpectation =
        WireMockExpectation.willRespondOk
          .expectsMethod(Post) // we could skip the method here and add it later but method is pretty hard to fail on
          .expectsUrl("/api-path-2".asUrlPathEquals)

      wireMock.stubExpectation(firstCallLooseExpectation)
      wireMock.stubExpectation(secondCallLooseExpectation)

      val result = for {
        _ <- callServer(path = "api-path-1", body = expectedBody1)
        _ <- callServer(path = "api-path-2", body = expectedBody2)
      } yield true

      // A None can ne cause by either call failing
      result shouldBe Some(true)

      import uk.org.devthings.scala.wiremockapi.remapping.WireMockExpectation.ops._

      val paramExpectation = ("param1" -> "paramValue1").asEqualTo

      wireMock.verify(
        firstCallLooseExpectation
          .expectsBody(BodyValueExpectation.equalsJson(expectedBody1))
          .expectsQueryParam(paramExpectation)
      )

      wireMock.verify(
        secondCallLooseExpectation
          .expectsBody(BodyValueExpectation.equalsJson(expectedBody2))
          .expectsQueryParam(paramExpectation)
      )

    }
  }

  /**
    * An auth check could just return a boolean/empty list etc. And auth checks can chain.
    *
    * @param path
    * @param body
    * @return
    */
  def callServer(path: String, body: String): Option[Boolean] = {
    import sttp.client3._
    val response = basicRequest
      .post(uri"${wireMock.baseRequestUrl}/$path?param1=paramValue1")
      .contentType("application/json") // this is auto-checked by BodyValueExpectation.equalsJson
      .body(body)
      .send(sttpBackend)

    if (response.code.code == 200) {
      Some(true)
    } else {
      None
    }
  }

}
