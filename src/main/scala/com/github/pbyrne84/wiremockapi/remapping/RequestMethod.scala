package com.github.pbyrne84.wiremockapi.remapping

import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder

sealed trait RequestMethod {

  def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder
  def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder

}

object RequestMethod {

  case object Get extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.get(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.getRequestedFor(urlExpectation.asWireMock)
  }

  case object Post extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.post(urlExpectation.asWireMock)

    def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.postRequestedFor(urlExpectation.asWireMock)
  }

  case object Put extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.put(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.putRequestedFor(urlExpectation.asWireMock)
  }

  case object Delete extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.delete(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.deleteRequestedFor(urlExpectation.asWireMock)
  }

  case object Patch extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.patch(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.patchRequestedFor(urlExpectation.asWireMock)
  }

  case object Options extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.options(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.optionsRequestedFor(urlExpectation.asWireMock)
  }

  case object Head extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.head(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.headRequestedFor(urlExpectation.asWireMock)
  }

  case object Trace extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.trace(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.traceRequestedFor(urlExpectation.asWireMock)
  }

  case object Any extends RequestMethod {
    override def asMappingBuilder(urlExpectation: UrlExpectation): MappingBuilder =
      WireMock.any(urlExpectation.asWireMock)

    override def asVerificationBuilder(urlExpectation: UrlExpectation): RequestPatternBuilder =
      WireMock.anyRequestedFor(urlExpectation.asWireMock)
  }

}
