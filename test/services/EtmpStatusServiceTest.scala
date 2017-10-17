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

import connectors.EtmpConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import utils.AwrsTestJson.testRefNo
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.http.logging.SessionId

class EtmpStatusServiceTest extends UnitSpec with OneServerPerSuite with MockitoSugar {

  implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockEtmpConnector = mock[EtmpConnector]

  object TestEtmpStatusService extends EtmpStatusService {
    override val etmpConnector = mockEtmpConnector
  }

  "EtmpStatusService " should {
    "use the correct connector" in {
      EtmpStatusService.etmpConnector shouldBe EtmpConnector
    }

    "successfully lookup application status when passed a valid reference number" in {
      val awrsRefNo = testRefNo
      when(mockEtmpConnector.checkStatus(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
      val result = TestEtmpStatusService.checkStatus(awrsRefNo)
      await(result).status shouldBe 200
    }

    "return Bad Request when passed an invalid reference number" in {
      val invalidAwrsRefNo = "AAW00000123456"
      when(mockEtmpConnector.checkStatus(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
      val result = TestEtmpStatusService.checkStatus(invalidAwrsRefNo)
      await(result).status shouldBe 400
    }
  }
}
