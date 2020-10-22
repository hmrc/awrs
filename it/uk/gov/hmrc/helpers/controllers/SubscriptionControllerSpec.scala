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

import akka.util.Timeout
import models.Approved
import org.scalatest.MustMatchers
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.utils.{IntegrationData, Stubs}
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}
import utils.{AWRSFeatureSwitches, FeatureSwitch}

import scala.concurrent.duration.FiniteDuration

class SubscriptionControllerSpec extends IntegrationSpec with AuthHelpers with MustMatchers with IntegrationData with Stubs {

  val secondsToWait: Int = 20

  val controllerUrl = "/awrs/send-data"

  "subscribe()" should {
    "return 200" when {
      "EMAC feature switch is true" when {
        "the etmp subscription is successful" when {
          "the enrolment upsert is successful" when {
            "the user is an Organisation" in {

              FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
              stubShowAndRedirectExternalCalls("Organisation")
              stubAuditPosts
              stubAwrsSubscriptionResponse(OK, Some(subscriptionSuccessResponse))
              stubUpsertAwrsEnrolment(NO_CONTENT)

              val resp: WSResponse = await(client(controllerUrl).post(awrsSubscriptionDataOrg))
              resp.status mustBe 200
            }
          }

          "the enrolment upsert is successful" when {
            "the user is an Individual" in {

              FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
              stubShowAndRedirectExternalCalls("Individual")
              stubAuditPosts
              stubAwrsSubscriptionResponse(OK, Some(subscriptionSuccessResponse))
              stubUpsertAwrsEnrolment(NO_CONTENT)

              val resp: WSResponse = await(client(controllerUrl).post(awrsSubscriptionDataInd))
              resp.status mustBe 200
            }
          }
        }
      }
    }

    "return 202 for a successful self heal" when {
      "EMAC feature switch is true" when {
        "the awrs subscription failed because it already exists" when {
          "Organisation details match business partner" in {

            FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
            stubShowAndRedirectExternalCalls("Organisation")
            stubAuditPosts
            stubAwrsSubscriptionResponse(BAD_REQUEST)
            stubEtmpRegistrationResponse(OK, etmpRegistrationResultOrg)
            stubGetSubscriptionStatus(OK, Some(Approved.name))
            stubUpsertAwrsEnrolment(NO_CONTENT)

            val resp: WSResponse = await(client(controllerUrl).post(awrsSubscriptionDataOrg))
            resp.status mustBe 202
          }

          "Individual details match the business partner" in {

            FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
            stubShowAndRedirectExternalCalls("Individual")
            stubAuditPosts
            stubEtmpRegistrationResponse(OK, etmpRegistrationResultInd)
            stubGetSubscriptionStatus(OK, Some(Approved.name))
            stubAwrsSubscriptionResponse(BAD_REQUEST)
            stubUpsertAwrsEnrolment(NO_CONTENT)

            val resp: WSResponse = await(client(controllerUrl).post(awrsSubscriptionDataInd))
            resp.status mustBe 202
          }
        }
      }
    }

    "return 400" when {

      "EMAC feature switch is false" when {
        "the awrs subscription fails" in {

          FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())
          stubAuditPosts
          stubAwrsSubscriptionResponse(BAD_REQUEST)

          val resp: WSResponse = await(client(controllerUrl).post(awrsSubscriptionDataOrg))
          resp.status mustBe 400
        }
      }

      "EMAC feature switch is true" when {
        "the awrs subscription failed because it already exists" when {
          "self heal cannot be attempted because Organisation data does not match business partner" in {

            val modifiedRegistrationResult = etmpRegistrationResultOrg.replace("Trading Trade", "Wrong Org Name")

            FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
            stubShowAndRedirectExternalCalls("Organisation")
            stubAuditPosts
            stubEtmpRegistrationResponse(OK, modifiedRegistrationResult)
            stubAwrsSubscriptionResponse(BAD_REQUEST)
            stubGetSubscriptionStatus(OK, Some(Approved.name))
            stubUpsertAwrsEnrolment(NO_CONTENT)

            val resp: WSResponse = await(client(controllerUrl).post(awrsSubscriptionDataOrg))
            resp.status mustBe 400
          }


          "self heal cannot be attempted because Individual data does not match business partner" in {

            val modifiedRegistrationResult = etmpRegistrationResultInd.replace("firstName", "wrong first name")

            FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
            stubShowAndRedirectExternalCalls("Individual")
            stubAuditPosts
            stubEtmpRegistrationResponse(OK, modifiedRegistrationResult)
            stubAwrsSubscriptionResponse(BAD_REQUEST)
            stubGetSubscriptionStatus(OK, Some(Approved.name))
            stubUpsertAwrsEnrolment(NO_CONTENT)

            val resp = await(
              client(controllerUrl).post(awrsSubscriptionDataInd))(Timeout(FiniteDuration(secondsToWait, "seconds")
            ))

            resp.status mustBe 400
          }
        }
      }
    }
  }
}