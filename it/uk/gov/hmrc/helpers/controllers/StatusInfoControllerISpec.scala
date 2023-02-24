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
import models.AwrsUsers
import org.scalatest.matchers.must.Matchers
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.utils.Stubs
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}

class StatusInfoControllerISpec extends IntegrationSpec with AuthHelpers with Matchers with Stubs {

  val testSafeId = "testSafeId123"
  val testCredId = "testCredId123"
  val controllerUrl: String = routes.StatusInfoController.checkUsersEnrolment(testSafeId, testCredId).url
  val listOfCredIds: String = """{"principalIDs": ["123"], "delegatedID": ["123"]}"""
  val testBusinessDetails: String = etmpBusinessDetailsData
  val testAwrsUsers = Json.toJson(AwrsUsers(List("api10"), Nil)).toString
  override val enrolmentRef = "XAAW00000123456"
  override val enrolmentKey = s"HMRC-AWRS-ORG~AWRSRefNumber~$enrolmentRef"

  def stubCheckAwrsEnrolment(status: Int): StubMapping = {
    stubbedGetUrlEqual(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users", status, testAwrsUsers)
  }

  def stubGetEtmpBusinessDetails(status: Int, businessDetails: String): StubMapping = {
    stubbedGetUrlEqual("/registration/details?safeid=testSafeId123&regime=AWRS", status, businessDetails)
  }

  "checkUsersEnrolment" should {
    "return a 200 OK when the credId is present in the list of AWRS user enrolments" in {
      //stubbed enrolment store call
      //stubbed get business details call
      stubAuditPosts
      stubCheckAwrsEnrolment(OK)
      stubGetEtmpBusinessDetails(OK, testBusinessDetails)

      val resp: WSResponse = await(authorisedClient(controllerUrl).get)
      resp.status mustBe OK
    }
    "return a 204 when the credID is not present" in {
      stubAuditPosts
      stubCheckAwrsEnrolment(NO_CONTENT)
      stubGetEtmpBusinessDetails(OK, testBusinessDetails)

      val resp: WSResponse = await(authorisedClient(controllerUrl).get)
      resp.status mustBe OK
    }
    "return a BAD_REQUEST when check awrs users enrolment returns a BAD REQUEST" in {
      stubAuditPosts
      stubCheckAwrsEnrolment(BAD_REQUEST)
      stubGetEtmpBusinessDetails(OK, testBusinessDetails)

      val resp: WSResponse = await(authorisedClient(controllerUrl).get)
      resp.status mustBe BAD_REQUEST
    }
    "return a NOT_FOUND when no business details are found" in {
      stubAuditPosts
      stubGetEtmpBusinessDetails(OK, "")

      val resp: WSResponse = await(authorisedClient(controllerUrl).get)
      resp.status mustBe NOT_FOUND
    }
  }
}
