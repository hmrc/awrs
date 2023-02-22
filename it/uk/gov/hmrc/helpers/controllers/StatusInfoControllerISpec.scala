/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.helpers.controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.routes
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.utils.Stubs
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}

class StatusInfoControllerISpec extends IntegrationSpec with AuthHelpers with Matchers with Stubs{

  val testSafeId = "testSafeId123"
  val testCredId = "testCredId123"
  val controllerUrl: String = routes.StatusInfoController.checkUsersEnrolment(testSafeId, testCredId).url
  val listOfCredIds: String = "{principalIDs: 123, delegatedID: 123}"
  override val enrolmentRef = "XAAW00000123456"
  override val enrolmentKey = s"HMRC-AWRS-ORG~AWRSRefNumber~$enrolmentRef"

  def stubCheckAwrsEnrolment(status: Int): StubMapping = {
    stubbedGet(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", status, listOfCredIds)
  }

  def stubGetEtmpBusinessDetails(status: Int): StubMapping = {
    stubbedGet(s"""/registration/details?safeid=testSafeId123&regime=AWRS""", status, listOfCredIds)
  }

  "checkUsersEnrolment" should {
    "return a 200 OK when the credId is present in the list of AWRS user enrolments" in {
      //stubbed enrolment store call
      //stubbed get business details call
      stubAuditPosts
      stubCheckAwrsEnrolment(OK)
      stubGetEtmpBusinessDetails(OK)

      val resp: WSResponse = await(authorisedClient(controllerUrl).get)
      resp.status mustBe 200
    }
  }
}
