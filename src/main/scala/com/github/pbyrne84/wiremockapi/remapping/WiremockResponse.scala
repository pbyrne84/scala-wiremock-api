package com.github.pbyrne84.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.{ResponseDefinitionBuilder, WireMock}

sealed abstract class ResponseBody
case object EmptyResponse extends ResponseBody
case class BinaryBody(value: List[Byte]) extends ResponseBody
case class StringBody(value: String) extends ResponseBody
case class JsonBody(value: String) extends ResponseBody {
  val jsonHeader: (String, String) = "Content-Type" -> "application/json"
}

object WiremockResponse {

  val emptySuccess: WiremockResponse = WiremockResponse()

  // Utility just to make the choice easier hiding the magic number element
  def redirectResponse(redirectHeader: RedirectHeader): WiremockResponse = {
    WiremockResponse(status = redirectHeader.status)
      .withHeader("Location", redirectHeader.uri.toString)
  }
}

case class WiremockResponse(
    status: Int = 200,
    headers: List[(String, List[String])] = List.empty,
    responseBody: ResponseBody = EmptyResponse
) {

  def withStatus(status: Int): WiremockResponse =
    copy(status = status)

  def withHeader(name: String, values: String*): WiremockResponse =
    copy(headers = headers :+ (name -> values.toList))

  def withResponseBody(responseBody: ResponseBody): WiremockResponse =
    copy(responseBody = responseBody)

  def asWireMock: ResponseDefinitionBuilder = {
    // Mutation party
    val builder = WireMock.aResponse().withStatus(status)

    responseBody match {
      case EmptyResponse =>
        ()

      case BinaryBody(value) =>
        builder.withBody(value.toArray)

      case StringBody(value) =>
        builder.withBody(value)

      case jsonBody: JsonBody =>
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
