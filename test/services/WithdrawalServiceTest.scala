/*
 * Copyright 2019 HM Revenue & Customs
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
import metrics.AwrsMetrics
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.BaseSpec

import scala.concurrent.Future

class WithdrawalServiceTest extends BaseSpec {
  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]

  object TestWithdrawalService extends WithdrawalService(app.injector.instanceOf[AwrsMetrics], mockEtmpConnector)

  "Withdrawal Service" should {
    val awrsRefNo = "XAAW0000010001"
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

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
