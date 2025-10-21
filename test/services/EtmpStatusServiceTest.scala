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

import connectors.{EtmpConnector, HipConnector}
import org.mockito.ArgumentMatchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import utils.{AWRSFeatureSwitches, BaseSpec, FeatureSwitch}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class EtmpStatusServiceTest extends BaseSpec with AnyWordSpecLike {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]

  object TestEtmpStatusService extends EtmpStatusService(mockEtmpConnector, mockHipConnector)

  "EtmpStatusService " must {
    val awrsRefNo = "XAAW0000010001"
    "successfully lookup application status when passed a valid reference number" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())
      when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "", Map.empty[String, Seq[String]])))
      val result = TestEtmpStatusService.checkStatus(awrsRefNo)
      await(result).status shouldBe 200
    }

    "return Bad Request when passed an invalid reference number" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())
      val invalidAwrsRefNo = "AAW00000123456"
      when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "", Map.empty[String, Seq[String]])))
      val result = TestEtmpStatusService.checkStatus(invalidAwrsRefNo)
      await(result).status shouldBe 400
    }

    "return OK and strip success node when HIP feature switch is enabled and HIP returns 200" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z",
          |    "formBundleStatus": "Pending",
          |    "groupBusinessPartner": "false",
          |    "safeid": "XA0000123456789"
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.checkStatus(ArgumentMatchers.eq(awrsRefNo))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, responseJson, Map.empty[String, Seq[String]])))

      val result = TestEtmpStatusService.checkStatus(awrsRefNo)
      await(result).status shouldBe OK
    }

    "return failure response when HIP feature switch is enabled and HIP returns non-200" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson = Json.parse(
        """
          |{
          |  "error": {
          |    "code": "500",
          |    "message": "Internal Server Error",
          |    "logID": "24B56DEABD748EB11C66897AB601D222"
          |  }
          |}
        """.stripMargin)

      val mockResponse = HttpResponse(Status.INTERNAL_SERVER_ERROR, responseJson, Map("correlationid" -> Seq("123e4567-e89b-12d3-a456-426614174000")))

      when(mockHipConnector.checkStatus(ArgumentMatchers.eq(awrsRefNo))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(mockResponse))

      val result = await(TestEtmpStatusService.checkStatus(awrsRefNo))

      result.status mustBe Status.INTERNAL_SERVER_ERROR
      Json.parse(result.body) mustBe responseJson
    }
  }
}
