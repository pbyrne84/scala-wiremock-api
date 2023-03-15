package com.github.pbyrne84.wiremockapi.remapping

import java.net.URI
//Taken from https://developer.mozilla.org/en-US/docs/Web/HTTP/Redirections

object RedirectHeader {
  private[remapping] object code {
    val MovedPermanently = 301
    val Permanent = 308
    val Found = 302
    val SeeOther = 303
    val Temporary = 307
    val MultipleChoice = 300
    val NotModified = 304
  }

}

sealed abstract class RedirectHeader(val status: Int, val uri: URI, val message: String, val typicalUseCase: String)
case class MovedPermanentlyRedirect(override val uri: URI)
    extends RedirectHeader(
      status = RedirectHeader.code.MovedPermanently,
      uri = uri,
      message = "Moved Permanently",
      typicalUseCase = "Reorganization of a website"
    )

case class PermanentRedirect(override val uri: URI)
    extends RedirectHeader(
      status = RedirectHeader.code.Permanent,
      uri = uri,
      message = "Permanent Redirect",
      typicalUseCase = "Reorganization of a website, with non-GET links/operations"
    )

case class FoundRedirect(override val uri: URI)
    extends RedirectHeader(
      status = RedirectHeader.code.Found,
      uri = uri,
      message = "Found",
      typicalUseCase = "The Web page is temporarily unavailable for unforeseen reasons"
    )

case class SeeOtherRedirect(override val uri: URI)
    extends RedirectHeader(
      status = RedirectHeader.code.SeeOther,
      uri = uri,
      message = "See Other",
      typicalUseCase =
        "Used to redirect after a PUT or a POST, so that refreshing the result page doesn't re-trigger the operation"
    )

case class TemporaryRedirect(override val uri: URI)
    extends RedirectHeader(
      status = RedirectHeader.code.Temporary,
      uri = uri,
      message = "Temporary Redirect",
      typicalUseCase =
        "The Web page is temporarily unavailable for unforeseen reasons. Better than 302 when non-GET operations are available on the site."
    )

case class MultipleChoiceRedirect(override val uri: URI)
    extends RedirectHeader(
      status = RedirectHeader.code.MultipleChoice,
      uri = uri,
      message = "Multiple Choice",
      typicalUseCase =
        "Not many: the choices are listed in an HTML page in the body. Machine-readable choices are encouraged to be sent as Link headers with rel=alternate."
    )

case class NotModifiedRedirect(override val uri: URI)
    extends RedirectHeader(
      status = RedirectHeader.code.NotModified,
      uri,
      "Not Modified",
      "Sent for revalidated conditional requests. Indicates that the cached response is still fresh and can be used."
    )
