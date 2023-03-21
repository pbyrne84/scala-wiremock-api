package com.github.pbyrne84.wiremockapi
import com.github.pbyrne84.wiremockapi.remapping.WiremockExpectation
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import org.scalactic.Prettifier
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client3.{HttpClientSyncBackend, Identity, SttpBackend}
import sttp.model.Header

abstract class BaseSpec extends AnyWordSpec with Matchers with BeforeAndAfter {

  private val caseClassPrettifier: CaseClassPrettifier = new CaseClassPrettifier

  implicit val prettifier: Prettifier = Prettifier.apply {
    case a: AnyRef if CaseClassPrettifier.shouldBeUsedInTestMatching(a) =>
      caseClassPrettifier.prettify(a)

    case a: Any => Prettifier.default(a)
  }

  object wireMock {
    val instance: TestWireMock = TestWireMock.instance
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
