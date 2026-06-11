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

import connectors.{EnrolmentStoreConnector, DesConnector, HipConnector}
import metrics.AwrsMetrics
import models.*
import org.mockito.ArgumentMatchers
  import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.test.Helpers.*
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AwrsTestJson.*
import utils.BaseSpec

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionServiceTest extends BaseSpec with AnyWordSpecLike {
  
  given config: ServicesConfig = app.injector.instanceOf[ServicesConfig]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]
  val mockEnrolmentStoreConnector: EnrolmentStoreConnector =
    mock[EnrolmentStoreConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val inputJson: JsValue = api4EtmpLTDJson
  val inputJsonUpdate: JsValue = api6RequestUpdateJsonWithAck
  val inputJsonWithAckRemovedUpdate: JsValue = ackRemovedJson
  val hipApi4InputLTDNewBusinessJson: JsValue = api4hipLTDNewBusinessJson
  val hipApi4InputSoleTraderNewBusinessJson: JsValue = api4hipSoleTraderNewBusinessJson
  val hipApi4InputCorporateBodyNewBusinessJson: JsValue = api4hipCorporateBodyNewBusinessJson
  val hipSuccessfulResponse: JsValue = api4hipSuccessfulResponseJson

  val safeId = "XA0001234567890"
  val successResponse: JsValue = Json.parse(
    s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "$testRefNo"}"""
  )
  val ggEnrolResponse: JsValue = Json.parse("""{}""")
  val failureResponse: JsValue =
    Json.parse("""{"Reason": "Resource not found"}""")
  val address: BCAddressApi3 =
    BCAddressApi3(addressLine1 = "", addressLine2 = "")
  val updatedData = new UpdateRegistrationDetailsRequest(
    None,
    false,
    Some(Organisation("testName")),
    address,
    ContactDetails(),
    false,
    false
  )
  
  given hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val testSubscriptionService: SubscriptionService = new SubscriptionService(
    app.injector.instanceOf[AwrsMetrics],
    mockEnrolmentStoreConnector,
    mockDesConnector,
    mockHipConnector
  )

  "SubscriptionService.updateSubscription" when {

    "HipConnector switch is enabled" must {
      "respond with Ok, when a valid update subscription json is supplied" in {
        when(
          mockHipConnector.updateSubscription(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )(using ArgumentMatchers.any())
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                api6SuccessResponseHipJson,
                Map.empty[String, Seq[String]]
              )
            )
          )
        val result =
          testSubscriptionService.updateSubscription(inputJsonUpdate, testRefNo)
        val response = await(result)
        response.status shouldBe OK
      }

      "respond with ok when valid update subscription json is supplied (Sole Trader)" in {
        when(
          mockHipConnector.updateSubscription(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )(using ArgumentMatchers.any())
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                api6SuccessResponseHipJson,
                Map.empty[String, Seq[String]]
              )
            )
          )
        val result =
          testSubscriptionService.updateSubscription(hipApi4InputSoleTraderNewBusinessJson, testRefNo)
        val response = await(result)
        response.status shouldBe OK
      }

      "repond with ok when valid update subscription json is supplied (Corporate Body)" in {
        when(
          mockHipConnector.updateSubscription(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )(using ArgumentMatchers.any())
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                api6SuccessResponseHipJson,
                Map.empty[String, Seq[String]]
              )
            )
          )
        val result =
          testSubscriptionService.updateSubscription(hipApi4InputCorporateBodyNewBusinessJson, testRefNo)
        val response = await(result)
        response.status shouldBe OK
      }

      "respond with BadRequest when response status from Hip is BAD_REQUEST" in {
        when(
          mockHipConnector.updateSubscription(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )(using ArgumentMatchers.any())
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_REQUEST,
                api6FailureResponseJson,
                Map.empty[String, Seq[String]]
              )
            )
          )
        val result =
          testSubscriptionService.updateSubscription(inputJsonUpdate, testRefNo)
        val response = await(result)
        response.status shouldBe BAD_REQUEST
      }

      "return BAD_REQUEST when JSON transformation fails" in {
        val invalidJson = Json.parse("""["invalid","json"]""")

        val result = testSubscriptionService.updateSubscription(invalidJson, safeId)
        val response = await(result)

        response.status shouldBe Status.BAD_REQUEST
        response.body should include("JSON transformation failed")
      }

      "respond with appropriate failure status code for an update request" in {

        val errorStatusMap = Map(
          400 -> "Bad Request",
          401 -> "Unauthorized",
          403 -> "Forbidden",
          404 -> "Not Found",
          415 -> "Unsupported media-type",
          500 -> "Internal Server Error"
        )

        errorStatusMap.foreach { error =>
          when(
            mockHipConnector.updateSubscription(
              ArgumentMatchers.any(),
              ArgumentMatchers.any()
            )(using ArgumentMatchers.any())
          )
            .thenReturn(
              Future.successful(
                HttpResponse(error._1, error._2, Map.empty[String, Seq[String]])
              )
            )

          val result = testSubscriptionService.updateSubscription(
            inputJsonWithAckRemovedUpdate,
            testRefNo
          )
          await(result).status shouldBe error._1
        }
      }
    }
  }

  "subscriptionService.subscribe" when {
    "feature flag is on and hip connector is enabled" must {
      "return CREATED when valid json is passed" in {

        when(mockHipConnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(CREATED, hipSuccessfulResponse, Map.empty[String, Seq[String]])))
        when(mockEnrolmentStoreConnector.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(
            Future.successful(HttpResponse(NO_CONTENT, ggEnrolResponse, Map.empty[String, Seq[String]]))
          )

        val result = testSubscriptionService.subscribe(inputJson, safeId, Some(testUtr), "SOP", "postcode")
        val response = await(result)
        response.status shouldBe CREATED
        response.json shouldBe successResponse
      }

      "return BAD_REQUEST when invalid json is passed with feature flag on" in {
        when(mockHipConnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureResponse, Map.empty[String, Seq[String]])))
        val result = testSubscriptionService.subscribe(hipApi4InputLTDNewBusinessJson, safeId, Some(testUtr), "LTD", "postcode")
        val response = await(result)
        response.status shouldBe BAD_REQUEST
        response.json shouldBe failureResponse
      }

      "return success when creating new sole trader business" in {
        when(mockHipConnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(CREATED, hipSuccessfulResponse, Map.empty)))

        when(mockEnrolmentStoreConnector.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, ggEnrolResponse, Map.empty)))

        val result = testSubscriptionService.subscribe(hipApi4InputSoleTraderNewBusinessJson, safeId, Some(testUtr), "SOP", "postcode")
        val response = await(result)

        response.status shouldBe CREATED
      }

      "return success when creating new corporate body business" in {
        when(mockHipConnector.subscribe(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(CREATED, hipSuccessfulResponse, Map.empty)))

        when(mockEnrolmentStoreConnector.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, ggEnrolResponse, Map.empty)))

        val result = testSubscriptionService.subscribe(hipApi4InputCorporateBodyNewBusinessJson, safeId, Some(testUtr), "CRN", "postcode")
        val response = await(result)

        response.status shouldBe CREATED
      }
    }
  }
  "subscriptionService.updateRequestForHip" must {
    "remove the acknowledgementReference field from the request json" in {
      val resultJson =
        testSubscriptionService.updateRequestForHip(inputJsonUpdate)
      resultJson shouldBe JsSuccess(inputJsonWithAckRemovedUpdate)
    }

    "return original json if no acknowledgementReference field is found" in {
      val resultJson = testSubscriptionService.updateRequestForHip(
        inputJsonWithAckRemovedUpdate
      )
      resultJson shouldBe JsSuccess(inputJsonWithAckRemovedUpdate)
    }
  }
}
