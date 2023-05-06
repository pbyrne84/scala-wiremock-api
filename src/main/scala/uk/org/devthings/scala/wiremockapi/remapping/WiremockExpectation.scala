package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching.{MultipartValuePattern, RequestPatternBuilder, StringValuePattern}

object WiremockExpectation {

  val default: WiremockExpectation = WiremockExpectation()

  def applyAsScenario(
      expectations: List[WiremockExpectation],
      server: WireMockServer,
      scenarioInfoGenerator: ScenarioInfoGenerator = ScenarioInfoGenerator.default
  ): List[WiremockExpectation] = {
    val maxIndex = expectations.size - 1

    expectations.zipWithIndex.map { case (expectation: WiremockExpectation, index) =>
      val scenarioInfo: ScenarioInfo =
        scenarioInfoGenerator.createScenarioInfo(scenarioInfoGenerator.scenarioName, index, maxIndex)

      val scenarioWithExpectation = expectation.setScenarioInfo(scenarioInfo)

      server.stubFor(scenarioWithExpectation.asExpectationBuilder)
      scenarioWithExpectation
    }
  }

  object ops
      extends UrlExpectationOps
      with BodyValueExpectationOps
      with WireMockValueExpectationOps
      with ResponseBodyOps {

    implicit class WiremockExpectationsOps(expectations: List[WiremockExpectation]) {
      def applyAsScenario(
          server: WireMockServer,
          scenarioInfoGenerator: ScenarioInfoGenerator = ScenarioInfoGenerator.default
      ): List[WiremockExpectation] = {
        WiremockExpectation.applyAsScenario(expectations, server, scenarioInfoGenerator)
      }
    }
  }
}

case class ScenarioInfo(scenarioName: String, expectedCurrentState: String, nextState: String)

case class WiremockExpectation(
    requestMethod: RequestMethod = RequestMethod.Any,
    urlExpectation: UrlExpectation = UrlExpectation.anyUrlMatcher,
    headerExpectations: Seq[NameValueExpectation] = List.empty,
    cookieExpectations: Seq[NameValueExpectation] = List.empty,
    queryParamExpectations: Seq[NameValueExpectation] = List.empty,
    bodyExpectations: Seq[BodyValueExpectation] = List.empty,
    multiPartExpectations: Seq[WiremockMultiPartRequestBodyExpectation] = List.empty,
    response: WiremockResponse = WiremockResponse.emptySuccess,
    maybeScenarioInfo: Option[ScenarioInfo] = None
) {

  def setMethod(requestMethod: RequestMethod): WiremockExpectation =
    copy(requestMethod = requestMethod)

  def setScenarioInfo(scenarioInfo: ScenarioInfo): WiremockExpectation =
    copy(maybeScenarioInfo = Some(scenarioInfo))

  def setUrl(urlExpectation: UrlExpectation): WiremockExpectation =
    copy(urlExpectation = urlExpectation)

  def withHeader(headerExpectation: NameValueExpectation): WiremockExpectation =
    copy(headerExpectations = headerExpectations :+ headerExpectation)

  def withHeaders(headerExpectations: NameValueExpectation*): WiremockExpectation =
    copy(headerExpectations = headerExpectations ++ headerExpectations)

  def withCookie(cookieExpectation: NameValueExpectation): WiremockExpectation =
    copy(cookieExpectations = cookieExpectations :+ cookieExpectation)

  def withQueryParam(queryParamExpectation: NameValueExpectation): WiremockExpectation =
    copy(queryParamExpectations = queryParamExpectations :+ queryParamExpectation)

  def withQueryParams(queryParamExpectations: NameValueExpectation*): WiremockExpectation =
    copy(queryParamExpectations = queryParamExpectations :++ queryParamExpectations)

  def withBody(bodyExpectation: BodyValueExpectation): WiremockExpectation =
    copy(bodyExpectations = bodyExpectations :+ bodyExpectation)

  def withBodies(additionalBodyExpectations: BodyValueExpectation*): WiremockExpectation =
    copy(bodyExpectations = bodyExpectations ++ additionalBodyExpectations)

  def withMultiPartRequest(bodyExpectation: WiremockMultiPartRequestBodyExpectation) =
    copy(multiPartExpectations = multiPartExpectations :+ bodyExpectation)

  def withResponse(wiremockResponse: WiremockResponse): WiremockExpectation =
    copy(response = wiremockResponse)

  def asExpectationBuilder: MappingBuilder = {
    // This is all going to mutate :(

    val mappingBuilder: MappingBuilder =
      maybeScenarioInfo
        .map { scenarioInfo =>
          requestMethod
            .asMappingBuilder(urlExpectation)
            .inScenario(scenarioInfo.scenarioName)
            .whenScenarioStateIs(scenarioInfo.expectedCurrentState)
            .willSetStateTo(scenarioInfo.nextState)
        }
        .getOrElse(requestMethod.asMappingBuilder(urlExpectation))

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
