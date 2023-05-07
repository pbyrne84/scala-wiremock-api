package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.{ResponseDefinitionBuilder, WireMock}

object WireMockResponse {

  val statusOk: WireMockResponse = WireMockResponse()
  val statusNotFound: WireMockResponse = WireMockResponse(status = 404)
  val statusServerError: WireMockResponse = WireMockResponse(status = 500)

  // Utility just to make the choice easier hiding the magic number element
  def redirectResponse(redirectHeader: RedirectHeader): WireMockResponse = {
    WireMockResponse(status = redirectHeader.status)
      .withHeader("location", redirectHeader.uri.toString)
  }

}

case class WireMockResponse(
    status: Int = 200,
    headers: List[(String, List[String])] = List.empty,
    responseBody: ResponseBody = EmptyResponseBody
) {

  def withStatus(status: Int): WireMockResponse =
    copy(status = status)

  def withHeader(name: String, values: String*): WireMockResponse =
    copy(headers = headers :+ (name -> values.toList))

  def withResponseBody(responseBody: ResponseBody): WireMockResponse =
    copy(responseBody = responseBody)

  def asWireMock: ResponseDefinitionBuilder = {
    // Mutation party
    val builder = WireMock.aResponse().withStatus(status)

    responseBody match {
      case EmptyResponseBody =>
        ()

      case BinaryResponseBody(value) =>
        builder.withBody(value.toArray)

      case StringResponseBody(value) =>
        builder.withBody(value)

      case jsonBody: JsonResponseBody =>
        builder
          .withBody(jsonBody.value)
          .withHeader(jsonBody.jsonHeader._1, jsonBody.jsonHeader._2)
    }

    headers.foreach { case (name, values) =>
      builder.withHeader(name, values: _*)
    }

    builder
  }

}
