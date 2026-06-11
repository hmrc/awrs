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

import connectors.HipConnector
import org.mockito.ArgumentMatchers
  import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.*
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.BaseSpec

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EtmpStatusServiceTest extends BaseSpec with AnyWordSpecLike {

  given hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  given config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  val mockHipConnector: HipConnector = mock[HipConnector]

  object TestEtmpStatusService extends EtmpStatusService(mockHipConnector)

  "EtmpStatusService " must {

    val awrsRefNo = "XAAW0000010001"

    "return OK and strip success node when HIP feature switch is enabled and HIP returns 200" in {

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

      when(mockHipConnector.checkStatus(ArgumentMatchers.eq(awrsRefNo))(using ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, responseJson, Map.empty[String, Seq[String]])))

      val result = TestEtmpStatusService.checkStatus(awrsRefNo)
      await(result).status shouldBe OK
    }

    "return failure response when HIP feature switch is enabled and HIP returns non-200" in {

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

      when(mockHipConnector.checkStatus(ArgumentMatchers.eq(awrsRefNo))(using ArgumentMatchers.any()))
        .thenReturn(Future.successful(mockResponse))

      val result = await(TestEtmpStatusService.checkStatus(awrsRefNo))

      result.status mustBe Status.INTERNAL_SERVER_ERROR
      Json.parse(result.body) mustBe responseJson
    }
  }
}
