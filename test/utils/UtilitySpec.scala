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

package utils

import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
  import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import utils.AwrsTestJson.*
import utils.Utility.*

class UtilitySpec extends MockitoSugar with ScalaFutures with AnyWordSpecLike {

  val hipApi4InputSoleTraderNewBusinessJson: JsValue = api4hipSoleTraderNewBusinessJson
  val hipApi4InputCorporateBodyNewBusinessJson: JsValue = api4hipCorporateBodyNewBusinessJson
  val etmpApi4SoleTrader: JsValue = api4EtmpSoleTraderJson
  val etmpApi4CorporateBody: JsValue = api4EtmpCorporateBodyJson

  "UtilitySpec" must {

    "convert years correctly from string dd/MM/yyyy to yyyy-MM-dd (mdtp to etmp)" in {
      for (i <- 2000 to 3000) {
        awrsToEtmpDateFormatter(s"03/01/$i") shouldBe (s"$i-01-03")
      }
    }

    "convert years correctly from yyyy-MM-dd to string dd/MM/yyyy (etmp to mdtp)" in {
      for (i <- 2000 to 3000) {
        etmpToAwrsDateFormatter(s"$i-01-03") shouldBe (s"03/01/$i")
      }
    }

    "extract JsObject successfully for a given successful HIP response payload" in {
      val hipResponsePayload: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      val expectedUpdatedJson: JsValue = Json.parse(
        """
          |{
          | "processingDateTime": "2025-09-11T10:30:00Z"
          |}
          |""".stripMargin)

      stripSuccessNode(hipResponsePayload) shouldBe expectedUpdatedJson
    }

    "Return the original json when 'success' node is missing in response" in {
      val hipResponsePayload: JsValue = Json.parse(
        """
          |{
          |  "notAsuccessKey": {
          |    "someOtherTestKey": "testValue"
          |  }
          |}
          |""".stripMargin)

      stripSuccessNode(hipResponsePayload) shouldBe hipResponsePayload
    }

    "companyRegistrationNumber be substituted with companyRegNumber if key is present (Sole Trader)" in {
      val requestJson = etmpApi4SoleTrader
      val expectedJson = hipApi4InputSoleTraderNewBusinessJson

      val editedRequestBody = Utility.mapCrnForHipRequest(requestJson)
      editedRequestBody.toString contains("companyRegNumber") shouldBe false
      (editedRequestBody \\ "companyRegNumber").isEmpty shouldBe true
      (editedRequestBody \\ "companyRegistrationNumber").nonEmpty shouldBe true
      editedRequestBody shouldBe expectedJson
    }

    "companyRegistrationNumber be substituted with companyRegNumber if key is present (Corporate Body)" in {
      val requestJson = etmpApi4CorporateBody
      val expectedJson = hipApi4InputCorporateBodyNewBusinessJson


      val editedRequestBody = Utility.mapCrnForHipRequest(requestJson)
      editedRequestBody.toString contains("companyRegNumber") shouldBe false
      (editedRequestBody \\ "companyRegNumber").isEmpty shouldBe true
      (editedRequestBody \\ "companyRegistrationNumber").nonEmpty shouldBe true
      editedRequestBody shouldBe expectedJson
    }

    "companyRegNumber be substituted with companyRegistrationNumber if key is present (Sole Trader)" in {
      val responseJson = hipApi4InputSoleTraderNewBusinessJson
      val expectedJson = etmpApi4SoleTrader

      val editedResponseBody = Utility.mapCrnForResponseModel(responseJson)
      (editedResponseBody \\ "companyRegNumber").nonEmpty shouldBe true
      editedResponseBody shouldBe expectedJson
    }

    "companyRegNumber be substituted with companyRegistrationNumber if key is present (Corporate Body)" in {
      val responseJson = hipApi4InputCorporateBodyNewBusinessJson
      val expectedJson = etmpApi4CorporateBody

      val editedResponseBody = Utility.mapCrnForResponseModel(responseJson)
      (editedResponseBody \\ "companyRegNumber").nonEmpty shouldBe true
      editedResponseBody shouldBe expectedJson
    }
  }

  "Response must be unchanged if companyRegistrationNumber key is not present as identification" in {

    val response = """{
                     |  "subscriptionType": {
                     |    "legalEntity": "Sole Trader",
                     |    "newAWBusiness": false,
                     |    "businessDetails": {
                     |      "soleProprietor": {
                     |        "tradingName": "Trading name",
                     |        "identification": {
                     |          "doYouHaveVRN": true,
                     |          "vrn": "123456789",
                     |          "doYouHaveUTR": false,
                     |          "doYouHaveNino": false
                     |        }
                     |      }
                     |    },
                     |    "businessAddressForAwrs": {
                     |      "currentAddress": {
                     |        "addressLine1": "102, Sutton Street",
                     |        "addressLine2": "Wokingham",
                     |        "postalCode": "DH1 4DJ",
                     |        "countryCode": "GB"
                     |      },
                     |      "communicationDetails": {
                     |        "telephone": "07123456789",
                     |        "mobileNo": "07123456789",
                     |        "email": "John@sky.com"
                     |      },
                     |      "operatingDuration": "over 10 years",
                     |      "differentOperatingAddresslnLast3Years": true
                     |    },
                     |    "contactDetails": {
                     |      "name": {
                     |        "firstName": "John",
                     |        "lastName": "Clark"
                     |      },
                     |      "useAlternateContactAddress": false,
                     |      "communicationDetails": {
                     |        "email": "John@googlemail.com"
                     |      }
                     |    },
                     |    "additionalBusinessInfo": {
                     |      "all": {
                     |        "typeOfWholesaler": {
                     |          "cashAndCarry": true,
                     |          "offTradeSupplierOnly": true,
                     |          "onTradeSupplierOnly": true,
                     |          "all": true,
                     |          "other": false
                     |        },
                     |        "typeOfAlcoholOrders": {
                     |          "onlineOnly": true,
                     |          "onlineAndTel": true,
                     |          "onlineTelAndPhysical": true,
                     |          "all": true,
                     |          "other": false
                     |        },
                     |        "typeOfCustomers": {
                     |          "pubs": true,
                     |          "nightClubs": true,
                     |          "privateClubs": true,
                     |          "hotels": true,
                     |          "hospitalityCatering": true,
                     |          "restaurants": true,
                     |          "indepRetailers": true,
                     |          "nationalRetailers": true,
                     |          "public": true,
                     |          "otherWholesalers": true,
                     |          "all": true,
                     |          "other": false
                     |        },
                     |        "productsSold": {
                     |          "beer": true,
                     |          "wine": true,
                     |          "spirits": true,
                     |          "cider": true,
                     |          "perry": true,
                     |          "all": true,
                     |          "other": false
                     |        },
                     |        "numberOfPremises": "1",
                     |        "premiseAddress": [
                     |          {
                     |            "address": {
                     |              "addressLine1": "100, Sutton Street",
                     |              "addressLine2": "Wokingham",
                     |              "postalCode": "DH1 4EJ",
                     |              "countryCode": "GB"
                     |            }
                     |          }
                     |        ],
                     |        "thirdPartyStorageUsed": false,
                     |        "suppliers": {
                     |          "supplier": [
                     |            {
                     |              "name": "Clare",
                     |              "isSupplierVatRegistered": true,
                     |              "vrn": "123456789",
                     |              "address": {
                     |                "addressLine1": "101, Sutton Street",
                     |                "addressLine2": "Wokingham",
                     |                "postalCode": "DH1 4BJ",
                     |                "countryCode": "GB"
                     |              }
                     |            }
                     |          ]
                     |        },
                     |        "alcoholGoodsExported": true,
                     |        "euDispatches": true,
                     |        "alcoholGoodsImported": true
                     |      }
                     |    },
                     |    "declaration": {
                     |      "nameOfPerson": "Lee Hawks",
                     |      "statusOfPerson": "Authorised Signatory",
                     |      "informationIsAccurateAndComplete": true
                     |    }
                     |  }
                     |}
                     |""".stripMargin


    val editedResponseBody = Utility.mapCrnForResponseModel(Json.parse(response))
    editedResponseBody.toString contains("companyRegNumber") shouldBe false
    editedResponseBody.toString contains("companyRegistrationNumber") shouldBe false
    (editedResponseBody \\ "companyRegNumber").isEmpty shouldBe true
    (editedResponseBody \\ "companyRegistrationNumber").isEmpty shouldBe true
    editedResponseBody shouldBe Json.parse(response)
  }
}

