package com.github.pbyrne84.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching.{
  MultipartValuePattern,
  MultipartValuePatternBuilder,
  RequestPatternBuilder,
  StringValuePattern
}

object WiremockExpectation {
  val default: WiremockExpectation = WiremockExpectation()

//  stubFor(
//    any(urlPathEqualTo("/everything"))
//      .withHeader("Accept", containing("xml"))
//      .withCookie("session", matching(".*12345.*"))
//      .withQueryParam("search_term", equalTo("WireMock"))
//      .withBasicAuth("jeff@example.com", "jeffteenjefftyjeff")
//      .withRequestBody(equalToXml("<search-results />"))
//      .withRequestBody(matchingXPath("//search-results"))
//      .withMultipartRequestBody(
//        aMultipart()
//          .withName("info")
//          .withHeader("Content-Type", containing("charset"))
//          .withBody(equalToJson("{}"))
//      )
//      .willReturn(aResponse())
//  );
}

case class WiremockExpectation(
    requestMethod: RequestMethod = RequestMethod.Any,
    urlExpectation: UrlExpectation = UrlExpectation.anyUrlMatcher,
    headerExpectations: Seq[NameValueExpectation] = List.empty,
    cookieExpectations: Seq[NameValueExpectation] = List.empty,
    queryParamExpectations: Seq[NameValueExpectation] = List.empty,
    bodyExpectations: Seq[BodyValueExpectation] = List.empty,
    multiPartExpectations: Seq[WiremockMultiPartRequestBodyExpectation] = List.empty,
    response: WiremockResponse = WiremockResponse.emptySuccess
) {

  def setMethod(requestMethod: RequestMethod): WiremockExpectation =
    copy(requestMethod = requestMethod)

  def setUrlExpectation(urlExpectation: UrlExpectation): WiremockExpectation =
    copy(urlExpectation = urlExpectation)

  def withHeaderExpectation(headerExpectation: NameValueExpectation): WiremockExpectation =
    copy(headerExpectations = headerExpectations :+ headerExpectation)

  def withHeaderExpectations(headerExpectations: Seq[NameValueExpectation]): WiremockExpectation =
    copy(headerExpectations = headerExpectations ++ headerExpectations)

  def withCookieExpectation(cookieExpectation: NameValueExpectation): WiremockExpectation =
    copy(cookieExpectations = cookieExpectations :+ cookieExpectation)

  def withQueryParamExpectation(queryParamExpectation: NameValueExpectation): WiremockExpectation =
    copy(queryParamExpectations = queryParamExpectations :+ queryParamExpectation)

  def withBodyExpectation(bodyExpectation: BodyValueExpectation): WiremockExpectation =
    copy(bodyExpectations = bodyExpectations :+ bodyExpectation)

  def withBodyExpectations(additionalBodyExpectations: Seq[BodyValueExpectation]): WiremockExpectation =
    copy(bodyExpectations = bodyExpectations ++ additionalBodyExpectations)

  def withMultiPartRequestBodyExpectation(bodyExpectation: WiremockMultiPartRequestBodyExpectation) =
    copy(multiPartExpectations = multiPartExpectations :+ bodyExpectation)

  def withResponse(wiremockResponse: WiremockResponse) =
    copy(response = wiremockResponse)

  def asExpectationBuilder: MappingBuilder = {
    // This is all going to mutate :(
    val mappingBuilder: MappingBuilder = requestMethod.asMappingBuilder(urlExpectation)

    headerExpectations.foreach(expectation => mappingBuilder.withHeader(expectation.name, expectation.pattern))
    cookieExpectations.foreach(expectation => mappingBuilder.withCookie(expectation.name, expectation.pattern))
    queryParamExpectations.foreach(expectation => mappingBuilder.withQueryParam(expectation.name, expectation.pattern))

    bodyExpectations.foreach { (expectation: BodyValueExpectation) =>
      mappingBuilder.withRequestBody(expectation.pattern)
      addAutoContentHeader(
        expectation.maybeContentTypeHeader,
        mappingBuilder.withHeader
      )
    }

    multiPartExpectations.foreach(expectation => mappingBuilder.withMultipartRequestBody(expectation.asWireMock))
    mappingBuilder.willReturn(response.asWireMock)

    mappingBuilder
  }

  // yes mutating builder by out params is nice
  private def addAutoContentHeader[A](
      maybeContentTypeHeader: Option[(String, String)],
      call: (String, StringValuePattern) => A
  ): Unit = {
    maybeContentTypeHeader.foreach { case (name, value) =>
      call(
        name,
        WireMock.equalTo(value)
      )
    }
  }

  // the interfaces are fairly similar but have different builders
  def asVerificationBuilder: RequestPatternBuilder = {
    val requestPatternBuilder: RequestPatternBuilder = requestMethod.asVerificationBuilder(urlExpectation)

    headerExpectations.foreach(expectation => requestPatternBuilder.withHeader(expectation.name, expectation.pattern))
    cookieExpectations.foreach(expectation => requestPatternBuilder.withCookie(expectation.name, expectation.pattern))
    queryParamExpectations.foreach(expectation =>
      requestPatternBuilder.withQueryParam(expectation.name, expectation.pattern)
    )
    bodyExpectations.foreach { expectation =>
      requestPatternBuilder.withRequestBody(expectation.pattern)
      addAutoContentHeader(
        expectation.maybeContentTypeHeader,
        requestPatternBuilder.withHeader
      )
    }

    multiPartExpectations.foreach { expection =>
      if (expection.verificationMatchingType == MultipartValuePattern.MatchingType.ALL) {
        requestPatternBuilder.withAllRequestBodyParts(expection.asWireMock)
      } else {
        requestPatternBuilder.withAnyRequestBodyPart(expection.asWireMock)
      }
    }

    requestPatternBuilder
  }
}
