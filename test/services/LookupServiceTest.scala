/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.{EtmpConnector, HipConnector}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import utils.AwrsTestJson.testRefNo
import utils.FeatureSwitch.disable
import utils.{AWRSFeatureSwitches, BaseSpec, FeatureSwitch}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class LookupServiceTest extends BaseSpec with AnyWordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]

  override def beforeEach(): Unit = {
    disable(AWRSFeatureSwitches.hipSwitch())
  }

  override def afterAll(): Unit = {
    disable(AWRSFeatureSwitches.hipSwitch())
  }

  object TestLookupService extends LookupService(mockEtmpConnector, mockHipConnector)


  "LookupService " must {

    "successfully lookup application when passed a valid reference number" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      val awrsRefNo = testRefNo

      when(mockEtmpConnector.lookup(any())(any()))
        .thenReturn(Future.successful(HttpResponse(OK, "", Map.empty[String, Seq[String]])))

      val result = TestLookupService.lookupApplication(awrsRefNo)
      await(result).status shouldBe OK
    }

    "return Bad Request when passed an invalid reference number" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())

      val invalidAwrsRefNo = "AAW00000123456"

      when(mockEtmpConnector.lookup(any())(any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "", Map.empty[String, Seq[String]])))

      val result = TestLookupService.lookupApplication(invalidAwrsRefNo)
      await(result).status shouldBe BAD_REQUEST
    }

    "successfully lookup application when passed a valid reference number v1" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "subscriptionType": {
          |      "awrsRegistrationNumber": "XHAW00000123456",
          |      "businessPartnerName": "BusinessPartner",
          |      "legalEntity": "Sole Trader",
          |      "newAWBusiness": false,
          |      "businessDetails": {
          |        "soleProprietor": {
          |          "tradingName": "Trading name",
          |          "identification": {
          |            "doYouHaveVRN": true,
          |            "vrn": "123456789",
          |            "doYouHaveUTR": false,
          |            "doYouHaveNino": false
          |          }
          |        }
          |      },
          |      "businessAddressForAwrs": {
          |        "currentAddress": {
          |          "addressLine1": "102, Sutton Street",
          |          "addressLine2": "Wokingham",
          |          "postalCode": "DH1 4DJ",
          |          "countryCode": "GB"
          |        },
          |        "communicationDetails": {
          |          "telephone": "07123456789",
          |          "mobileNo": "07123456789",
          |          "email": "John@sky.com"
          |        },
          |        "operatingDuration": "over 10 years",
          |        "differentOperatingAddresslnLast3Years": true
          |      },
          |      "contactDetails": {
          |        "name": {
          |          "firstName": "John",
          |          "lastName": "Clark"
          |        },
          |        "useAlternateContactAddress": false,
          |        "communicationDetails": {
          |          "email": "John@googlemail.com"
          |        }
          |      },
          |      "additionalBusinessInfo": {
          |        "all": {
          |          "typeOfWholesaler": {
          |            "cashAndCarry": true,
          |            "offTradeSupplierOnly": true,
          |            "onTradeSupplierOnly": true,
          |            "all": true,
          |            "other": false
          |          },
          |          "typeOfAlcoholOrders": {
          |            "onlineOnly": true,
          |            "onlineAndTel": true,
          |            "onlineTelAndPhysical": true,
          |            "all": true,
          |            "other": false
          |          },
          |          "typeOfCustomers": {
          |            "pubs": true,
          |            "nightClubs": true,
          |            "privateClubs": true,
          |            "hotels": true,
          |            "hospitalityCatering": true,
          |            "restaurants": true,
          |            "indepRetailers": true,
          |            "nationalRetailers": true,
          |            "public": true,
          |            "otherWholesalers": true,
          |            "all": true,
          |            "other": false
          |          },
          |          "productsSold": {
          |            "beer": true,
          |            "wine": true,
          |            "spirits": true,
          |            "cider": true,
          |            "perry": true,
          |            "all": true,
          |            "other": false
          |          },
          |          "numberOfPremises": "1",
          |          "premiseAddress": [
          |            {
          |              "address": {
          |                "addressLine1": "100, Sutton Street",
          |                "addressLine2": "Wokingham",
          |                "postalCode": "DH1 4EJ",
          |                "countryCode": "GB"
          |              }
          |            }
          |          ],
          |          "thirdPartyStorageUsed": false,
          |          "suppliers": {
          |            "supplier": [
          |              {
          |                "name": "Clare",
          |                "isSupplierVatRegistered": true,
          |                "vrn": "123456789",
          |                "address": {
          |                  "addressLine1": "101, Sutton Street",
          |                  "addressLine2": "Wokingham",
          |                  "postalCode": "DH1 4BJ",
          |                  "countryCode": "GB"
          |                }
          |              }
          |            ]
          |          },
          |          "alcoholGoodsExported": true,
          |          "euDispatches": true,
          |          "alcoholGoodsImported": true
          |        }
          |      },
          |      "declaration": {
          |        "nameOfPerson": "Lee Hawks",
          |        "statusOfPerson": "Authorised Signatory",
          |        "informationIsAccurateAndComplete": true
          |      }
          |    }
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.lookup(any())(any()))
        .thenReturn(Future.successful(HttpResponse(OK, responseJson, Map.empty[String, Seq[String]])))

      val result = await(TestLookupService.lookupApplication(testRefNo))
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe (responseJson \ "success").get
    }

    "respond the caller with appropriate failure status code for a validation failures in lookup request" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson: JsValue = Json.parse(
        """
          {
          |  "errors": {
          |    "processingDate": "2022-01-31T09:26:17Z",
          |    "code": "002",
          |    "text": "ID not found"
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.lookup(any())(any()))
        .thenReturn(Future.successful(HttpResponse(Status.UNPROCESSABLE_ENTITY, responseJson, Map.empty[String, Seq[String]])))

      val result = await(TestLookupService.lookupApplication(testRefNo))
      result.status shouldBe Status.UNPROCESSABLE_ENTITY
      Json.parse(result.body) mustBe responseJson
    }

    "respond the caller with BAD_REQUEST if invalid JSON is send" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val response: String =
        """inavalid JSON""".stripMargin

      when(mockHipConnector.lookup(any())(any()))
        .thenReturn(Future.successful(HttpResponse(Status.BAD_REQUEST, response, Map.empty[String, Seq[String]])))

      val result = await(TestLookupService.lookupApplication(testRefNo))
      result.status shouldBe Status.BAD_REQUEST
      result.body mustBe response
    }
  }
}
