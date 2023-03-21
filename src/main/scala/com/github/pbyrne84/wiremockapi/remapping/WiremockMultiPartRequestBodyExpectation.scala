package com.github.pbyrne84.wiremockapi.remapping

import com.github.tomakehurst.wiremock.matching.{MultipartValuePattern, MultipartValuePatternBuilder}

case class WiremockMultiPartRequestBodyExpectation(
    name: String,
    headerExpectations: List[NameValueExpectation] = List.empty,
    bodyExpectations: List[BodyValueExpectation] = List.empty,
    verificationMatchingType: MultipartValuePattern.MatchingType = MultipartValuePattern.MatchingType.ALL
) {
  def withHeaderExpectation(headerExpectation: NameValueExpectation): WiremockMultiPartRequestBodyExpectation =
    copy(headerExpectations = headerExpectations :+ headerExpectation)

  def withBodyExpectation(bodyExpectation: BodyValueExpectation): WiremockMultiPartRequestBodyExpectation =
    copy(bodyExpectations = bodyExpectations :+ bodyExpectation)

  def setVerificationMathingType(
      verificationMatchingType: MultipartValuePattern.MatchingType
  ): WiremockMultiPartRequestBodyExpectation =
    copy(verificationMatchingType = verificationMatchingType)

  def asWireMock: MultipartValuePatternBuilder = {
    import com.github.tomakehurst.wiremock.client.WireMock._
    val base = aMultipart()
      .withName(name)

    headerExpectations.foreach(expectation => base.withHeader(expectation.name, expectation.pattern))
    bodyExpectations.foreach(expectation => base.withBody(expectation.pattern))

    base
  }

}
