package com.github.pbyrne84.wiremockapi.remapping

import com.github.pbyrne84.wiremockapi.BaseSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.prop.TableDrivenPropertyChecks
import sttp.model.StatusCode
import org.scalatest.prop.TableDrivenPropertyChecks
import sttp.model.{Header, StatusCode}
class WiremockResponseSpec extends BaseSpec with TableDrivenPropertyChecks {

  before {
    reset()
  }

  // Some people like checkSameElementAs, I don't, fails like a question in a holiday puzzle book
  private val defaultWireMockHeaderNames = List(
    "transfer-encoding",
    "content-encoding",
    "server",
    "matched-stub-id",
    "vary"
  ).sorted

  "WireMockResponse" should {
    import sttp.client3._

    "return a 200 with empty body for a default constructor" in {
      val defaultConstructor = WiremockResponse()
      val emptySuccess = WiremockResponse.emptySuccess

      val options = Table(
        ("type", "value"),
        ("defaultConstructor", defaultConstructor),
        ("emptySuccess", emptySuccess)
      )

      forAll(options) { (_, value) =>
        wireMock.stubAnyRequestResponse(value.asWireMock)
        val request = basicRequest.get(uri"${wireMock.baseRequestUrl}/")

        val response = request.send(sttpBackend)

        // Some people like checkSameElementAs, I don't, fails like a question in a holiday puzzle book
        response.headers.map(_.name).sorted shouldBe defaultWireMockHeaderNames

        response.code shouldBe StatusCode.Ok
        response.body shouldBe Right("")

      }
    }

    "return a json response" in {
      val statusCodes = Table("status-code", 200, 201, 202)
      // language=json
      val jsonContent =
        """{
          | "a" : "value"
          |}""".stripMargin

      val wirMockResponse = WiremockResponse()
        .withResponseBody(
          JsonBody(
            jsonContent
          )
        )

      forAll(statusCodes) { statusCode =>
        wireMock.reset()
        wireMock.stubAnyRequestResponse(wirMockResponse.withStatus(statusCode).asWireMock)

        val request = basicRequest.get(uri"${wireMock.baseRequestUrl}/")
        val response = request.send(sttpBackend)

        response.headers.nonDefaultHeaders shouldBe List(Header("content-type", "application/json"))
        response.code.code shouldBe statusCode
        response.body shouldBe Right(jsonContent)
      }
    }
  }
}
