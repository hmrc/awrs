/*
 * Copyright 2023 HM Revenue & Customs
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


import connectors.{EnrolmentStoreConnector, EtmpConnector, HipConnector}
import metrics.AwrsMetrics
import models._
import org.mockito.ArgumentMatchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AwrsTestJson._
import utils.{AWRSFeatureSwitches, BaseSpec, FeatureSwitch}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceTest extends BaseSpec with AnyWordSpecLike {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val mockhipconnector: HipConnector = mock[HipConnector]
  val mockEnrolmentStoreConnector: EnrolmentStoreConnector = mock[EnrolmentStoreConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  val inputJson: JsValue = api4EtmpLTDJson
  val safeId = "XA0001234567890"
  val successResponse: JsValue = Json.parse(
    s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "$testRefNo"}""")
  val ggEnrolResponse: JsValue = Json.parse( """{}""")
  val failureResponse: JsValue = Json.parse( """{"Reason": "Resource not found"}""")
  val address: BCAddressApi3 = BCAddressApi3(addressLine1 = "", addressLine2 = "")
  val updatedData = new UpdateRegistrationDetailsRequest(None, false, Some(Organisation("testName")), address, ContactDetails(), false, false)
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  object TestSubscriptionServiceEMAC extends SubscriptionService(
    app.injector.instanceOf[AwrsMetrics],
    mockEnrolmentStoreConnector,
    mockEtmpConnector,
    mockhipconnector
  )

  "Subscription Service with EMAC switched on" must {
    behave like subscriptionServicesPart1(TestSubscriptionServiceEMAC)
    behave like subscriptionServicesPart2EMAC(TestSubscriptionServiceEMAC)
  }

  def subscriptionServicesPart2EMAC(testSubscriptionService: => SubscriptionService): Unit = {
    FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

    "subscribe when we are passed valid json and the subscription and enrolment creation are successful" in {
      when(mockEtmpConnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))
      when(mockEnrolmentStoreConnector.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, ggEnrolResponse, Map.empty[String, Seq[String]])))

      val result = testSubscriptionService.subscribe(inputJson, safeId, Some(testUtr), "SOP", "postcode")
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe successResponse
    }

    "respond with BAD_REQUEST, when subscription works but enrolment store connector request fails with a Bad request but audit the Bad request" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      when(mockEtmpConnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))
      when(mockEnrolmentStoreConnector.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureResponse, Map.empty[String, Seq[String]])))
      val result = testSubscriptionService.subscribe(inputJson, safeId, Some(testUtr), "SOP", "postcode")
      val response = await(result)
      response.status shouldBe BAD_REQUEST
    }
  }

  def subscriptionServicesPart1(testSubscriptionService: => SubscriptionService): Unit = {

    "respond with BadRequest, when subscription request fails with a Bad request" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      when(mockEtmpConnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureResponse, Map.empty[String, Seq[String]])))
      val result = testSubscriptionService.subscribe(inputJson, safeId, Some(testUtr), "SOP", "postcode")
      val response = await(result)
      response.status shouldBe BAD_REQUEST
      response.json shouldBe failureResponse
    }

    "respond with Ok, when a valid update subscription json is supplied" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      when(mockEtmpConnector.updateSubscription(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, api6SuccessResponseJson, Map.empty[String, Seq[String]])))
      val result = testSubscriptionService.updateSubscription(inputJson, testRefNo)
      val response = await(result)
      response.status shouldBe OK
    }

    "respond with BadRequest when update subscription json is invalid" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      when(mockEtmpConnector.updateSubscription(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureResponse, Map.empty[String, Seq[String]])))
      val result = testSubscriptionService.updateSubscription(inputJson, testRefNo)
      val response = await(result)
      response.status shouldBe BAD_REQUEST
    }

    "respond with Ok, when a valid update Group Partner registration json is supplied" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      val updateSuccessResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z"}""")
      when(mockEtmpConnector.updateGrpRepRegistrationDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, updateSuccessResponse, Map.empty[String, Seq[String]])))
      val result = testSubscriptionService.updateGrpRepRegistrationDetails(testSafeId, updatedData)
      val response = await(result)
      response.status shouldBe OK
    }

    "respond with BadRequest when update Group Partner registration json is invalid" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      when(mockEtmpConnector.updateGrpRepRegistrationDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureResponse, Map.empty[String, Seq[String]])))
      val result = testSubscriptionService.updateGrpRepRegistrationDetails(testSafeId, updatedData)
      val response = await(result)
      response.status shouldBe BAD_REQUEST
    }
  }

  "subscriptionService.subscribe" must {
    "return OK when valid json is passed with feature flag on" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      when(mockhipconnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val result = TestSubscriptionServiceEMAC.subscribe(inputJson, safeId, Some(testUtr), "SOP", "postcode")
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe successResponse
    }

    "return BAD_REQUEST when invalid json is passed with feature flag on" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      when(mockhipconnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureResponse, Map.empty[String, Seq[String]])))

      val result = TestSubscriptionServiceEMAC.subscribe(inputJson, safeId, Some(testUtr), "SOP", "postcode")
      val response = await(result)
      response.status shouldBe BAD_REQUEST
      response.json shouldBe failureResponse
    }
  }
}
