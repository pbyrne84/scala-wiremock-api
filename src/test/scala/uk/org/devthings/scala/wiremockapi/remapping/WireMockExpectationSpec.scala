package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching._
import org.scalactic.anyvals.NonEmptyList
import org.scalatest.prop.TableDrivenPropertyChecks
import sttp.model.Uri
import uk.org.devthings.scala.wiremockapi.BaseSpec

import java.util
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala}

/**
  * Verify request with duplicate query params https://github.com/wiremock/wiremock/issues/398 is not supported yet
  * https://github.com/wiremock/wiremock/pull/2110
  */
class WireMockExpectationSpec extends BaseSpec with TableDrivenPropertyChecks {
  import sttp.client3._
  // Extension methods for tuples to expectation, a bit more finger friendly.

  import WireMockExpectation.ops._

  private val methodRequestMappings = List(
    (RequestMethod.Any, basicRequest.get _),
    (RequestMethod.Get, basicRequest.get _),
    (RequestMethod.Put, basicRequest.put _),
    (RequestMethod.Post, basicRequest.post _),
    (RequestMethod.Delete, basicRequest.delete _),
    (RequestMethod.Patch, basicRequest.patch _)
  )

  private val allOperations = List(
    ("name1", "value1").asEqualTo,
    ("name2", "value2").asMatching,
    ("name3", "value3").asContaining,
    ("name4", "<value4/>").asEqualToXml,
    ("name5", "{}").asEqualToJson,
    ("name6", "value6").asMatchingJsonPath,
    ("name7", "value7").asMatchingXPath
  )

  // doesn't actually allow duplicated named fields for repeated instances such as param1=a&param1=b etc.
  private val allOperationsWhenMappedToWireMock = Map(
    "name1" -> WireMock.equalTo("value1"),
    "name2" -> WireMock.matching("value2"),
    "name3" -> WireMock.containing("value3"),
    "name4" -> WireMock.equalToXml("<value4/>"),
    "name5" -> WireMock.equalToJson("{}"),
    "name6" -> WireMock.matchingJsonPath("value6"),
    "name7" -> WireMock.matchingXPath("value7")
  )

  before {
    reset()
  }

  "wiremock expectation" should {

    "match and verify an empty request on default" in {
      val default = WireMockExpectation.willRespondOk
      wireMock.stubExpectation(default)

      val request = basicRequest.get(uri"${wireMock.baseRequestUrl}/")
      val response = request.send(sttpBackend)

      response.code.code shouldBe 200
      response.body shouldBe Right("")

      wireMock.verify(default)
    }

    "match and verify all additional get values on default" in {
      val default = WireMockExpectation.willRespondOk
      wireMock.stubExpectation(default)

      val request = basicRequest
        .get(uri"${wireMock.baseRequestUrl}/url?param1=2")
        .header("header1", "header1Value")
        .cookie("cookie1", "cookie1Value")

      val response = request.send(sttpBackend)

      response.code.code shouldBe 200
      response.body shouldBe Right("")

      wireMock.verify(default)
    }

    "match and verify on all http methods mapped off default" in {
      val allMethods =
        Table(
          ("method", "request call"),
          methodRequestMappings: _*
        )

      forAll(allMethods) { (method, call) =>
        reset()

        val default = WireMockExpectation.willRespondOk.expectsMethod(method)
        wireMock.stubExpectation(default)

        val request = call(uri"${wireMock.baseRequestUrl}/url?param1=2")
          .header("header1", "header1Value")
          .cookie("cookie1", "cookie1Value")

        val response = request.send(sttpBackend)

        response.code.code shouldBe 200
        response.body shouldBe Right("")

        wireMock.verify(default)
      }
    }

    "fail on all other methods except the one that is mocked" in {
      val allMethods =
        Table(
          ("method", "request call"),
          methodRequestMappings.filterNot(_._1 == RequestMethod.Any): _*
        )

      forAll(allMethods) { (currentHttpMethod, validCall) =>
        reset()

        val default = WireMockExpectation.willRespondOk.expectsMethod(currentHttpMethod)
        wireMock.stubExpectation(default)

        val otherMethodCalls: List[(RequestMethod, Uri => Request[Either[String, String], Any])] = methodRequestMappings
          .filterNot { case (method, _) =>
            method == currentHttpMethod || method == RequestMethod.Any
          }

        otherMethodCalls.foreach { case (method, invalidCall) =>
          withClue(s"Calling the mock with the unexpected '$method' call") {
            validCall(uri"${wireMock.baseRequestUrl}").send(sttpBackend).code.code shouldBe 200
            invalidCall(uri"${wireMock.baseRequestUrl}").send(sttpBackend).code.code shouldBe 404
          }
        }

      }

    }

    "map headers to their wiremock equivalent" in {
      val expectation = allOperations.foldLeft(WireMockExpectation.willRespondOk) {
        case (expectation: WireMockExpectation, valueExpectation: NameValueExpectation) =>
          expectation.expectsHeader(valueExpectation)
      }

      val setupExpectationHeaders: Map[String, StringValuePattern] = getMultiValuePatternMapFromExpectation(
        expectation.asExpectationBuilder,
        (requestPattern: RequestPattern) => requestPattern.getHeaders
      )

      val setupVerificationHeaders: Map[String, StringValuePattern] = getMultiValuePatternMapFromVerification(
        expectation.asVerificationBuilder,
        (requestPattern: RequestPattern) => requestPattern.getHeaders
      )

      setupExpectationHeaders shouldBe allOperationsWhenMappedToWireMock
      setupVerificationHeaders shouldBe allOperationsWhenMappedToWireMock
    }

    def getMultiValuePatternMapFromExpectation(
        mappingBuilder: MappingBuilder,
        call: RequestPattern => java.util.Map[String, MultiValuePattern]
    ): Map[String, StringValuePattern] = {
      call(mappingBuilder.build().getRequest).asScala.map(a => a._1 -> a._2.getValuePattern).toMap // else mutable
    }

    def getMultiValuePatternMapFromVerification(
        mappingBuilder: RequestPatternBuilder,
        call: RequestPattern => java.util.Map[String, MultiValuePattern]
    ): Map[String, StringValuePattern] = {
      call(mappingBuilder.build()).asScala.map(a => a._1 -> a._2.getValuePattern).toMap
    }

    "map parameters to their wiremock equivalent" in {
      val expectation = allOperations
        .foldLeft(WireMockExpectation.willRespondOk) {
          case (expectation: WireMockExpectation, valueExpectation: NameValueExpectation) =>
            expectation.expectsQueryParam(valueExpectation)
        }

      val setupExpectationParameters: Map[String, StringValuePattern] = getMultiValuePatternMapFromExpectation(
        expectation.asExpectationBuilder,
        (requestPattern: RequestPattern) => requestPattern.getQueryParameters
      )

      val setupVerificationParameters: Map[String, StringValuePattern] = getMultiValuePatternMapFromVerification(
        expectation.asVerificationBuilder,
        (requestPattern: RequestPattern) => requestPattern.getQueryParameters
      )

      setupExpectationParameters shouldBe allOperationsWhenMappedToWireMock
      setupVerificationParameters shouldBe allOperationsWhenMappedToWireMock
    }

    "map cookies to their wiremock equivalent" in {
      val expectation = allOperations
        .foldLeft(WireMockExpectation.willRespondOk) {
          case (expectation: WireMockExpectation, valueExpectation: NameValueExpectation) =>
            expectation.expectsCookie(valueExpectation)
        }

      val setupExpectationParameters: Map[String, StringValuePattern] = getStringValuePatternMapFromExpectation(
        expectation.asExpectationBuilder,
        (requestPattern: RequestPattern) => requestPattern.getCookies
      )

      val setupVerificationParameters: Map[String, StringValuePattern] = getStringValuePatternMapFromVerification(
        expectation.asVerificationBuilder,
        (requestPattern: RequestPattern) => requestPattern.getCookies
      )

      setupExpectationParameters shouldBe allOperationsWhenMappedToWireMock
      setupVerificationParameters shouldBe allOperationsWhenMappedToWireMock
    }

    def getStringValuePatternMapFromExpectation(
        mappingBuilder: MappingBuilder,
        call: RequestPattern => java.util.Map[String, StringValuePattern]
    ): Map[String, StringValuePattern] = {
      call(mappingBuilder.build().getRequest).asScala.map(a => a._1 -> a._2).toMap
    }

    def getStringValuePatternMapFromVerification(
        mappingBuilder: RequestPatternBuilder,
        call: RequestPattern => java.util.Map[String, StringValuePattern]
    ): Map[String, StringValuePattern] = {
      call(mappingBuilder.build()).asScala.map(a => a._1 -> a._2).toMap
    }

    "map body to their wiremock equivalent auto adding content type headers where applicable by default" in {

      val mappings = Table(
        ("desc", "content type header", "expectation"),
        ("json equals with header", Some("json"), "{}".asBodyEqualsJson),
        ("json matches with header", Some("json"), "json-path".asBodyMatchesJsonPath),
        ("xml equals with header", Some("xml"), "<xml/>".asBodyEqualsXml),
        ("xml matches with header", Some("xml"), "xml-path".asBodyMatchesXPath),
        ("any expectation", None, BodyValueExpectation.any),
        ("json equals with no header", None, "{}".asBodyEqualsJson.withDisabledAutoHeader),
        ("json matches with no header", None, "json-path".asBodyMatchesJsonPath.withDisabledAutoHeader),
        ("xml equals with no header", None, "<xml/>".asBodyEqualsXml.withDisabledAutoHeader),
        ("xml matches with no header", None, "xml-path".asBodyMatchesXPath.withDisabledAutoHeader),
        ("string equals", None, "string-equals".asBodyEquals),
        ("string matches", None, "string-matches".asBodyMatches),
        ("string containing", None, "string-contains".asBodyContains)
      )

      forAll(mappings) { (_, maybeContentType, bodyValueExpectation) =>
        val expectationWithBody = WireMockExpectation.willRespondOk
          .expectsBody(bodyValueExpectation)

        val builtWiremockExpectation = expectationWithBody.asExpectationBuilder
          .build()

        val builtWiremockVerification = expectationWithBody.asVerificationBuilder
          .build()

        builtWiremockExpectation.getRequest.getBodyPatterns.asScala.toList shouldBe List(bodyValueExpectation.pattern)
        builtWiremockVerification.getBodyPatterns.asScala.toList shouldBe List(bodyValueExpectation.pattern)

        implicit class HeaderOps[A](maybeHeaderMap: java.util.Map[A, MultiValuePattern]) {
          def comparableHeaders: List[(A, StringValuePattern)] = {
            Option(maybeHeaderMap)
              .getOrElse(new util.HashMap[A, MultiValuePattern]())
              .asScala
              .map(header => header._1 -> header._2.getValuePattern)
              .toList
          }
        }

        maybeContentType.foreach { expectedContentType =>
          val expectedHeaderMap = List(
            "content-type" -> s"application/$expectedContentType".asEqualTo
          )

          builtWiremockExpectation.getRequest.getHeaders.comparableHeaders shouldBe expectedHeaderMap
          builtWiremockVerification.getHeaders.comparableHeaders shouldBe expectedHeaderMap

        }
      }
    }

    "allow multiple body expectations" in {

      val expectations: NonEmptyList[BodyValueExpectation] = NonEmptyList(
        "body-equals".asBodyEquals,
        "body-matches".asBodyMatches,
        "body-containing".asBodyContains
      )

      val expectation = WireMockExpectation.willRespondOk
        .expectsBody(expectations.head)
        .expectsBodies(expectations.tail: _*)

      implicit class BodyPatternOps(patterns: java.util.List[ContentPattern[_]]) {
        def asSortedScalaList: List[ContentPattern[_]] = patterns.asScala.toList.sortBy(_.toString)
      }

      val sortedExpectations = expectations.toList
        .map(_.pattern)
        .sortBy(_.toString)

      expectation.asExpectationBuilder.build().getRequest.getBodyPatterns.asSortedScalaList shouldBe sortedExpectations
      expectation.asVerificationBuilder.build().getBodyPatterns.asSortedScalaList shouldBe sortedExpectations

    }

  }

}
