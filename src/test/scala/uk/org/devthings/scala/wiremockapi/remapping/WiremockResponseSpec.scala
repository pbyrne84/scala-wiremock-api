package uk.org.devthings.scala.wiremockapi.remapping

import org.scalatest.prop.TableDrivenPropertyChecks
import sttp.model.{Header, StatusCode}
import uk.org.devthings.scala.wiremockapi.BaseSpec
class WiremockResponseSpec extends BaseSpec with TableDrivenPropertyChecks {

  before {
    reset()
  }

  private val defaultWireMockHeaderNames = List(
    "transfer-encoding",
    "content-encoding",
    "server",
    "matched-stub-id",
    "vary",
    ":status"
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

        response.headers
          .filterNot(header => defaultWireMockHeaderNames.contains(header.name)) shouldBe List.empty

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
          JsonResponseBody(
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
