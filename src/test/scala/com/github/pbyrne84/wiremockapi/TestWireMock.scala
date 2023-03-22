package com.github.pbyrne84.wiremockapi

import com.github.pbyrne84.wiremockapi.remapping.WiremockExpectation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching.UrlPattern

import java.net.ServerSocket
import scala.util.{Failure, Success, Using}

object TestWireMock {

  lazy val instance: TestWireMock = new TestWireMock(detectFreePort)

  private def detectFreePort: Int = {
    // Need to make sure it shuts down after calculation
    Using(new ServerSocket(0)) { serverSocket: ServerSocket =>
      serverSocket.getLocalPort
    } match {
      case Failure(exception) =>
        throw new RuntimeException("Failed calculation free port", exception)
      case Success(port) =>
        port
    }
  }

}

class TestWireMock(val port: Int) {

  val wireMockServer: WireMockServer = new WireMockServer(port)

  def reset(): Unit = {
    if (!wireMockServer.isRunning) {
      println(s"starting wireMockServer on port $port")
      wireMockServer.start()
    }

    wireMockServer.resetAll()
  }

  def stubAnyRequestResponse(responseDefinitionBuilder: ResponseDefinitionBuilder): Unit = {
    val anyRequestMappingBuilder = WireMock
      .any(UrlPattern.ANY)
      .willReturn(responseDefinitionBuilder)

    stubFor(anyRequestMappingBuilder)
  }

  private def stubFor(mappingBuilder: MappingBuilder): Unit = {
    wireMockServer.stubFor(mappingBuilder)
  }

  def stubExpectation(wiremockExpectation: WiremockExpectation): Unit = {
    stubFor(wiremockExpectation.asExpectationBuilder)
  }

  def verify(wiremockExpectation: WiremockExpectation): Unit = {
    wireMockServer.verify(wiremockExpectation.asVerificationBuilder)
  }

}
