package com.github.pbyrne84.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.{ResponseDefinitionBuilder, WireMock}

object ResponseBody {
  def jsonBody(json: String): JsonResponseBody = JsonResponseBody(json)
  def binaryBody(value: List[Byte]): BinaryResponseBody = BinaryResponseBody(value)
  def stringBody(string: String): StringResponseBody = StringResponseBody(string)
}

sealed abstract class ResponseBody
case object EmptyResponseBody extends ResponseBody
case class BinaryResponseBody(value: List[Byte]) extends ResponseBody
case class StringResponseBody(value: String) extends ResponseBody
case class JsonResponseBody(value: String) extends ResponseBody {
  val jsonHeader: (String, String) = "content-type" -> "application/json"
}

object WiremockResponse {

  val emptySuccess: WiremockResponse = WiremockResponse()

  // Utility just to make the choice easier hiding the magic number element
  def redirectResponse(redirectHeader: RedirectHeader): WiremockResponse = {
    WiremockResponse(status = redirectHeader.status)
      .withHeader("location", redirectHeader.uri.toString)
  }

}

case class WiremockResponse(
    status: Int = 200,
    headers: List[(String, List[String])] = List.empty,
    responseBody: ResponseBody = EmptyResponseBody
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
