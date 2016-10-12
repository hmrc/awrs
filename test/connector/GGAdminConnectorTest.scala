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

package connector

import java.util.UUID

import connectors.GovernmentGatewayAdminConnector
import models.{KnownFact, KnownFactsForService}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.Play
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson

import scala.concurrent.Future
import utils.AwrsTestJson.testRefNo

class GGAdminConnectorTest extends UnitSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter with AwrsTestJson {

  object TestAuditConnector extends AuditConnector with AppName with RunMode {
    override lazy val auditingConfig = LoadAuditingConfig("auditing")
  }

  class MockHttp extends WSGet with WSPost with HttpAuditing{
    override val hooks = Seq(AuditingHook)
    override def auditConnector: AuditConnector = TestAuditConnector
    override def appName = Play.configuration.getString("appName").getOrElse("awrs")
  }

  val mockWSHttp = mock[MockHttp]

  object TestGGAdminConnector extends GovernmentGatewayAdminConnector {
    override val http = mockWSHttp
  }

  before {
    reset(mockWSHttp)
  }

  "GGAdminConnector" should {
    "for a successful submission, return 200 response" in {

      val knownFact = KnownFactsForService(List(KnownFact("AWRS-REF-NO",testRefNo)))
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = None)))
      val result = TestGGAdminConnector.addKnownFacts(knownFact)
      await(result).status shouldBe 200
    }

  }
}
