package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching._

object BodyValueExpectation {
  import WireMockValueExpectation.ops._
  val any: BodyValueExpectation = BodyValueExpectation(".*".asMatching)
  def equalsJson(json: String): BodyValueExpectation = BodyValueExpectation(json.asEqualToJson)
  def matchesJsonPath(json: String): BodyValueExpectation = BodyValueExpectation(json.asMatchingJsonPath)
  def equalsXml(xml: String): BodyValueExpectation = BodyValueExpectation(xml.asEqualToXml)
  def matchesXmlPath(xml: String): BodyValueExpectation = BodyValueExpectation(xml.asMatchingXPath)

  object ops {
    implicit class StringAsBodyOps(value: String) {
      def asBodyContains: BodyValueExpectation = BodyValueExpectation(WireMock.containing(value))

      def asBodyEquals: BodyValueExpectation = BodyValueExpectation(WireMock.equalTo(value))

      def asBodyMatches: BodyValueExpectation = BodyValueExpectation(WireMock.matching(value))

      def asBodyEqualsXml: BodyValueExpectation = BodyValueExpectation(WireMock.equalToXml(value))

      def asBodyMatchesXPath: BodyValueExpectation = BodyValueExpectation(WireMock.matchingXPath(value))

      def asBodyEqualsJson: BodyValueExpectation = BodyValueExpectation(WireMock.equalToJson(value))

      def asBodyMatchesJsonPath: BodyValueExpectation = BodyValueExpectation(WireMock.matchingJsonPath(value))

    }
  }
}

/**
  * @param pattern
  * @param autoHeader
  *   \- if xml or json we we'll add the content type to the expectation/verification header this gets boring to do it
  *   all the time and so is often forgotten.
  */
case class BodyValueExpectation(pattern: StringValuePattern, autoHeader: Boolean = true) {

  val maybeContentTypeHeader: Option[(String, String)] = pattern match {
    case _: EqualToXmlPattern | _: MatchesXPathPattern if autoHeader =>
      Some("content-type" -> "application/xml")

    case _: EqualToJsonPattern | _: MatchesJsonPathPattern if autoHeader =>
      Some("content-type" -> "application/json")

    case _ => None
  }

  def withDisabledAutoHeader: BodyValueExpectation =
    copy(autoHeader = false)

  def withEnabledAutoHeader: BodyValueExpectation =
    copy(autoHeader = true)
}
