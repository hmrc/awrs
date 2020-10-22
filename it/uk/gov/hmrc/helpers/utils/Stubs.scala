/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.helpers.utils

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.helpers.IntegrationSpec

trait Stubs extends IntegrationSpec with IntegrationData {

  def stubShowAndRedirectExternalCalls(affinityGroup: String): StubMapping = {
    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(
            s"""{
               |  "authorisedEnrolments": [{}],
               |  "affinityGroup": "$affinityGroup",
               |  "credentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
               |  "authProviderId": { "ggCredId": "123" }
               |}""".stripMargin
          )
      )
    )
  }

  def stubEtmpRegistrationResponse(status: Int, body: String): StubMapping = {
    stubbedGet(regimeURI, status, body)
  }

  def stubAwrsSubscriptionResponse(status: Int, body: Option[String] = None): StubMapping = {
    stubbedPost(s"$baseURI$subscriptionURI$safeId", status, body.getOrElse(""))
  }

  def stubGetSubscriptionStatus(status: Int, awrsStatus: Option[String] = None): StubMapping = {
    val responseBody: String = awrsStatus.fold(""){awrsStat =>
      Json.obj(
      "processingDate" -> "20/01/2010",
      "formBundleStatus" -> awrsStat,
      "groupBusinessPartner" -> false
      ).toString
    }
    stubbedGet(s"""$baseURI$subscriptionURI$enrolmentRef$statusURI""", status, responseBody)
  }

  def stubUpsertAwrsEnrolment(status: Int): StubMapping = {
    stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", status)
  }

  def stubAuditPosts: StubMapping = {
    stubbedPost("/write/audit", OK, "")
    stubbedPost("/write/audit/merged", OK, "")
  }

}
