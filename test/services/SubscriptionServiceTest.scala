/*
 * Copyright 2017 HM Revenue & Customs
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

package services

import java.util.UUID

import connectors.{EtmpConnector, GovernmentGatewayAdminConnector}
import metrics.AwrsMetrics
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson

import scala.concurrent.Future
import utils.AwrsTestJson._

class SubscriptionServiceTest extends UnitSpec with OneServerPerSuite with MockitoSugar with AwrsTestJson {
  val mockEtmpConnector = mock[EtmpConnector]
  val mockggAdminConnector = mock[GovernmentGatewayAdminConnector]

  object TestSubscriptionService extends SubscriptionService {
    override val ggAdminConnector = mockggAdminConnector
    override val etmpConnector = mockEtmpConnector
    override val metrics = AwrsMetrics
  }

  "Subscription Service" should {
    val inputJson = api4EtmpLTDJson
    val safeId = "XA0001234567890"
    val successResponse = Json.parse( s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "$testRefNo"}""")
    val ggEnrolResponse = Json.parse( """{}""")
    val failureResponse = Json.parse( """{"Reason": "Resource not found"}""")
    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

    "use the correct Connectors" in {
      SubscriptionService.etmpConnector shouldBe EtmpConnector
      SubscriptionService.ggAdminConnector shouldBe GovernmentGatewayAdminConnector
    }

    "subscribe when we are passed valid json" in {
      when(mockEtmpConnector.subscribe(Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      when(mockggAdminConnector.addKnownFacts(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val result = TestSubscriptionService.subscribe(inputJson,safeId, Some(testUtr), "SOP","postcode")
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe successResponse
    }

    "respond with BadRequest, when subscription request fails with a Bad request" in {
      when(mockEtmpConnector.subscribe(Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(failureResponse))))
      val result = TestSubscriptionService.subscribe(inputJson,safeId, Some(testUtr), "SOP","postcode")
      val response = await(result)
      response.status shouldBe BAD_REQUEST
      response.json shouldBe failureResponse
    }

    "respond with Ok, when subscription works but gg admin request fails with a Bad request but audit the Bad request" in {
      when(mockEtmpConnector.subscribe(Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      when(mockggAdminConnector.addKnownFacts(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(failureResponse))))
      val result = TestSubscriptionService.subscribe(inputJson,safeId, Some(testUtr), "SOP","postcode")
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe successResponse
    }

    "respond with Ok, when a valid update subscription json is supplied" in {
      when(mockEtmpConnector.updateSubscription(Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api6SuccessResponseJson))))
      val result = TestSubscriptionService.updateSubcription(inputJson, testRefNo)
      val response = await(result)
      response.status shouldBe OK
    }

    "respond with BadRequest when update subscription json is invalid" in {
      when(mockEtmpConnector.updateSubscription(Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(failureResponse))))
      val result = TestSubscriptionService.updateSubcription(inputJson, testRefNo)
      val response = await(result)
      response.status shouldBe BAD_REQUEST
    }
  }

}
