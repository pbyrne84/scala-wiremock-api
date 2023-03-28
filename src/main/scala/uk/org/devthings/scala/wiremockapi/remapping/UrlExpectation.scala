package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching

object UrlExpectation {
  val anyUrlMatcher: UrlMatching = UrlMatching(".*")

  implicit class UrlStringMatcherOps(string: String) {
    def asUrlEquals: UrlExpectation = UrlEquals(string)
    def asUrlPathEquals: UrlExpectation = UrlPathEquals(string)
    def asUrlMatching: UrlExpectation = UrlMatching(string)
    def asUrlPathMatching: UrlExpectation = UrlPatchMatching(string)
  }

  def matchesAll(value: String): UrlExpectation = value.asUrlMatching
  def equalsAll(value: String): UrlExpectation = value.asUrlEquals
  def matchesPath(value: String): UrlExpectation = value.asUrlPathMatching
  def equalsPath(value: String): UrlExpectation = value.asUrlPathEquals
}

sealed trait UrlExpectation {
  def asWireMock: com.github.tomakehurst.wiremock.matching.UrlPattern
}

sealed trait UrlRegexExpectation extends UrlExpectation {}

case class UrlMatching(regex: String) extends UrlRegexExpectation {
  override def asWireMock: matching.UrlPattern = WireMock.urlMatching(regex)
}

case class UrlPatchMatching(regex: String) extends UrlRegexExpectation {
  override def asWireMock: matching.UrlPattern = WireMock.urlPathMatching(regex)
}

sealed trait UrlEqualsExpectation extends UrlExpectation

case class UrlEquals(value: String) extends UrlEqualsExpectation {
  override def asWireMock: matching.UrlPattern = WireMock.urlEqualTo(value)
}

case class UrlPathEquals(value: String) extends UrlEqualsExpectation {
  override def asWireMock: matching.UrlPattern = WireMock.urlPathEqualTo(value)
}
