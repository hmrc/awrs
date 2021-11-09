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

import controllers.routes
import models.{Approved, Rejected}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.utils.Stubs
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}
import utils.{AWRSFeatureSwitches, FeatureSwitch}

class EtmpCheckControllerSpec extends IntegrationSpec with AuthHelpers with Matchers with Stubs {

  val controllerUrl: String = routes.EtmpCheckController.checkEtmp().url

  "checkEtmp()" should {

    "return 200 for a successful self heal" when {

      "EMAC feature flag is true" when {

        "there is a business partner and regime in ETMP" when {

          "the status of the AWRS subscription is not a NonSelfHealStatus" when {

            "the business details provided match the details held on the business partner" when {

              "the enrolment has been upserted successfully" when {

                "the user is an organisation" in {

                  FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
                  stubAuditPosts
                  stubEtmpRegistrationResponse(OK, etmpRegistrationResultOrg)
                  stubAwrsSubscriptionResponse(OK, Some(subscriptionSuccessResponse))
                  stubShowAndRedirectExternalCalls("Organisation")
                  stubGetSubscriptionStatus(OK, Some(Approved.name))
                  stubUpsertAwrsEnrolment(NO_CONTENT)

                  val resp: WSResponse = await(client(controllerUrl).post(checkEtmpPostDataOrg))
                  resp.status mustBe 200
                }

                "the user is an individual" in {

                  FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
                  stubAuditPosts
                  stubEtmpRegistrationResponse(OK, etmpRegistrationResultInd)
                  stubAwrsSubscriptionResponse(OK, Some(subscriptionSuccessResponse))
                  stubShowAndRedirectExternalCalls("Individual")
                  stubGetSubscriptionStatus(OK, Some(Approved.name))
                  stubUpsertAwrsEnrolment(NO_CONTENT)

                  val resp: WSResponse = await(client(controllerUrl).post(checkEtmpPostDataInd))
                  resp.status mustBe 200
                }
              }
            }
          }
        }
      }
    }

    "return 204" when {

      "EMAC feature flag is false" in {
        FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())
        stubAuditPosts

        val resp: WSResponse = await(client(controllerUrl).post(checkEtmpPostDataInd))
        resp.status mustBe 204
      }

      "EMAC feature flag is true" when {

        "there is a business partner and regime in ETMP" when {

          "the status of the AWRS subscription is a NonSelfHealStatus" in {

            FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
            stubAuditPosts
            stubEtmpRegistrationResponse(OK, etmpRegistrationResultInd)
            stubShowAndRedirectExternalCalls("Individual")
            stubGetSubscriptionStatus(OK, Some(Rejected.name))

            val resp: WSResponse = await(client(controllerUrl).post(checkEtmpPostDataInd))
            resp.status mustBe 204

          }
        }

        "there is no business partner and regime in ETMP" in {

          FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
          stubAuditPosts
          stubEtmpRegistrationResponse(NOT_FOUND, "")

          val resp: WSResponse = await(client(controllerUrl).post(checkEtmpPostDataOrg))
          resp.status mustBe 204
        }
      }
    }
  }
}
