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

package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.AwrsUsers
import org.scalatest.matchers.must.Matchers
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.utils.Stubs
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}

class StatusInfoControllerISpec extends IntegrationSpec with AuthHelpers with Matchers with Stubs {

  val testSafeId = "testSafeId123"
  val testCredId = "awrsUserCredId"
  val controllerUrl: String = routes.StatusInfoController.enrolledUsers(testSafeId).url
  val testBusinessDetails: String = etmpBusinessDetailsData
  val testAwrsUsers: String = Json.toJson(AwrsUsers(List("awrsUserCredId"), Nil)).toString
  override val enrolmentRef = "XAAW00000123456"
  override val enrolmentKey = s"""HMRC-AWRS-ORG~AWRSRefNumber~$enrolmentRef"""
  val usersJson = """{"principalUserIds":["awrsUserCredId"],"delegatedUserIds":[]}"""
  val emptyUsersJson = """{"principalUserIds":[],"delegatedUserIds":[]}"""

  def stubGetEnrolledUsers(status: Int): StubMapping = {
    stubbedGetUrlEqual(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users", status, testAwrsUsers)
  }

  def stubGetEtmpBusinessDetails(status: Int, businessDetails: String): StubMapping = {
    stubbedGetUrlEqual(s"""/registration/details?safeid=$testSafeId&regime=AWRS""", status, businessDetails)
  }

  "checkUsersEnrolment" should {
    "return a 200 OK containing true when the user already has an AWRS account" in {
      stubAuditPosts
      stubGetEnrolledUsers(OK)
      stubGetEtmpBusinessDetails(OK, testBusinessDetails)

      val resp: WSResponse = await(authorisedClient(controllerUrl).get())
      resp.status mustBe OK
      resp.body mustBe usersJson
    }
    "return a 204 containing false when the user does not have an AWRS account" in {
      stubAuditPosts
      stubGetEnrolledUsers(NO_CONTENT)
      stubGetEtmpBusinessDetails(OK, testBusinessDetails)

      val resp: WSResponse = await(authorisedClient(controllerUrl).get())
      resp.status mustBe OK
      resp.body mustBe emptyUsersJson
    }
    "return a BAD_REQUEST when check AWRS users enrolment returns a BAD REQUEST" in {
      stubAuditPosts
      stubGetEnrolledUsers(BAD_REQUEST)
      stubGetEtmpBusinessDetails(OK, testBusinessDetails)

      val resp: WSResponse = await(authorisedClient(controllerUrl).get())
      resp.status mustBe BAD_REQUEST
      resp.body mustBe s"""Error when checking enrolment store for $enrolmentRef"""
    }
    "return a NOT_FOUND when no business details are found" in {
      stubAuditPosts
      stubGetEtmpBusinessDetails(OK, "")

      val resp: WSResponse = await(authorisedClient(controllerUrl).get())
      resp.status mustBe NOT_FOUND
      resp.body mustBe s"""AWRS enrolled Business Details not found for $testSafeId"""
    }
  }
}
