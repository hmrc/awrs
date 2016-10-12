/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.EtmpConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import utils.AwrsTestJson.testRefNo

class EtmpStatusInfoServiceTest extends UnitSpec with OneServerPerSuite with MockitoSugar {

  implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockEtmpConnector = mock[EtmpConnector]

  object TestEtmpStatusInfoService extends EtmpStatusInfoService {
    override val etmpConnector = mockEtmpConnector
  }

  "TestEtmpStatusInfoService " should {
    "use the correct connector" in {
      EtmpStatusInfoService.etmpConnector shouldBe connectors.EtmpConnector
    }

    "successfully lookup status info when passed a valid reference number and contact number" in {
      val awrsRefNo = testRefNo
      val contactNumber = "0123456789"
      when(mockEtmpConnector.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
      val result = TestEtmpStatusInfoService.getStatusInfo(awrsRefNo, contactNumber)
      await(result).status shouldBe 200
    }

    "return Bad Request when passed an invalid reference number and contact number" in {
      val invalidAwrsRefNo = "AAW00000123456"
      val contactNumber = "0123456789"
      when(mockEtmpConnector.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
      val result = TestEtmpStatusInfoService.getStatusInfo(invalidAwrsRefNo, contactNumber)
      await(result).status shouldBe 400
    }
  }

}
