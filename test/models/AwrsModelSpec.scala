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

package models


import org.joda.time.LocalDate
import play.api.libs.json.{JsPath, Json}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestUtil._
import utils.{AwrsTestJson, TestUtil}
import utils.AwrsTestJson._

class AwrsModelSpec extends UnitSpec with AwrsTestJson {

  "An AwrsModelSpec CorporateBody json" should {
    "transform correctly to valid SubscriptionType Object for Corporate Body" in {
      val awrsModel = api4FrontendLTDJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("supplier")
      etmpJson should include("name")
      etmpJson should include("isSupplierVatRegistered")
      etmpJson should include("vrn")

      etmpJson should include("addressLine1")
      etmpJson should include("addressLine2")
      etmpJson should include("addressLine3")
      etmpJson should include("addressLine4")
      etmpJson should include("postalCode")
      etmpJson should include("countryCode")

      etmpJson should include("typeOfWholesaler")
      etmpJson should include("cashAndCarry")
      etmpJson should include("offTradeSupplierOnly")
      etmpJson should include("onTradeSupplierOnly")
      etmpJson should include("other")
      etmpJson should include("typeOfWholesaler")
      etmpJson should include("cashAndCarry")
      etmpJson should include("offTradeSupplierOnly")
      etmpJson should include("onTradeSupplierOnly")
      etmpJson should include("other")

      etmpJson should include("pubs")
      etmpJson should include("nightClubs")
      etmpJson should include("privateClubs")
      etmpJson should include("hotels")
      etmpJson should include("hospitalityCatering")
      etmpJson should include("restaurants")
      etmpJson should include("indepRetailers")
      etmpJson should include("nationalRetailers")
      etmpJson should include("public")
      etmpJson should include("otherWholesalers")
      etmpJson should include("all")

      etmpJson should include("premiseAddress")
      etmpJson should include("alcoholGoodsImported")
      etmpJson should include("legalEntity")
      etmpJson should include("newAWBusiness")
      etmpJson should include("subscriptionType")
      etmpJson should include("acknowledgmentReference")


      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with missing VRN for corporateBodyBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "doYouHaveVRN", api4FrontendLTDString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "vrn", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should not include "\"doYouHaveVRN\":\"false"
      etmpJson should not include "\"vrn\":\"000000000"

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with missing UTR for corporateBodyBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "doYouHaveUTR", api4FrontendLTDString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "utr", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should not include "\"doYouHaveUTR\":\"false"
      etmpJson should not include testUtr

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object without incorporation details for corporateBodyBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "companyRegDetails", api4FrontendLTDString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" ->
          Json.obj("businessRegistrationDetails" ->
            Json.obj("isBusinessIncorporated" -> "No"))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should not include "\"isBusinessIncorporated\":\"false"

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object when director national Id is supplied for corporateBodyBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessDirectors", api4FrontendLTDString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessDirectors" ->
            Json.obj("directors" ->
              Json.arr(Json.obj(
                "directorsAndCompanySecretaries" -> "Director",
                "personOrCompany" -> "person",
                "firstName" -> "Example",
                "lastName" -> "Exampleson",
                "doTheyHaveNationalInsurance" -> "Yes",
                "nationalID" -> "1234567890",
                "otherDirectors" -> "No"
              )),
              "modelVersion" -> s"${BusinessDirectors.latestModelVersion}"
            ))),
        deletedJson)

      println(updatedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"status\":\"Director")
      etmpJson should include("\"nationalIdNumber\":\"1234567890")
      etmpJson should not include testNino

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object when director passport number is supplied for corporateBodyBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessDirectors", api4FrontendLTDString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessDirectors" ->
            Json.obj("directors" -> Json.arr(Json.obj(
              "directorsAndCompanySecretaries" -> "Director",
              "personOrCompany" -> "person",
              "firstName" -> "Example",
              "lastName" -> "Exampleson",
              "doTheyHaveNationalInsurance" -> "Yes",
              "passportNumber" -> "0987654321",
              "otherDirectors" -> "No")),
              "modelVersion" -> "1.0")
          )),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"status\":\"Director")
      etmpJson should include("\"passportNumber\":\"0987654321")
      etmpJson should not include testNino

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object when director has no ID supplied for corporateBodyBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessDirectors", api4FrontendLTDString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessDirectors" ->
            Json.obj("directors" -> Json.arr(Json.obj(
              "directorsAndCompanySecretaries" -> "Director",
              "personOrCompany" -> "person",
              "firstName" -> "Example",
              "lastName" -> "Exampleson",
              "doTheyHaveNationalInsurance" -> "Yes",
              "otherDirectors" -> "No")),
              "modelVersion" -> s"${BusinessDirectors.latestModelVersion}"
            ))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"status\":\"Director")
      etmpJson should not include testNino

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe false
    }

    "transform correctly to valid SubscriptionType Object when no directors have been supplied for corporateBodyBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessDirectors", api4FrontendLTDString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessDirectors" ->
            Json.obj(
              "directors" -> Json.arr(),
              "modelVersion" -> s"${BusinessDirectors.latestModelVersion}"
            )
          )),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"numberOfCoOfficials\":\"0\"")
      TestUtil.validateJson(schemaPath, etmpJson) shouldBe false

    }

  }

  "A AwrsFrontEndModel CorporateBody json with isBusinessIncorporated Yes" should {
    "transform correctly to valid  SubscriptionType Object for Corporate Body" in {

      val awrsModel = api4FrontendLTDJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("llpCorporateBody")
      etmpJson should include("isBusinessIncorporated")
      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }
  }

  "A AwrsFrontEndModel Partnership json" should {
    "transform correctly to valid SubscriptionType Object with missing VRN for Partnership business details" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "doYouHaveVRN", api4FrontendPartnershipString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "vrn", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"businessDetails\":{\"nonProprietor\":{\"tradingName\":\"asddsad\",\"identification\":{\"doYouHaveVRN\":false")
      etmpJson should not include "\"businessDetails\":{\"nonProprietor\":{\"tradingName\":\"asddsad\",\"identification\":{\"doYouHaveVRN\":false,\"vrn\":\"000000000\""

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with missing UTR for Partnership business details" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "doYouHaveUTR", api4FrontendPartnershipString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "utr", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"businessDetails\":{\"nonProprietor\":{\"tradingName\":\"asddsad\",\"identification\":{\"doYouHaveVRN\":true,\"vrn\":\"000000000\",\"doYouHaveUTR\":false")
      etmpJson should not include "\"businessDetails\":{\"nonProprietor\":{\"tradingName\":\"asddsad\",\"identification\":{\"doYouHaveVRN\":true,\"vrn\":\"000000000\",\"doYouHaveUTR\":true,\"utr\":\"1111111111\""

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with nino not present for Individual partnership" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "partnership" \ "partners", api4FrontendPartnershipString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("partnership" -> Json.
            obj("partners" -> Json.arr(Json.obj()
              .++(Json.obj("entityType" -> "Individual"))
              .++(Json.obj("partnerAddress" -> Json.
                obj("postcode" -> "AA1 1AA")
                .++(Json.obj("addressLine1" -> "address line 1"))
                .++(Json.obj("addressLine2" -> "address line 2"))
                .++(Json.obj("addressLine3" -> "address line 3"))
                .++(Json.obj("addressLine4" -> "address line 4"))
                .++(Json.obj("countryCode" -> "GB"))
              ))
              .++(Json.obj("firstName" -> "Example"))
              .++(Json.obj("lastName" -> "Exampleson")))))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"numberOfPartners\":\"1")
      etmpJson should include("\"individual\":{\"name\":{\"firstName\":\"Example\",\"lastName\":\"Exampleson\"},\"doYouHaveNino\":false")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with company Reg Details not present for Corporate Body partnership" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "partnership" \ "partners", api4FrontendPartnershipString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("partnership" -> Json.
            obj("partners" -> Json.arr(Json.obj()
              .++(Json.obj("entityType" -> "Corporate Body"))
              .++(Json.obj("partnerAddress" -> Json.
                obj("postcode" -> "AA1 1AA")
                .++(Json.obj("addressLine1" -> "address line 1"))
                .++(Json.obj("addressLine2" -> "address line 2"))
                .++(Json.obj("addressLine3" -> "address line 3"))
                .++(Json.obj("addressLine4" -> "address line 4"))
                .++(Json.obj("countryCode" -> "GB"))
              ))
              .++(Json.obj(
                "companyNames" -> Json.obj(
                  "businessName" -> "company Name",
                  "doYouHaveTradingName" -> "Yes",
                  "tradingName" -> "trading Name")
              ))
              .++(Json.obj("tradingName" -> "trading Name"))
              .++(Json.obj("doYouHaveVRN" -> "No"))
              .++(Json.obj("doYouHaveUTR" -> "No"))
            )))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"numberOfPartners\":\"1")
      etmpJson should include("\"isBusinessIncorporated\":false")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }
  }

  "A AwrsFrontEndModel Soletrader json" should {
    "transform correctly to valid  SubscriptionType for Sole Trader" in {

      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("supplier")
      etmpJson should include("name")
      etmpJson should include("isSupplierVatRegistered")
      etmpJson should include("addressLine1")
      etmpJson should include("addressLine2")
      etmpJson should include("addressLine3")
      etmpJson should include("addressLine4")
      etmpJson should include("postalCode")
      etmpJson should include("countryCode")

      etmpJson should include("typeOfWholesaler")
      etmpJson should include("cashAndCarry")
      etmpJson should include("offTradeSupplierOnly")
      etmpJson should include("onTradeSupplierOnly")
      etmpJson should include("other")
      etmpJson should include("typeOfWholesaler")
      etmpJson should include("cashAndCarry")
      etmpJson should include("offTradeSupplierOnly")
      etmpJson should include("onTradeSupplierOnly")
      etmpJson should include("other")

      etmpJson should include("pubs")
      etmpJson should include("nightClubs")
      etmpJson should include("privateClubs")
      etmpJson should include("hotels")
      etmpJson should include("hospitalityCatering")
      etmpJson should include("restaurants")
      etmpJson should include("indepRetailers")
      etmpJson should include("nationalRetailers")
      etmpJson should include("public")
      etmpJson should include("otherWholesalers")
      etmpJson should include("all")

      etmpJson should include("premiseAddress")
      etmpJson should include("alcoholGoodsImported")
      etmpJson should include("legalEntity")
      etmpJson should include("newAWBusiness")
      etmpJson should include("subscriptionType")
      etmpJson should include("acknowledgmentReference")
      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with missing VRN for soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "doYouHaveVRN", api4FrontendSOPString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "vrn", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should not include "\"doYouHaveUTR\":\"false"
      etmpJson should not include "\"vrn\":\"000000000"

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with missing UTR for soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "doYouHaveUTR", api4FrontendSOPString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "utr", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should not include "\"doYouHaveUTR\":\"false"
      etmpJson should not include testUtr

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with missing Nino for soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "doYouHaveNino", api4FrontendSOPString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "nino", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should not include "\"doYouHaveNino\":\"false"
      etmpJson should not include testNino

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with no main place of business for soleTraderBusinessDetails" in {

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessDetails" -> Json.obj()
            .++(Json.obj("mainPlaceOfBusiness" -> "No"))
            .++(Json.obj("mainAddress" -> Json.obj()
              .++(Json.obj("addressLine1" -> "1 Example Street"))
              .++(Json.obj("addressLine2" -> "Exampe View"))
              .++(Json.obj("addressLine3" -> "Exampe Town"))
              .++(Json.obj("addressLine4" -> "Exampeshire"))
              .++(Json.obj("postcode" -> "AA1 1AA"))
              .++(Json.obj("addressCountryCode" -> "GB")))
            ))
        ),
        api4FrontendSOPString)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with empty main place of business for soleTraderBusinessDetails" in {

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("soleTraderBusinessDetails" -> Json.obj()
            .++(Json.obj("mainPlaceOfBusiness" -> "No"))
            .++(Json.obj("mainAddress" -> Json.obj()
              .++(Json.obj("addressLine1" -> "1 Example Street"))
              .++(Json.obj("addressLine2" -> "Exampe View"))
              .++(Json.obj("addressLine3" -> "Exampe Town"))
              .++(Json.obj("addressLine4" -> "Exampeshire"))
              .++(Json.obj("postcode" -> "AA1 1AA"))
              .++(Json.obj("addressCountryCode" -> "GB")))
            ))
        ),
        api4FrontendSOPString)

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessDetails" \ "mainPlaceOfBusiness", updatedJson)

      val awrsModel = Json.parse(deletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with no address 3 or 4 for soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessCustomerDetails" \ "businessAddress" \ "line_3", api4FrontendSOPString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessCustomerDetails" \ "businessAddress" \ "line_4", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should not include "\"addressLine3\":\"Exampe Town"
      etmpJson should not include "\"addressLine4\":\"Exampeshire"

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with empty placeOfBusinessLast3Yearsfor soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessContacts" \ "placeOfBusinessLast3Years", api4FrontendSOPString)

      val awrsModel = Json.parse(deletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"differentOperatingAddresslnLast3Years\":false")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with contactAddressSame No soleTraderBusinessDetails" in {

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessContacts" -> Json.obj()
            .++(Json.obj("contactAddressSame" -> "No"))
            .++(Json.obj("contactAddress" -> Json.obj()
              .++(Json.obj("addressLine1" -> "3 Example street"))
              .++(Json.obj("addressLine2" -> "Example Suburbs"))
              .++(Json.obj("addressLine3" -> "Example Park"))
              .++(Json.obj("addressLine4" -> "Exampledon"))
              .++(Json.obj("postalCode" -> "AA1 1AA"))
              .++(Json.obj("countryCode" -> "GB")))))),
        api4FrontendSOPString)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"useAlternateContactAddress\":true")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with empty contactAddressSame soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessContacts" \ "contactAddressSame", api4FrontendSOPString)

      val awrsModel = Json.parse(deletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"useAlternateContactAddress\":false")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with other wholesaler type soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "tradingActivity" \ "otherWholesaler", api4FrontendSOPString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("tradingActivity" -> Json.
            obj("wholesalerType" -> Json.arr("99")))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"cashAndCarry\":false")
      etmpJson should include("\"other\":true")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe false
    }

    "transform correctly to valid SubscriptionType Object with other customer type soleTraderBusinessDetails" in {

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("products" -> Json.
            obj("mainCustomers" -> Json.arr("99")))),
        api4FrontendSOPString)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"other\":true")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe false
    }

    "transform correctly to valid SubscriptionType Object with other product type soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "products" \ "otherProductType", api4FrontendSOPString)

      val awrsModel = Json.parse(deletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"other\":true")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe false
    }

    "transform correctly to valid SubscriptionType Object with alcohol order type soleTraderBusinessDetails" in {

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("tradingActivity" -> Json.
            obj("typeOfAlcoholOrders" -> Json.arr()))),
        api4FrontendSOPString)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"onlineOnly\":false")
      etmpJson should include("\"onlineAndTel\":false")
      etmpJson should include("\"onlineTelAndPhysical\":false")
      etmpJson should include("\"all\":false")
      etmpJson should include("\"other\":false")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object with other alchohol order type soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "tradingActivity" \ "otherTypeOfAlcoholOrders", api4FrontendSOPString)

      val awrsModel = Json.parse(deletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"other\":true")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe false
    }

    "transform correctly to valid SubscriptionType Object with doesBusinessImportAlcohol empty soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "tradingActivity" \ "doesBusinessImportAlcohol", api4FrontendSOPString)

      val awrsModel = Json.parse(deletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"alcoholGoodsImported\":false")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "transform correctly to valid SubscriptionType Object missing declaration and role soleTraderBusinessDetails" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "applicationDeclaration" \ "declarationName", api4FrontendSOPString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "applicationDeclaration" \ "declarationRole", deletedJson)

      val awrsModel = Json.parse(finalDeletedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"nameOfPerson\":\"\"")
      etmpJson should include("\"statusOfPerson\":\"Other\"")

      TestUtil.validateJson(schemaPath, etmpJson) shouldBe false
    }

  }

  "A AwrsFrontEndModel with Premises Address json" should {
    "transform correctly to valid  SubscriptionType for Sole Trader" in {

      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]

      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("premiseAddress")
      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }
  }

  "A AwrsFrontEndModel with Premises Address json" should {
    "transform correctly to valid  SubscriptionType for PARTNERSHIP" in {

      val awrsModel = api4FrontendPartnershipJson.as[AWRSFEModel]

      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()
      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true

    }
  }

  "A AwrsFrontEndModel with LLP details" should {

    "transform correctly to valid SubscriptionType for LLP without Groups" in {
      val awrsModel = api4FrontendLLPJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include(Json.parse(api5EtmpLLPString).toString())
      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }
  }

  "A AwrsFrontEndModel with LP details" should {

    "transform correctly to valid SubscriptionType for LP without groups" in {

      val awrsModel = api4FrontendLPJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include(Json.parse(api5EtmpLPString).toString())
      TestUtil.validateJson(schemaPath, etmpJson) should be(true)
    }

    "transform correctly to valid  SubscriptionType for LLP with groups" in {
      val awrsModel = api4FrontendLLPGRPJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      withClue(s"etmpJson:\n\n$etmpJson\n\n") {
        etmpJson should include(Json.parse(api5EtmpLLPGroupString).toString())
        TestUtil.validateJson(schemaPath, etmpJson) should be(true)
      }
    }
  }

  "A AwrsFrontEndModel with LTD details" should {
    "transform correctly to valid SubscriptionType for LTD without groups" in {
      val awrsModel = api4FrontendLTDJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include(Json.parse(api5EtmpLTDString).toString())
      TestUtil.validateJson(schemaPath, etmpJson) should be(true)
    }

    "transform correctly to valid SubscriptionType for LTD with groups" in {
      val awrsModel = api4FrontendLTDGRPWithCompaniesJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"numberOfGrpMembers\":\"2\"")
      etmpJson should include("\"creatingAGroup\":true")
      etmpJson should include("\"groupJoiningDate\":\"" + testGrpJoinDate)

      TestUtil.validateJson(schemaPath, etmpJson) should be(true)
    }
  }

  "A AwrsFrontEndModel with Partnership details" should {

    "transform correctly to valid  SubscriptionType for Partnership without groups" in {
      val awrsModel = api4FrontendPartnershipJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include(Json.parse(api5EtmpPartnershipString).toString())
      TestUtil.validateJson(schemaPath, etmpJson) should be(true)
    }
  }

  "A AwrsFrontEndModel with SOP details" should {

    "transform correctly to valid SubscriptionType for SOP without groups" in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include(Json.parse(api5EtmpSOPString).toString())
      TestUtil.validateJson(schemaPath, etmpJson) should be(true)
    }
  }

  "An API6 CorporateBody json" should {
    "transform correctly to valid Corporate Body" in {

      val awrsModel = api6FrontendLTDJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("supplier")
      etmpJson should include("name")
      etmpJson should include("isSupplierVatRegistered")
      etmpJson should include("vrn")

      etmpJson should include("addressLine1")
      etmpJson should include("addressLine2")
      etmpJson should include("addressLine3")
      etmpJson should include("addressLine4")
      etmpJson should include("postalCode")
      etmpJson should include("countryCode")

      etmpJson should include("typeOfWholesaler")
      etmpJson should include("cashAndCarry")
      etmpJson should include("offTradeSupplierOnly")
      etmpJson should include("onTradeSupplierOnly")
      etmpJson should include("other")
      etmpJson should include("typeOfWholesaler")
      etmpJson should include("cashAndCarry")
      etmpJson should include("offTradeSupplierOnly")
      etmpJson should include("onTradeSupplierOnly")
      etmpJson should include("other")

      etmpJson should include("pubs")
      etmpJson should include("nightClubs")
      etmpJson should include("privateClubs")
      etmpJson should include("hotels")
      etmpJson should include("hospitalityCatering")
      etmpJson should include("restaurants")
      etmpJson should include("indepRetailers")
      etmpJson should include("nationalRetailers")
      etmpJson should include("public")
      etmpJson should include("otherWholesalers")
      etmpJson should include("all")

      etmpJson should include("\"additionalBusinessInfoChanged\":true")
      etmpJson should include("\"businessAddressChanged\":true")
      etmpJson should include("\"businessDetailsChanged\":true")
      etmpJson should include("\"contactDetailsChanged\":true")
      etmpJson should include("\"coOfficialsChanged\":true")
      etmpJson should include("\"declarationChanged\":false")
      etmpJson should include("\"groupMembersChanged\":true")
      etmpJson should include("\"partnersChanged\":true")
      etmpJson should include("\"premisesChanged\":true")
      etmpJson should include("\"suppliersChanged\":true")

      TestUtil.validateJson("/schema/API6EtmpSchema.json", etmpJson) shouldBe true
    }
  }

}
