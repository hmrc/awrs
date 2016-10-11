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
import metrics.Metrics
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson

import scala.concurrent.Future

class WithdrawalServiceTest extends UnitSpec with OneServerPerSuite with MockitoSugar with AwrsTestJson {
  val mockEtmpConnector = mock[EtmpConnector]

  object TestWithdrawalService extends WithdrawalService {
    override val etmpConnector = mockEtmpConnector
    override val metrics = Metrics
  }

  "Withdrawal Service" should {
    val awrsRefNo = "XAAW0000010001"
    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

    "use the correct Connectors" in {
      SubscriptionService.etmpConnector shouldBe EtmpConnector
    }

    "perform a withdrawal when passed valid json" in {
      when(mockEtmpConnector.withdrawal(Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api8SuccessfulResponseJson))))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe api8SuccessfulResponseJson
    }

    "respond with BadRequest, when withdrawal request fails with a Bad request" in {
      when(mockEtmpConnector.withdrawal(Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(api8FailureResponseJson))))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe BAD_REQUEST
      response.json shouldBe api8FailureResponseJson
    }
    
  }

}
