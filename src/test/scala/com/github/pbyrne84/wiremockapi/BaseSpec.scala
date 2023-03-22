package com.github.pbyrne84.wiremockapi
import com.github.pbyrne84.wiremockapi.remapping.WiremockExpectation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import io.circe.{Json, ParsingFailure}
import org.scalactic.Prettifier
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client3.{HttpClientSyncBackend, Identity, SttpBackend}
import sttp.model.Header

abstract class BaseSpec extends AnyWordSpec with ScalaFutures with ImpatientPatience with Matchers with BeforeAndAfter {

  private val caseClassPrettifier: CaseClassPrettifier = new CaseClassPrettifier

  implicit val prettifier: Prettifier = Prettifier.apply {
    case a: AnyRef if CaseClassPrettifier.shouldBeUsedInTestMatching(a) =>
      caseClassPrettifier.prettify(a)

    case a: Any => Prettifier.default(a)
  }

  // namespace things as it makes ripping it out when it gets messy easy
  object wireMock {
    val instance: TestWireMock = TestWireMock.instance
    val server: WireMockServer = instance.wireMockServer
    val port: Int = instance.port
    def reset(): Unit = instance.reset()

    val baseRequestUrl = s"http://localhost:$port"

    def stubAnyRequestResponse(responseDefinitionBuilder: ResponseDefinitionBuilder): Unit =
      instance.stubAnyRequestResponse(responseDefinitionBuilder)

    def stubExpectation(wiremockExpectation: WiremockExpectation): Unit =
      instance.stubExpectation(wiremockExpectation)

    def verify(wiremockExpectation: WiremockExpectation): Unit =
      instance.verify(wiremockExpectation)

  }

  object json {

    def unsafeParse(string: String): Json = {
      io.circe.parser.parse(string) match {
        case Left(value) => fail(new RuntimeException(s"Could not parse '$string''"))
        case Right(value) => value
      }
    }

    def parse(string: String): Either[ParsingFailure, Json] = {
      io.circe.parser.parse(string)
    }
  }

  object headers {
    val defaultWireMockHeaderNames: Seq[String] = List(
      "transfer-encoding",
      "content-encoding",
      "server",
      "matched-stub-id",
      "vary"
    ).sorted
  }

  implicit class HeaderOps(list: Seq[Header]) {
    def nonDefaultHeaders: List[Header] =
      list
        .filterNot(header => headers.defaultWireMockHeaderNames.contains(header.name))
        .sortBy(_.name)
        .toList
  }

  protected val sttpBackend: SttpBackend[Identity, Any] = HttpClientSyncBackend()

  def reset(): Unit = wireMock.reset()
}
