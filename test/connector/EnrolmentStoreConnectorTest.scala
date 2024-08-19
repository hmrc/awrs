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

package connector

import connectors.EnrolmentStoreConnector
import models.AwrsUsers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.BaseSpec

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreConnectorTest extends BaseSpec with AnyWordSpecLike {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  trait Setup extends ConnectorTest {
    object TestEnrolmentStoreConnector extends EnrolmentStoreConnector(mockAuditConnector, mockHttpClient, config, "awrs")
  }

  val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  "Enrolment store connector" must {
    "Return a list of userIDs when given an awrs reference" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val awrsRef = "XAAW00000120001"
      val testAwrsUsers: AwrsUsers = AwrsUsers(List("api10"), Nil)

//      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.contains(s"""HMRC-AWRS-ORG~AWRSRefNumber~$awrsRef"""),
//        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
//        ArgumentMatchers.any(), ArgumentMatchers.any())).
//        thenReturn(Future.successful(HttpResponse(OK, Json.toJson(testAwrsUsers), Map.empty[String, Seq[String]])))

      when(executeGet[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, Json.toJson(testAwrsUsers), Map.empty[String, Seq[String]])))

      val result: Future[Either[Int, AwrsUsers]] = TestEnrolmentStoreConnector.getAWRSUsers(awrsRef)(hc)
      await(result) should be(Right(testAwrsUsers))
    }

    "Return no content when given an awrs enrolment and there are no users in enrolment store" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val awrsRef = "NO-CONTENT-AWRS-REF"
      val testEmptyAwrsUsers: AwrsUsers = AwrsUsers(Nil, Nil)

//      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.contains(s"""HMRC-AWRS-ORG~AWRSRefNumber~$awrsRef"""),
//        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
//        ArgumentMatchers.any(), ArgumentMatchers.any())).
//        thenReturn(Future.successful(HttpResponse(NO_CONTENT, Json.toJson(testEmptyAwrsUsers), Map.empty[String, Seq[String]])))

      when(executeGet[HttpResponse]).thenReturn(Future.successful(HttpResponse(NO_CONTENT, Json.toJson(testEmptyAwrsUsers), Map.empty[String, Seq[String]])))

      val result: Future[Either[Int, AwrsUsers]] = TestEnrolmentStoreConnector.getAWRSUsers(awrsRef)(hc)
      await(result) should be(Right(testEmptyAwrsUsers))

    }

    "Return an error when given an invalid enrolment key" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val awrsRef = "ERROR-AWRS-REF"

//      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.contains(s"""HMRC-AWRS-ORG~AWRSRefNumber~$awrsRef"""),
//        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
//        ArgumentMatchers.any(), ArgumentMatchers.any())).
//        thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "Some Bad Json", Map.empty[String, Seq[String]])))

      when(executeGet[HttpResponse]).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "Some Bad Json", Map.empty[String, Seq[String]])))


      val result: Future[Either[Int, AwrsUsers]] = TestEnrolmentStoreConnector.getAWRSUsers(awrsRef)(hc)
      await(result) should be(Left(BAD_REQUEST))

    }
  }
}
