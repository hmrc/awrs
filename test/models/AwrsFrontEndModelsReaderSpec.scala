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

package models

import play.api.libs.json.{JsPath, Json}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson
import utils.AwrsTestJson._
import utils.TestUtil._

import scala.collection.GenSeq

class AwrsFrontEndModelsReaderSpec extends UnitSpec with AwrsTestJson {

  "AwrsFrontEndModelsReaderSpec " should {

    "transform ETMP LTD Json correctly to AWRSFEModel Frontend Model " in {
      val awrsFEModelDetails = api4EtmpLTDJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      awrsFEModelDetails shouldBe a[AWRSFEModel]
    }

    "transform ETMP SOP Json correctly to AWRSFEModel Frontend Model " in {
      val awrsFEModelDetails = api4EtmpSOPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      awrsFEModelDetails shouldBe a[AWRSFEModel]
    }

    "transform ETMP Partnership Json correctly to AWRSFEModel Frontend Model " in {
      val awrsFEModelDetails = api4EtmpPartnershipJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      awrsFEModelDetails shouldBe a[AWRSFEModel]
    }

    "transform ETMP LLP Json correctly to AWRSFEModel Frontend Model " in {
      val awrsFEModelDetails = api4EtmpLLPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      awrsFEModelDetails shouldBe a[AWRSFEModel]
    }

    "transform ETMP GRP LTD Json correctly to AWRSFEModel Frontend Model " in {
      val awrsFEModelDetails = api4EtmpLTDGRPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      awrsFEModelDetails shouldBe a[AWRSFEModel]
    }

    "transform ETMP GRP LLP Json correctly to AWRSFEModel Frontend Model " in {
      val awrsFEModelDetails = api4EtmpLLPGRPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      awrsFEModelDetails shouldBe a[AWRSFEModel]
    }

    "transform correctly to SubscriptionType Frontend Model " in {
      val subscriptionTypeDetails = api4EtmpLTDJson.as[SubscriptionTypeFrontEnd](SubscriptionTypeFrontEnd.reader)
      subscriptionTypeDetails shouldBe a[SubscriptionTypeFrontEnd]
    }

    "transform correctly to BusinessCustomerDetails Frontend Model " in {
      val businessCustomerDetails = api4EtmpLTDJson.as[BusinessCustomerDetails](BusinessCustomerDetails.reader)
      businessCustomerDetails.businessType shouldBe None
    }

    "transform correctly to SubscriptionType Frontend Model when there is 'No Supplier' " in {
      val supplierPath = JsPath \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "suppliers"
      val updatedJson = deleteFromJson(supplierPath, api4EtmpLTDString)
      val subscriptionType = Json.parse(updatedJson).as[SubscriptionTypeFrontEnd](SubscriptionTypeFrontEnd.reader)
      subscriptionType.suppliers.get.suppliers.head.alcoholSuppliers shouldBe Some("No")
    }

    "transform correctly to SoleTraderBusinessDetails Frontend Model " in {
      val testJson = api4EtmpSOPJson

      val businessDetails = testJson.as[BusinessDetails](BusinessDetails.reader(Some("SOP")))
      val businessRegistrationDetails = testJson.as[BusinessRegistrationDetails](BusinessRegistrationDetails.reader(Some("SOP")))
      val businessContacts = testJson.as[BusinessContacts](BusinessContacts.reader)
      val placeOfBusiness = testJson.as[PlaceOfBusiness](PlaceOfBusiness.reader)

      businessDetails shouldBe a[BusinessDetails]
      businessDetails.tradingName.get shouldBe "Trading name"
      businessRegistrationDetails.doYouHaveVRN shouldBe Some("Yes")
      businessRegistrationDetails.vrn shouldBe Some("123456789")
      businessRegistrationDetails.doYouHaveUTR shouldBe Some("No")
      businessRegistrationDetails.doYouHaveNino shouldBe Some("No")

      businessContacts shouldBe a[BusinessContacts]
      businessContacts.telephone shouldBe "07000111222"
      businessContacts.email shouldBe "example@example.com"
      placeOfBusiness.operatingDuration shouldBe "10 or more years"
    }

    "transform correctly to CorporateBodyBusinessDetails Frontend Model " in {
      val testJson = api4EtmpLTDJson

      val businessDetails = testJson.as[BusinessDetails](BusinessDetails.reader(Some("LTD")))
      val businessRegistrationDetails = testJson.as[BusinessRegistrationDetails](BusinessRegistrationDetails.reader(Some("LTD")))
      val businessContacts = testJson.as[BusinessContacts](BusinessContacts.reader)
      val placeOfBusiness = testJson.as[PlaceOfBusiness](PlaceOfBusiness.reader)

      businessDetails shouldBe a[BusinessDetails]
      businessDetails.tradingName.get shouldBe "Example Ltd"
      businessRegistrationDetails.isBusinessIncorporated shouldBe Some("Yes")
      businessRegistrationDetails.doYouHaveVRN shouldBe Some("Yes")
      businessRegistrationDetails.doYouHaveUTR shouldBe Some("Yes")

      businessContacts shouldBe a[BusinessContacts]
      businessContacts.telephone shouldBe "07000111222"
      businessContacts.email shouldBe "test@example.com"
      placeOfBusiness.operatingDuration shouldBe "Less than 2 years"
    }

    "transform correctly to Partnership Business Details Frontend Model and create correct json" in {
      val testJson = api4EtmpPartnershipJson

      val businessDetails = testJson.as[BusinessDetails](BusinessDetails.reader(Some("Partnership")))
      val businessRegistrationDetails = testJson.as[BusinessRegistrationDetails](BusinessRegistrationDetails.reader(Some("Partnership")))
      val businessContacts = testJson.as[BusinessContacts](BusinessContacts.reader)
      val placeOfBusiness = testJson.as[PlaceOfBusiness](PlaceOfBusiness.reader)

      businessDetails shouldBe a[BusinessDetails]
      businessDetails.tradingName.get shouldBe "Trading name"
      businessRegistrationDetails.doYouHaveVRN shouldBe Some("No")
      businessRegistrationDetails.doYouHaveUTR shouldBe Some("No")

      businessContacts shouldBe a[BusinessContacts]
      placeOfBusiness.operatingDuration shouldBe "10 or more years"
      businessContacts.contactAddressSame shouldBe Some("Yes")
      businessContacts.contactFirstName shouldBe "Contact first name"
      businessContacts.contactLastName shouldBe "Contact last name"
      businessContacts.telephone shouldBe "07000111222"
      placeOfBusiness.placeOfBusinessLast3Years shouldBe Some("No")
    }

    "transform correctly to GroupRepBusinessDetails Frontend Model " in {
      val testJson = api4EtmpLLPGRPJson

      val businessDetails = testJson.as[BusinessDetails](BusinessDetails.reader(Some("LLP_GRP")))
      val businessRegistrationDetails = testJson.as[BusinessRegistrationDetails](BusinessRegistrationDetails.reader(Some("LLP_GRP")))
      val businessContacts = testJson.as[BusinessContacts](BusinessContacts.reader)
      val placeOfBusiness = testJson.as[PlaceOfBusiness](PlaceOfBusiness.reader)

      businessDetails shouldBe a[BusinessDetails]
      businessDetails.tradingName.get shouldBe "Example"
      businessRegistrationDetails.isBusinessIncorporated shouldBe Some("No")
      businessRegistrationDetails.doYouHaveVRN shouldBe Some("Yes")
      businessRegistrationDetails.doYouHaveUTR shouldBe Some("No")

      businessContacts shouldBe a[BusinessContacts]
      businessContacts.telephone shouldBe "07000111222"
      businessContacts.email shouldBe "example@example.com"
      placeOfBusiness.operatingDuration shouldBe "10 or more years"
    }

    "transform correctly to Suppliers Frontend Model " in {
      val suppliers = api4EtmpLTDJson.as[Suppliers](Suppliers.reader)
      suppliers.suppliers(0).additionalSupplier.get shouldBe "Yes"
      suppliers.suppliers(1).additionalSupplier.get shouldBe "Yes"
      suppliers.suppliers(2).additionalSupplier.get shouldBe "No"
      suppliers shouldBe a[Suppliers]

      val businessSuppliers = Json.toJson(suppliers).toString()

      businessSuppliers should include("alcoholSuppliers")
      businessSuppliers should include("supplierName")
      businessSuppliers should include("vatRegistered")
      businessSuppliers should include("supplierAddress")
      businessSuppliers should include("additionalSupplier")
      businessSuppliers should include("ukSupplier")
    }

    "transform correctly to Partner Details Frontend Model for Partnership" in {
      val partners = api4EtmpPartnershipJson.as[Partners](Partners.reader)
      partners shouldBe a[Partners]
      partners.partners(0).otherPartners.get shouldBe "Yes"
      partners.partners(1).otherPartners.get shouldBe "Yes"
      partners.partners(2).otherPartners.get shouldBe "No"

      partners.partners.head.entityType.get shouldBe "Individual"
      partners.partners.head.firstName.get shouldBe "example"
      partners.partners.head.lastName.get shouldBe "exampleson"
      partners.partners.head.companyNames.tradingName shouldBe None
      partners.partners.head.companyNames.businessName shouldBe None
      partners.partners.head.doYouHaveNino.get shouldBe "Yes"
      partners.partners.head.nino.get shouldBe testNino
      partners.partners.head.doYouHaveUTR shouldBe None
      partners.partners.head.utr shouldBe None
      partners.partners.head.doYouHaveVRN shouldBe None
      partners.partners.head.vrn shouldBe None
      partners.partners.head.partnerAddress.get.addressLine1 shouldBe "1 Example Street"
      partners.partners.head.partnerAddress.get.addressLine2 shouldBe "Exampe View"
      partners.partners.head.partnerAddress.get.addressLine3.get shouldBe "Exampe Town"
      partners.partners.head.partnerAddress.get.addressLine4.get shouldBe "Exampeshire"
      partners.partners.head.partnerAddress.get.postcode.get shouldBe "AA1 1AA"
      partners.partners.head.isBusinessIncorporated shouldBe None
      partners.partners.head.companyRegDetails shouldBe None

      partners.partners.last.entityType.get shouldBe "Sole Trader"
      partners.partners.last.firstName.get shouldBe "example"
      partners.partners.last.lastName.get shouldBe "exampleson"
      partners.partners.last.companyNames.tradingName.get shouldBe "trading name"
      partners.partners.last.companyNames.businessName shouldBe None
      partners.partners.last.doYouHaveNino.get shouldBe "Yes"
      partners.partners.last.nino.get shouldBe testNino
      partners.partners.last.doYouHaveUTR.get shouldBe "Yes"
      partners.partners.last.utr.get shouldBe testUtr
      partners.partners.last.doYouHaveVRN.get shouldBe "Yes"
      partners.partners.last.vrn.get shouldBe "000000000"
      partners.partners.last.partnerAddress.get.addressLine1 shouldBe "1 Example Street"
      partners.partners.last.partnerAddress.get.addressLine2 shouldBe "Exampe View"
      partners.partners.last.partnerAddress.get.addressLine3.get shouldBe "Exampe Town"
      partners.partners.last.partnerAddress.get.addressLine4.get shouldBe "Exampeshire"
      partners.partners.last.partnerAddress.get.postcode.get shouldBe "AA1 1AA"
      partners.partners.head.isBusinessIncorporated shouldBe None
      partners.partners.head.companyRegDetails shouldBe None
    }

    "transform correctly to Partner Details Frontend Model for Partnership when Partner is of type Corporate Body" in {
      val partners = api4EtmpPartnershipJson.as[Partners](Partners.reader)
      partners shouldBe a[Partners]
      partners.partners(0).otherPartners.get shouldBe "Yes"
      partners.partners(1).otherPartners.get shouldBe "Yes"
      partners.partners(2).otherPartners.get shouldBe "No"

      partners.partners(1).entityType.get shouldBe "Corporate Body"
      partners.partners(1).companyNames.businessName.get shouldBe "company name"
      partners.partners(1).companyNames.tradingName.get shouldBe "trading name"
      partners.partners(1).firstName shouldBe None
      partners.partners(1).lastName shouldBe None
      partners.partners(1).doYouHaveNino shouldBe None
      partners.partners(1).nino shouldBe None
      partners.partners(1).doYouHaveVRN.get shouldBe "Yes"
      partners.partners(1).vrn.get shouldBe "000000000"
      partners.partners(1).doYouHaveUTR.get shouldBe "Yes"
      partners.partners(1).utr.get shouldBe testUtr
      partners.partners(1).isBusinessIncorporated.get shouldBe "Yes"
      partners.partners(1).companyRegDetails.get.companyRegistrationNumber shouldBe "55555555"
      partners.partners(1).companyRegDetails.get.dateOfIncorporation shouldBe "01/01/2015"
      partners.partners(1).partnerAddress.get.addressLine1 shouldBe "1 Example Street"
      partners.partners(1).partnerAddress.get.addressLine2 shouldBe "Exampe View"
      partners.partners(1).partnerAddress.get.addressLine3.get shouldBe "Exampe Town"
      partners.partners(1).partnerAddress.get.addressLine4.get shouldBe "Exampeshire"
      partners.partners(1).partnerAddress.get.postcode.get shouldBe "AA1 1AA"
    }

    "transform correctly to Partner Details Frontend Model for LLP" in {
      val partners = api4EtmpLLPJson.as[Partners](Partners.reader)
      partners shouldBe a[Partners]
      partners.partners(0).otherPartners.get shouldBe "Yes"
      partners.partners(1).otherPartners.get shouldBe "Yes"
      partners.partners(2).otherPartners.get shouldBe "No"

      partners.partners.head.entityType.get shouldBe "Individual"
      partners.partners.head.firstName.get shouldBe "example"
      partners.partners.head.lastName.get shouldBe "exampleson"
      partners.partners.head.companyNames.tradingName shouldBe None
      partners.partners.head.companyNames.businessName shouldBe None
      partners.partners.head.doYouHaveNino.get shouldBe "Yes"
      partners.partners.head.nino.get shouldBe testNino
      partners.partners.head.doYouHaveUTR shouldBe None
      partners.partners.head.utr shouldBe None
      partners.partners.head.doYouHaveVRN shouldBe None
      partners.partners.head.vrn shouldBe None
      partners.partners.head.partnerAddress.get.addressLine1 shouldBe "1 Example Street"
      partners.partners.head.partnerAddress.get.addressLine2 shouldBe "Exampletown"
      partners.partners.head.partnerAddress.get.addressLine3 shouldBe None
      partners.partners.head.partnerAddress.get.addressLine4 shouldBe None
      partners.partners.head.partnerAddress.get.postcode.get shouldBe "AA1 1AA"
      partners.partners.head.isBusinessIncorporated shouldBe None
      partners.partners.head.companyRegDetails shouldBe None

      partners.partners.last.entityType.get shouldBe "Sole Trader"
      partners.partners.last.firstName.get shouldBe "example"
      partners.partners.last.lastName.get shouldBe "exampleson"
      partners.partners.last.companyNames.tradingName.get shouldBe "trading name"
      partners.partners.last.companyNames.businessName shouldBe None
      partners.partners.last.doYouHaveNino.get shouldBe "Yes"
      partners.partners.last.nino.get shouldBe testNino
      partners.partners.last.doYouHaveUTR.get shouldBe "No"
      partners.partners.last.utr shouldBe None
      partners.partners.last.doYouHaveVRN.get shouldBe "No"
      partners.partners.last.vrn shouldBe None
      partners.partners.last.partnerAddress.get.addressLine1 shouldBe "2 Example Street"
      partners.partners.last.partnerAddress.get.addressLine2 shouldBe "Exampletown"
      partners.partners.last.partnerAddress.get.addressLine3 shouldBe None
      partners.partners.last.partnerAddress.get.addressLine4 shouldBe None
      partners.partners.last.partnerAddress.get.postcode.get shouldBe "AA1 1AA"
      partners.partners.head.isBusinessIncorporated shouldBe None
      partners.partners.head.companyRegDetails shouldBe None
    }

    "transform correctly to Partner Details Frontend Model for LLP when Partner is of type Corporate Body" in {
      val partners = api4EtmpLLPJson.as[Partners](Partners.reader)
      partners shouldBe a[Partners]
      partners.partners(0).otherPartners.get shouldBe "Yes"
      partners.partners(1).otherPartners.get shouldBe "Yes"
      partners.partners(2).otherPartners.get shouldBe "No"

      partners.partners(1).entityType.get shouldBe "Corporate Body"
      partners.partners(1).companyNames.businessName.get shouldBe "company name"
      partners.partners(1).companyNames.tradingName.get shouldBe "trading name"
      partners.partners(1).firstName shouldBe None
      partners.partners(1).lastName shouldBe None
      partners.partners(1).doYouHaveNino shouldBe None
      partners.partners(1).nino shouldBe None
      partners.partners(1).doYouHaveVRN.get shouldBe "No"
      partners.partners(1).vrn shouldBe None
      partners.partners(1).doYouHaveUTR.get shouldBe "No"
      partners.partners(1).utr shouldBe None
      partners.partners(1).isBusinessIncorporated.get shouldBe "Yes"
      partners.partners(1).companyRegDetails.get.companyRegistrationNumber shouldBe "12345678"
      partners.partners(1).companyRegDetails.get.dateOfIncorporation shouldBe "30/10/2010"
      partners.partners(1).partnerAddress.get.addressLine1 shouldBe "address line 1"
      partners.partners(1).partnerAddress.get.addressLine2 shouldBe "address line 2"
      partners.partners(1).partnerAddress.get.addressLine3.get shouldBe "address line 3"
      partners.partners(1).partnerAddress.get.addressLine4 shouldBe None
      partners.partners(1).partnerAddress.get.postcode.get shouldBe "AA1 1AA"
    }

    "transform correctly to Business Directors Frontend Model " in {
      val directors = api4EtmpLTDJson.as[BusinessDirectors](BusinessDirectors.reader)
      directors.directors.head.directorsAndCompanySecretaries shouldBe "Director"
      directors.directors.head.personOrCompany shouldBe "person"
      directors.directors.head.firstName.get shouldBe "Example"
      directors.directors.head.lastName.get shouldBe "Exampleson"
      directors.directors.last.directorsAndCompanySecretaries shouldBe "Director"
      directors.directors.last.personOrCompany shouldBe "company"
      directors.directors.last.companyNames.get.businessName.get shouldBe "Company name"
      directors.directors.last.companyNames.get.tradingName.get shouldBe "Trading name"
      directors.directors.last.doYouHaveVRN.get shouldBe "No"
      directors.directors.last.doYouHaveUTR.get shouldBe "Yes"
      directors.directors.last.utr.get shouldBe testUtr
      directors.directors.last.doYouHaveCRN.get shouldBe "No"
    }

    "transform correctly to Business Directors Frontend Model when coOfficial is of type company" in {
      val directors = api4EtmpLTDJson.as[BusinessDirectors](BusinessDirectors.reader)
      val dl = directors.directors.last
      dl.directorsAndCompanySecretaries shouldBe "Director"
      dl.personOrCompany shouldBe "company"
      val d1 = directors.directors(1)
      d1.directorsAndCompanySecretaries shouldBe "Director and Company Secretary"
      d1.personOrCompany shouldBe "person"
      val d2 = directors.directors(2)
      d2.directorsAndCompanySecretaries shouldBe "Company Secretary"
      d2.personOrCompany shouldBe "person"
    }

    "transform correctly to Additional Business premises Frontend Model and create correct business premises json" in {
      val premises = api4EtmpLTDJson.as[AdditionalBusinessPremisesList](AdditionalBusinessPremisesList.reader)
      premises shouldBe a[AdditionalBusinessPremisesList]
      val businessPremises = Json.toJson(premises).toString()
      businessPremises should include("additionalPremises")
      businessPremises should include("additionalAddress")
      businessPremises should include("addAnother")
    }

    "transform to correct number of business premises address" in {
      val premises = api4EtmpLTDJson.as[AdditionalBusinessPremisesList](AdditionalBusinessPremisesList.reader)
      premises shouldBe a[AdditionalBusinessPremisesList]

      val businessPremises = Json.toJson(premises).toString()
      premises.premises.size shouldBe 2
      businessPremises should include("\"postcode\":\"CC1 1CC")
      businessPremises should include("\"postcode\":\"DD1 1DD")
    }

    "transform correctly to ApplicationDeclaration Frontend Model " in {
      val applicationDeclaration = api4EtmpSOPJson.as[ApplicationDeclaration](ApplicationDeclaration.reader)
      applicationDeclaration.declarationName.get shouldBe "Example Exampleson"
      applicationDeclaration.declarationRole.get shouldBe "Authorised Signatory"
    }

    "transform correctly to Trading Acitivity Frontend Model " in {
      val tradingActivity = api4EtmpSOPJson.as[TradingActivity](TradingActivity.reader)
      tradingActivity.wholesalerType.containsSlice(GenSeq("01", "02", "03", "06")) shouldBe true
      tradingActivity.typeOfAlcoholOrders.containsSlice(GenSeq("02", "03", "04", "01")) shouldBe true
      tradingActivity.doesBusinessImportAlcohol.get shouldBe "Yes"
      tradingActivity.otherWholesaler.fold("")(x => x) shouldBe ""
      tradingActivity.otherTypeOfAlcoholOrders.fold("")(x => x) shouldBe ""
    }

    "transform correctly to Products Frontend Model " in {
      val products = api4EtmpSOPJson.as[Products](Products.reader)
      products.mainCustomers.containsSlice(GenSeq("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "13")) shouldBe true
      products.productType.containsSlice(GenSeq("05", "02", "03", "04", "06", "01")) shouldBe true
      products.otherMainCustomers.fold("")(x => x) shouldBe ""
      products.otherProductType.fold("")(x => x) shouldBe ""
    }

    "transform correctly to Trading Acitivity Frontend Model when 'alcoholGoodsImported' is False" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.
              obj("alcoholGoodsImported" -> false)))), api4EtmpSOPString)
      val tradingActivity = Json.parse(updatedJson).as[TradingActivity](TradingActivity.reader)
      tradingActivity.doesBusinessImportAlcohol.get shouldBe "No"
    }

    "transform correct declaration JSON element when doYouHaveVRN -> true and vrn -> 123456789" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessDetails" -> Json.
            obj("nonProprietor" -> Json.
              obj("identification" -> Json.
                obj("doYouHaveVRN" -> true)
                .++(Json.obj("vrn" -> "123456789")))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"doYouHaveVRN\":\"Yes")
      awrsFEJson should include("\"vrn\":\"123456789")
    }

    "transform correct declaration JSON element when alcoholExported -> true and euDispatches -> true" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.obj()
              .++(Json.obj("alcoholGoodsExported" -> true))
              .++(Json.obj("euDispatches" -> true))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"exportLocation\":[\"euDispatches\",\"outsideEU\"]")

    }

    "transform correct declaration JSON element when alcoholExported -> true and euDispatches -> false" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.obj()
              .++(Json.obj("alcoholGoodsExported" -> true))
              .++(Json.obj("euDispatches" -> false))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"exportLocation\":[\"outsideEU\"]")

    }

    "transform correct declaration JSON element when alcoholExported -> false and euDispatches -> true" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.obj()
              .++(Json.obj("alcoholGoodsExported" -> false))
              .++(Json.obj("euDispatches" -> true))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"exportLocation\":[\"euDispatches\"]")

    }

    "transform correct declaration JSON element when alcoholExported -> false and euDispatches -> false" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.obj()
              .++(Json.obj("alcoholGoodsExported" -> false))
              .++(Json.obj("euDispatches" -> false))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"doYouExportAlcohol\":\"No\"")

    }

    "transform correct declaration JSON element when third party supplier -> true" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.obj()
              .++(Json.obj("thirdPartyStorageUsed" -> true))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"thirdPartyStorage\":\"Yes")

    }

    "transform correct declaration JSON element when third party supplier -> false" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.obj()
              .++(Json.obj("thirdPartyStorageUsed" -> false))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"thirdPartyStorage\":\"No")

    }

    "transform correct declaration JSON element when third party supplier -> none" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("additionalBusinessInfo" -> Json.
            obj("all" -> Json.obj()
              .++(Json.obj("thirdPartyStorageUsed" -> ""))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"thirdPartyStorage\":\"No")
    }

    "transform correct declaration JSON element when mainAddress is included" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessAddressForAwrs" -> Json.
            obj("currentAddress" -> Json.
              obj("postalCode" -> "AA1 1AA")
              .++(Json.obj("addressLine1" -> "1 Example Street"))
              .++(Json.obj("addressLine2" -> "Exampe View"))
              .++(Json.obj("addressLine3" -> "Exampe Town"))
              .++(Json.obj("addressLine4" -> "Exampeshire"))
              .++(Json.obj("countryCode" -> "GB"))))
          .++(Json.obj("additionalBusinessInfo" -> Json.obj("all" -> Json.obj("premiseAddress" -> Json.arr(Json.obj("address" -> Json.
            obj("postalCode" -> "AA1 1AA")
            .++(Json.obj("addressLine1" -> "1 Example Street"))
            .++(Json.obj("addressLine2" -> "Exampe View"))
            .++(Json.obj("addressLine3" -> "Exampe Town"))
            .++(Json.obj("addressLine4" -> "Exampeshire"))
            .++(Json.obj("countryCode" -> "GB"))))))))),
        api4EtmpSOPString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"doYouHaveVRN\":\"Yes")
      awrsFEJson should include("\"vrn\":\"123456789")
    }

    "transform correct declaration JSON element when doYouHaveUTR -> true and utr -> 123456789" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessDetails" -> Json.
            obj("nonProprietor" -> Json.
              obj("identification" -> Json.
                obj("doYouHaveUTR" -> true)
                .++(Json.obj("utr" -> "123456789")))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"doYouHaveUTR\":\"Yes")
      awrsFEJson should include("\"utr\":\"123456789")
    }

    "transform correct declaration JSON element when isBusinessIncorporated -> true " +
      "and companyRegistrationNumber -> and dateOfIncorporation -> 09/01/1995" in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessDetails" -> Json.
            obj("llpCorporateBody" -> Json.
              obj("incorporationDetails" -> Json.obj()
                .++(Json.obj("isBusinessIncorporated" -> true))
                .++(Json.obj("companyRegistrationNumber" -> "12345678"))
                .++(Json.obj("dateOfIncorporation" -> "1995-01-09")))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"isBusinessIncorporated\":\"Yes")
      awrsFEJson should include("\"companyRegistrationNumber\":\"12345678")
      awrsFEJson should include("\"dateOfIncorporation\":\"09/01/1995")
    }

    "For Sole trader  transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> true " in {
      val awrsFEModel = Json.parse(api4EtmpSOPString).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"No")
      awrsFEJson should include("\"postcode\":\"AA11AA")
      awrsFEJson should include("\"addressLine1\":\"Operating Address line 1")
      awrsFEJson should include("\"addressLine2\":\"Operating Address line 2")
    }

    "For Sole trader transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> false " in {
      val differentOperatingAddresslnLast3YearsJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessAddressForAwrs" -> Json.
            obj("differentOperatingAddresslnLast3Years" -> false))), api4EtmpSOPString)
      val previousAddressPath = JsPath \ "subscriptionType" \ "businessAddressForAwrs" \ "previousAddress"
      val updatedJson = deleteFromJson(previousAddressPath, differentOperatingAddresslnLast3YearsJson.toString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"Yes")
      awrsFEJson shouldNot include("\"postcode\":\"AA11AA")
      awrsFEJson shouldNot include("\"addressLine1\":\"Operating Address line 1")
    }

    "For Limited Compmpany transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> true " in {
      val awrsFEModel = Json.parse(api4EtmpLTDString).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"No")
      awrsFEJson should include("\"postcode\":\"AA11AA")
      awrsFEJson should include("\"addressLine1\":\"Operating Address line 1")
      awrsFEJson should include("\"addressLine2\":\"Operating Address line 2")
    }

    "For Limited Compmpany transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> false " in {
      val differentOperatingAddresslnLast3YearsJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessAddressForAwrs" -> Json.
            obj("differentOperatingAddresslnLast3Years" -> false))), api4EtmpLTDString)
      val previousAddressPath = JsPath \ "subscriptionType" \ "businessAddressForAwrs" \ "previousAddress"
      val updatedJson = deleteFromJson(previousAddressPath, differentOperatingAddresslnLast3YearsJson.toString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"Yes")
      awrsFEJson shouldNot include("\"postcode\":\"BB11BB")
      awrsFEJson shouldNot include("\"addressLine1\":\"Operating Address line 1")
    }

    "For Partnership  transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> true " in {
      val awrsFEModel = Json.parse(api4EtmpPartnershipString).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"No")
      awrsFEJson should include("\"postcode\":\"AA11AA")
      awrsFEJson should include("\"addressLine1\":\"Operating Address line 1")
      awrsFEJson should include("\"addressLine2\":\"Operating Address line 2")
    }

    "For Partnership transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> false " in {
      val differentOperatingAddresslnLast3YearsJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessAddressForAwrs" -> Json.
            obj("differentOperatingAddresslnLast3Years" -> false))), api4EtmpPartnershipString)
      val previousAddressPath = JsPath \ "subscriptionType" \ "businessAddressForAwrs" \ "previousAddress"
      val updatedJson = deleteFromJson(previousAddressPath, differentOperatingAddresslnLast3YearsJson.toString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"Yes")
      awrsFEJson shouldNot include("\"postcode\":\"AA11AA")
      awrsFEJson shouldNot include("\"addressLine1\":\"Operating Address line 1")
    }

    "For LLP/LP transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> true " in {
      val awrsFEModel = Json.parse(api4EtmpLLPString).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"No")
      awrsFEJson should include("\"postcode\":\"AA11AA")
      awrsFEJson should include("\"addressLine1\":\"Operating Address line 1")
      awrsFEJson should include("\"addressLine2\":\"Operating Address line 2")
    }

    "For LLP/LP transform correct declaration JSON element when differentOperatingAddresslnLast3Years -> false " in {
      val differentOperatingAddresslnLast3YearsJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("businessAddressForAwrs" -> Json.
            obj("differentOperatingAddresslnLast3Years" -> false))), api4EtmpLLPString)
      val previousAddressPath = JsPath \ "subscriptionType" \ "businessAddressForAwrs" \ "previousAddress"
      val updatedJson = deleteFromJson(previousAddressPath, differentOperatingAddresslnLast3YearsJson.toString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)

      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"placeOfBusinessLast3Years\":\"Yes")
      awrsFEJson shouldNot include("\"postcode\":\"AA11AA")
      awrsFEJson shouldNot include("\"addressLine1\":\"Operating Address line 1")
    }

    "For LLP/LP transform correct Partnership Details JSON element to AWRS Partnership Details JSON element" in {
      val modelFromEtmp = api4EtmpLLPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      Json.toJson(modelFromEtmp) shouldBe api5FrontendLLPJson
    }

    "For Partnership transform correct Partnership Details JSON element to AWRS Partnership Details JSON element" in {
      val modelFromEtmp = api4EtmpPartnershipJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      Json.toJson(modelFromEtmp) shouldBe api5FrontendPartnershipJson
    }

    "For Sole Trader transform correct declaration JSON element when useAlternateContactAddress -> true " in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("contactDetails" -> Json.
            obj("useAlternateContactAddress" -> true)
            .++(Json.obj("address" -> Json.obj()
              .++(Json.obj("addressLine1" -> "3 Example street"))
              .++(Json.obj("addressLine2" -> "Example Suburbs"))
              .++(Json.obj("addressLine3" -> "Example Park"))
              .++(Json.obj("addressLine4" -> "Exampledon"))
              .++(Json.obj("postalCode" -> "AA1 1AA"))
              .++(Json.obj("countryCode" -> "GB")))))),
        api4EtmpSOPString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"contactAddressSame\":\"No")
      awrsFEJson should include("\"addressLine1\":\"3 Example street")
      awrsFEJson should include("\"addressLine2\":\"Example Suburbs")
    }

    "For Sole Trader transform correct declaration JSON element when useAlternateContactAddress -> false " in {
      val awrsFEModel = Json.parse(api4EtmpSOPString).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"contactAddressSame\":\"Yes")
      awrsFEJson shouldNot include("\"addressLine1\":\"3 Example street")
    }

    "For LTD transform correct declaration JSON element when useAlternateContactAddress -> true " in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("contactDetails" -> Json.
            obj("useAlternateContactAddress" -> true)
            .++(Json.obj("address" -> Json.obj()
              .++(Json.obj("addressLine1" -> "3 Example street"))
              .++(Json.obj("addressLine2" -> "Example Suburbs"))
              .++(Json.obj("addressLine3" -> "Example Park"))
              .++(Json.obj("addressLine4" -> "Exampledon"))
              .++(Json.obj("postalCode" -> "AA1 1AA"))
              .++(Json.obj("countryCode" -> "GB")))))),
        api4EtmpLTDString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"contactAddressSame\":\"No")
      awrsFEJson should include("\"addressLine1\":\"3 Example street")
      awrsFEJson should include("\"addressLine2\":\"Example Suburbs")
    }

    "For LTD transform correct declaration JSON element when useAlternateContactAddress -> false " in {
      val awrsFEModel = Json.parse(api4EtmpLTDString).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"contactAddressSame\":\"Yes")
      awrsFEJson shouldNot include("\"addressLine1\":\"3 Example street")
    }

    "For Partnership transform correct JSON element when useAlternateContactAddress -> true " in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("contactDetails" -> Json.
            obj("useAlternateContactAddress" -> true)
            .++(Json.obj("address" -> Json.obj()
              .++(Json.obj("addressLine1" -> "3 Example street"))
              .++(Json.obj("addressLine2" -> "Example Suburbs"))
              .++(Json.obj("addressLine3" -> "Example Park"))
              .++(Json.obj("addressLine4" -> "Exampledon"))
              .++(Json.obj("postalCode" -> "AA1 1AA"))
              .++(Json.obj("countryCode" -> "GB")))))),
        api4EtmpPartnershipString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"contactAddressSame\":\"No")
      awrsFEJson should include("\"addressLine1\":\"3 Example street")
      awrsFEJson should include("\"addressLine2\":\"Example Suburbs")
    }

    "For Partnership transform correct declaration JSON element when useAlternateContactAddress -> false " in {
      val awrsFEModel = Json.parse(api4EtmpPartnershipString).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"contactAddressSame\":\"Yes")
      awrsFEJson shouldNot include("\"addressLine1\":\"3 Example street")
    }

    "For LLP/LP transform correct JSON element when useAlternateContactAddress -> true " in {
      val updatedJson = updateJson(Json.
        obj("subscriptionType" -> Json.
          obj("contactDetails" -> Json.
            obj("useAlternateContactAddress" -> true)
            .++(Json.obj("address" -> Json.obj()
              .++(Json.obj("addressLine1" -> "3 Example street"))
              .++(Json.obj("addressLine2" -> "Example Suburbs"))
              .++(Json.obj("addressLine3" -> "Example Park"))
              .++(Json.obj("addressLine4" -> "Exampledon"))
              .++(Json.obj("postalCode" -> "AA1 1AA"))
              .++(Json.obj("countryCode" -> "GB")))))),
        api4EtmpLLPString)

      val awrsFEModel = Json.parse(updatedJson).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"contactAddressSame\":\"No")
      awrsFEJson should include("\"addressLine1\":\"3 Example street")
      awrsFEJson should include("\"addressLine2\":\"Example Suburbs")
    }

    "For LLP/LP transform correct declaration JSON element when useAlternateContactAddress -> false " in {
      val awrsFEModel = Json.parse(api4EtmpLLPString).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()
      awrsFEJson should include("\"contactAddressSame\":\"Yes")
      awrsFEJson shouldNot include("\"addressLine1\":\"3 Example street")
    }

    "return a list of 1 Additional premise with only additional premises as Some(No) if there are no premises " in {
      val awrsFEModel = Json.parse(api4EtmpSOPString).as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should include("\"additionalPremises\":\"No")
    }

    "return a list of Group members with the last group member's add another member set to No " in {
      val groupMember = api4EtmpLTDGRPJson.as[GroupMembers](GroupMembers.reader)

      groupMember.members.last.addAnotherGrpMember.get shouldBe "No"
    }

    "transform correctly to Group members Frontend Model for business type 'LTD' " in {
      val groupMember = api4EtmpLTDGRPJson.as[GroupMembers](GroupMembers.reader)
      groupMember.members.head.companyNames.businessName.get shouldBe "ExampleName"
      groupMember.members.head.companyNames.tradingName.get shouldBe "Example"
      groupMember.members.head.address.get.addressLine1 shouldBe "16 Example Road"
      groupMember.members.head.address.get.addressLine2 shouldBe "Exampleland"
      groupMember.members.head.address.get.postcode.get shouldBe "AA1 1AA"

      groupMember.members.head.doYouHaveVRN.get shouldBe "Yes"
      groupMember.members.head.vrn shouldBe Some("000000000")

      groupMember.members.head.doYouHaveUTR.get shouldBe "No"

      groupMember.members.head.isBusinessIncorporated.get shouldBe "No"

      groupMember.members(1).companyNames.businessName.get shouldBe "Another Example"
      groupMember.members(1).companyNames.tradingName.get shouldBe "Example Trade"
      groupMember.members(1).address.get.addressLine1 shouldBe "22 Example Road"
      groupMember.members(1).address.get.addressLine2 shouldBe "Exampleland"
      groupMember.members(1).address.get.postcode.get shouldBe "AA1 1AA"

    }

    "transform correctly to Group members Frontend Model for business type 'LLP' " in {
      val groupMember = api4EtmpLLPGRPJson.as[GroupMembers](GroupMembers.reader)
      groupMember.members.head.companyNames.businessName.get shouldBe "ExampleName"
      groupMember.members.head.companyNames.tradingName.get shouldBe "Example"

      groupMember.members.head.doYouHaveUTR.get shouldBe "No"

      groupMember.members.head.doYouHaveVRN.get shouldBe "Yes"
      groupMember.members.head.vrn shouldBe Some("000000000")

      groupMember.members.head.isBusinessIncorporated.get shouldBe "No"

      groupMember.members.head.address.get.addressLine1 shouldBe "16 Example Road"
      groupMember.members.head.address.get.addressLine2 shouldBe "Exampleland"
      groupMember.members.head.address.get.postcode.get shouldBe "AA1 1AA"

      groupMember.members(1).companyNames.businessName.get shouldBe "Another Example"
      groupMember.members(1).companyNames.tradingName.get shouldBe "Example Trade"
      groupMember.members(1).address.get.addressLine1 shouldBe "22 Example Road"
      groupMember.members(1).address.get.addressLine2 shouldBe "Exampleland"
      groupMember.members(1).address.get.postcode.get shouldBe "AA1 1AA"
    }

    "transform correctly to Group Declaration frontend model" in {
      val groupDeclaration = api4EtmpLLPGRPJson.as[GroupDeclaration](GroupDeclaration.reader)
      groupDeclaration.groupRepConfirmation shouldBe true
    }

    "For LLP_GRP transform correct declaration JSON element to include group elements" in {
      val awrsFEModel = api4EtmpLLPGRPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      Json.toJson(awrsFEModel) shouldBe api5FrontendLLPGroupJson
    }

    "For LTD_GRP transform correct declaration JSON element to include group elements " in {
      val awrsFEModel = api4EtmpLTDGRPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      Json.toJson(awrsFEModel) shouldBe api5FrontendLTDGroupJson
    }

    "For Sole Trader, make sure Group Details are not present " in {
      val awrsFEModel = api4EtmpSOPJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should not include "groupMembers"
      awrsFEJson should not include "groupRepBusinessDetails"
    }

    "For Limited Company without groups, make sure Group Details are not present " in {
      val awrsFEModel = api4EtmpLTDJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      val awrsFEJson = Json.toJson(awrsFEModel).toString()

      awrsFEJson should not include "groupMembers"
      awrsFEJson should not include "groupRepBusinessDetails"
    }

    "transform correctly to AWRSFEModel Frontend Model with new business attributes " in {
      val awrsFEModelDetails = api4EtmpLTDNewBusinessJson.as[AWRSFEModel](AWRSFEModel.etmpReader)
      awrsFEModelDetails shouldBe a[AWRSFEModel]
      awrsFEModelDetails.subscriptionTypeFrontEnd.businessDetails.get.newAWBusiness.get.proposedStartDate shouldBe Some("30/10/2016")
    }
  }

}
