package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching

private[wiremockapi] trait UrlExpectationOps {

  implicit class UrlStringMatcherOps(string: String) {
    def asUrlEquals: UrlExpectation = UrlExpectation.equalsAll(string)

    def asUrlPathEquals: UrlExpectation = UrlExpectation.equalsPath(string)

    def asUrlMatching: UrlExpectation = UrlExpectation.matchesAll(string)

    def asUrlPathMatching: UrlExpectation = UrlExpectation.matchesPath(string)
  }

}

object UrlExpectation extends UrlExpectationOps {
  val anyUrlMatcher: UrlMatching = UrlMatching(".*")

  def matchesAll(value: String): UrlExpectation = UrlMatching(value)

  def equalsAll(value: String): UrlExpectation = UrlEquals(value)

  def matchesPath(value: String): UrlExpectation = UrlPatchMatching(value)

  def equalsPath(value: String): UrlExpectation = UrlPathEquals(value)

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
