/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.EtmpConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import java.util.UUID
import scala.concurrent.Future

class EtmpStatusInfoServiceTest extends BaseSpec with WordSpecLike with MustMatchers {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]

  object TestEtmpStatusInfoService extends EtmpStatusInfoService(mockEtmpConnector)

  "TestEtmpStatusInfoService " must {

    "successfully lookup status info when passed a valid reference number and contact number" in {
      val awrsRefNo = testRefNo
      val contactNumber = "0123456789"
      when(mockEtmpConnector.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "", Map.empty[String, Seq[String]])))
      val result = TestEtmpStatusInfoService.getStatusInfo(awrsRefNo, contactNumber)
      await(result).status shouldBe 200
    }

    "return Bad Request when passed an invalid reference number and contact number" in {
      val invalidAwrsRefNo = "AAW00000123456"
      val contactNumber = "0123456789"
      when(mockEtmpConnector.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "", Map.empty[String, Seq[String]])))
      val result = TestEtmpStatusInfoService.getStatusInfo(invalidAwrsRefNo, contactNumber)
      await(result).status shouldBe 400
    }
  }
}
