package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching.{MultipartValuePattern, RequestPatternBuilder, StringValuePattern}

object WiremockExpectation {

  val willRespondOk: WiremockExpectation = willRespondStatus(200)
  // Wiremock will return 404 by default on a non match but that leads to potentially confusing log output
  val willRespondNotFound: WiremockExpectation = willRespondStatus(404)
  def willRespondStatus(status: Int): WiremockExpectation = WiremockExpectation().willReturnStatus(status)
  val willRespondInternalServerError: WiremockExpectation = willRespondStatus(500)

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
    response: WiremockResponse = WiremockResponse.statusOk,
    maybeScenarioInfo: Option[ScenarioInfo] = None
) {

  def expectsUrl(urlExpectation: UrlExpectation): WiremockExpectation =
    copy(urlExpectation = urlExpectation)

  def expectsMethod(requestMethod: RequestMethod): WiremockExpectation =
    copy(requestMethod = requestMethod)

  def setScenarioInfo(scenarioInfo: ScenarioInfo): WiremockExpectation =
    copy(maybeScenarioInfo = Some(scenarioInfo))

  def expectsHeader(headerExpectation: NameValueExpectation): WiremockExpectation =
    copy(headerExpectations = headerExpectations :+ headerExpectation)

  def expectsHeaders(headerExpectations: NameValueExpectation*): WiremockExpectation =
    copy(headerExpectations = headerExpectations ++ headerExpectations)

  def expectsCookie(cookieExpectation: NameValueExpectation): WiremockExpectation =
    copy(cookieExpectations = cookieExpectations :+ cookieExpectation)

  def expectsQueryParam(queryParamExpectation: NameValueExpectation): WiremockExpectation =
    copy(queryParamExpectations = queryParamExpectations :+ queryParamExpectation)

  def expectsQueryParams(queryParamExpectations: NameValueExpectation*): WiremockExpectation =
    copy(queryParamExpectations = queryParamExpectations :++ queryParamExpectations)

  def expectsBody(bodyExpectation: BodyValueExpectation): WiremockExpectation =
    copy(bodyExpectations = bodyExpectations :+ bodyExpectation)

  def expectsBodies(additionalBodyExpectations: BodyValueExpectation*): WiremockExpectation =
    copy(bodyExpectations = bodyExpectations ++ additionalBodyExpectations)

  def expectsMultiPartRequest(bodyExpectation: WiremockMultiPartRequestBodyExpectation): WiremockExpectation =
    copy(multiPartExpectations = multiPartExpectations :+ bodyExpectation)

  def willReturnStatus(status: Int): WiremockExpectation = {
    copy(response = response.withStatus(status))
  }

  def willRespondWithBody(responseBody: ResponseBody): WiremockExpectation = {
    copy(response = response.withResponseBody(responseBody))
  }

  def willRespondWithHeader(name: String, values: String*): WiremockExpectation = {
    copy(response = response.withHeader(name, values: _*))
  }

  def willRespondWith(wiremockResponse: WiremockResponse): WiremockExpectation =
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
