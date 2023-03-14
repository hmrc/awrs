
package uk.gov.hmrc.helpers.http

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait StubbedBasicHttpCalls {

  def stubbedGetUrlEqual(url: String, statusCode: Int, responseBody: String): StubMapping = {
    stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseBody)
      )
    )
  }

  def stubbedGet(url: String, statusCode: Int, responseBody: String): StubMapping = {
    stubFor(get(urlPathMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseBody)
      )
    )
  }

  def stubbedPost(url: String, statusCode: Int, responseBody: String): StubMapping = {
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseBody)
      )
    )
  }

  def stubbedPut(url: String, statusCode: Int): StubMapping = {
    stubFor(put(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
      )
    )
  }
}
