package uk.org.devthings.scala.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern

// just simplify the api a bit for finger reasons as it is often easier to google to use the api as there is so
// much in the WireMock class

private[wiremockapi] trait WireMockValueExpectationOps {
  implicit class TupleStringStringWireMockOps(tuple: (String, String)) {
    val (name, value) = tuple

    def asContaining: NameValueExpectation = NameValueExpectation(name, WireMock.containing(value))

    def asEqualTo: NameValueExpectation = NameValueExpectation(name, WireMock.equalTo(value))

    def asMatching: NameValueExpectation = NameValueExpectation(name, WireMock.matching(value))

    def asEqualToXml: NameValueExpectation = NameValueExpectation(name, WireMock.equalToXml(value))

    def asMatchingXPath: NameValueExpectation = NameValueExpectation(name, WireMock.matchingXPath(value))

    def asEqualToJson: NameValueExpectation = NameValueExpectation(name, WireMock.equalToJson(value))

    def asMatchingJsonPath: NameValueExpectation = NameValueExpectation(name, WireMock.matchingJsonPath(value))
  }

  implicit class StringWireMockOps(value: String) {
    def asContaining: StringValuePattern = WireMock.containing(value)

    def asEqualTo: StringValuePattern = WireMock.equalTo(value)

    def asMatching: StringValuePattern = WireMock.matching(value)

    def asEqualToXml: StringValuePattern = WireMock.equalToXml(value)

    def asMatchingXPath: StringValuePattern = WireMock.matchingXPath(value)

    def asEqualToJson: StringValuePattern = WireMock.equalToJson(value)

    def asMatchingJsonPath: StringValuePattern = WireMock.matchingJsonPath(value)
  }
}

case class NameValueExpectation(name: String, pattern: StringValuePattern)
