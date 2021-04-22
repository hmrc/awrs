/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JodaWrites._
import play.api.libs.json._
import utils.AwrsTestJson.testNino
import utils.TestUtil._
import utils.{AwrsTestJson, TestUtil}

class EtmpModelHelperSpec extends PlaySpec with AwrsTestJson with WordSpecLike with MustMatchers {

  val grpJoinDate: LocalDate = LocalDate.now()

  val LTDGroupsJson: JsValue = Json.parse(api4FrontendLTDGRPWithCompaniesString.replace(f"$$grpJoinDate", grpJoinDate.toString))

  object TestEtmpModelHelper extends EtmpModelHelper

  "toEtmpDeclaration method of EtmpModelHelper " must {
    "transform correct declaration JSON element when declarationRole -> Director " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("applicationDeclaration" -> Json.obj("declarationRole" -> "Director"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpDeclaration(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("nameOfPerson")
      etmpJson should include("\"statusOfPerson\":\"Director")
      etmpJson shouldNot include("personStatusOther")
      etmpJson should include("informationIsAccurateAndComplete")
    }


    "transform correct declaration JSON element when declarationRole -> Company Secretary" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("applicationDeclaration" -> Json.obj("declarationRole" -> "Company Secretary"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpDeclaration(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"statusOfPerson\":\"Company Secretary")
      etmpJson shouldNot include("personStatusOther")
    }


    "transform correct declaration JSON element when declarationRole -> Director and Company Secretary " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("applicationDeclaration" -> Json.obj("declarationRole" -> "Director and Company Secretary"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpDeclaration(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"statusOfPerson\":\"Director and Company Secretary")
      etmpJson shouldNot include("personStatusOther")
    }


    "transform correct declaration JSON element when declarationRole -> Authorised Signatory " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("applicationDeclaration" -> Json.obj("declarationRole" -> "Authorised Signatory"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpDeclaration(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"statusOfPerson\":\"Authorised Signatory")
      etmpJson shouldNot include("personStatusOther")
    }


    "transform correct declaration JSON element when declarationRole -> Other " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("applicationDeclaration" -> Json.obj("declarationRole" -> "Other"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpDeclaration(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"statusOfPerson\":\"Other")
      etmpJson should include("personStatusOther")
    }
  }

  "toEtmpAddress method of EtmpModelHelper " must {
    "transform correct address JSON element " in {

      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpAddress(awrsModel.subscriptionTypeFrontEnd.suppliers.reduceLeft((x, y) => x).
        suppliers.head.supplierAddress.reduceLeft((p, q) => p)).toString()
      etmpJson should include("postalCode")
      etmpJson should include("addressLine1")
      etmpJson should include("addressLine2")
      etmpJson should include("\"countryCode\":\"GB")
    }

    "transform non-UK address JSON element " in {

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("suppliers" -> Json.
            obj("suppliers" -> Json.arr(
              Json.obj("alcoholSuppliers" -> "Yes")
                .++(Json.obj("supplierName" -> "Example Supplier"))
                .++(Json.obj("vatRegistered" -> "No"))
                .++(Json.obj("additionalSupplier" -> "No"))
                .++(Json.obj("supplierAddress" ->
                  Json.obj("addressLine1" -> "3 Example Road")
                    .++(Json.obj("addressLine2" -> "Paris"))
                    .++(Json.obj("addressCountry" -> "France"))
                    .++(Json.obj("addressCountryCode" -> "FR"))
                )))))),
        api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAddress(awrsModel.subscriptionTypeFrontEnd.suppliers.reduceLeft((x, y) => x).
        suppliers.head.supplierAddress.reduceLeft((p, q) => p)).toString()
      etmpJson should not include "postalCode"
      etmpJson should include("addressLine1")
      etmpJson should include("addressLine2")
      etmpJson should include("\"countryCode\":\"FR")
    }


    "check if json contains one premise " in {
      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("additionalPremises" -> Json.
            obj("premises" -> Json.arr(Json.obj("additionalPremises" -> "No"))))),
        api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]


      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"numberOfPremises\":\"1")
    }
  }

  "createEtmpSupplierJson method of EtmpModelHelper " must {
    "transform to correct JSON element without supplier " in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "suppliers" \ "suppliers", api4FrontendSOPString)
      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("suppliers" -> Json.
            obj("suppliers" -> Json.arr(
              Json.obj("alcoholSuppliers" -> "No")
            )))),
        deletedJson)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.createEtmpSupplierJson(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson shouldNot include("supplier")
      etmpJson shouldNot include("isSupplierVatRegistered")
      etmpJson shouldNot include("address")
      etmpJson shouldNot include("addressLine1")
      etmpJson shouldNot include("addressLine2")
      etmpJson shouldNot include("postalCode")
      etmpJson shouldNot include("countryCode")
    }
  }

  "toEtmpNewAWBusiness method of EtmpModelHelper " must {
    "transform to correct JSON element which is newAWBusiness" in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()
      etmpJson should include("newAWBusiness")
      etmpJson should include("\"newAWBusiness\":false")
    }
  }


  "toEtmpAdditionalBusinessInfoPartnerCorporateBody method of EtmpModelHelper " must {
    "transform to correct JSON element and count correct numberOfCoOfficials" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfoPartnerCorporateBody(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("partnerCorporateBody")
      etmpJson should include("\"numberOfCoOfficials\":\"5")
      etmpJson should include("coOfficialDetails")
    }
  }

  "toEtmpGroupMember method of EtmpModelHelper " must {
    "transform to correct JSON element and give correct group members" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpGroupMember(awrsModel.subscriptionTypeFrontEnd.groupMembers.get.members.head).toString()
      etmpJson should include("names")
      etmpJson should include("tradingName")
      etmpJson should include("incorporationDetails")
      etmpJson should include("groupJoiningDate")
      etmpJson should include("address")
      etmpJson should include("doYouHaveVRN")
      etmpJson should include("doYouHaveUTR")
    }

    "transform to correct JSON element and give correct group members with trading name" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpGroupMember(awrsModel.subscriptionTypeFrontEnd.groupMembers.get.members.head).toString()
      etmpJson should include("names")
      etmpJson should include("companyName")
      etmpJson should include("tradingName")
    }

    "transform to correct JSON element and give correct group members without trading name" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpGroupMember(awrsModel.subscriptionTypeFrontEnd.groupMembers.get.members.last).toString()
      etmpJson should include("names")
      etmpJson should include("companyName")
      etmpJson should not include "tradingName"
    }

    "transform to correct JSON element and give correct group members (actual output)" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpGroupMember(awrsModel.subscriptionTypeFrontEnd.groupMembers.get.members.head)

      val updatedJson = Json.parse(updateJson(Json.
        obj("groupJoiningDate" -> LocalDate.now()), commonGroupMemberString))

      val today = LocalDate.now()
      etmpJson.toString() should include("\"groupJoiningDate\":\"" + today)
      etmpJson shouldBe updatedJson
    }
  }

  "toEtmpGroupMembers method of EtmpModelHelper " must {
    "transform to correct JSON element and give correct group members" in {

      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpGroupMembers(awrsModel.subscriptionTypeFrontEnd)

      val updatedGroupMembersJsonString = commonGroupMembersString.replace(f"$$grpJoinDate", grpJoinDate.toString)
      val groupMembersJson = Json.parse(updatedGroupMembersJsonString)

      etmpJson shouldBe groupMembersJson

    }
  }

  "toEtmpGroupMemberDetails method of EtmpModelHelper " must {
    "transform to correct JSON element and count correct numberOfGroupMembers" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpGroupMemberDetails(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"numberOfGrpMembers\":\"2")
      etmpJson should include("groupMember")
    }
  }

  "toEtmpAdditionalBusinessInfo method of EtmpModelHelper " must {
    "transform to correct JSON element and count correct number of premises including the main premise" in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")
      etmpJson should include("typeOfWholesaler")
      etmpJson should include("\"cashAndCarry\":true")
      etmpJson should include("\"offTradeSupplierOnly\":true")
      etmpJson should include("typeOfCustomers")
      etmpJson should include("pubs")
      etmpJson should include("productsSold")
      etmpJson should include("all")
      etmpJson should include("numberOfPremises")
      etmpJson should include("\"numberOfPremises\":\"3")
      etmpJson should include("suppliers")
    }

    "transform data to include 3rd party storage, do you export and EU dispatches " in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")
      etmpJson should include("typeOfWholesaler")
      etmpJson should include("\"cashAndCarry\":true")
      etmpJson should include("\"offTradeSupplierOnly\":true")
      etmpJson should include("typeOfCustomers")
      etmpJson should include("pubs")
      etmpJson should include("productsSold")
      etmpJson should include("all")
      etmpJson should include("numberOfPremises")
      etmpJson should include("\"numberOfPremises\":\"3")
      etmpJson should include("suppliers")
      etmpJson should include("thirdPartyStorageUsed")
      etmpJson should include("euDispatches")
      etmpJson should include("alcoholGoodsExported")
    }

    "transform data to include 3rd party storage, cvalue should be true " in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")
      etmpJson should include("typeOfWholesaler")
      etmpJson should include("\"cashAndCarry\":true")
      etmpJson should include("\"offTradeSupplierOnly\":true")
      etmpJson should include("typeOfCustomers")
      etmpJson should include("pubs")
      etmpJson should include("productsSold")
      etmpJson should include("all")
      etmpJson should include("numberOfPremises")
      etmpJson should include("\"numberOfPremises\":\"3")
      etmpJson should include("suppliers")
      etmpJson should include("thirdPartyStorageUsed\":true")
      etmpJson should include("euDispatches")
      etmpJson should include("alcoholGoodsExported")
    }

    "transform data to include 3rd party storage, value should be false " in {

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("tradingActivity" -> Json.obj()
            .++(Json.obj("thirdPartyStorage" -> "No")))),
        api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")
      etmpJson should include("typeOfWholesaler")
      etmpJson should include("\"cashAndCarry\":true")
      etmpJson should include("\"offTradeSupplierOnly\":true")
      etmpJson should include("typeOfCustomers")
      etmpJson should include("pubs")
      etmpJson should include("productsSold")
      etmpJson should include("all")
      etmpJson should include("numberOfPremises")
      etmpJson should include("\"numberOfPremises\":\"3")
      etmpJson should include("suppliers")
      etmpJson should include("thirdPartyStorageUsed\":false")
      etmpJson should include("euDispatches")
      etmpJson should include("alcoholGoodsExported")
    }

    "transform doYouExportAlcohol(No)to alcoholGoodsExported=false and EUdispatches=false " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("tradingActivity" -> Json.obj("doYouExportAlcohol" -> Some("No")))), api4FrontendSOPString)
      val afterDeleteJson =  deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "tradingActivity" \ "exportLocation", updatedJson)

      val awrsModel = Json.parse(afterDeleteJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")

      etmpJson should include("\"alcoholGoodsExported\":false")
      etmpJson should include("\"euDispatches\":false")

    }

    "transform exportLocation(euDispatches)to alcoholGoodsExported=false and EUdispatches=true " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("tradingActivity" -> Json.obj("exportLocation" -> List("euDispatches")))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")

      etmpJson should include("\"alcoholGoodsExported\":false")
      etmpJson should include("\"euDispatches\":true")

    }

    "transform doYouExportAlcohol(outsideEU)to alcoholGoodsExported=true and EUdispatches=false " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("tradingActivity" -> Json.obj("exportLocation" -> List("outsideEU")))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")

      etmpJson should include("\"alcoholGoodsExported\":true")
      etmpJson should include("\"euDispatches\":false")

    }

    "transform doYouExportAlcohol(outsideEU, euDispatches)to alcoholGoodsExported=true and EUdispatches=true " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("tradingActivity" -> Json.obj("exportLocation" -> List("outsideEU", "euDispatches")))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")

      etmpJson should include("\"alcoholGoodsExported\":true")
      etmpJson should include("\"euDispatches\":true")
    }

    "transform doesBusinessImportAlcohol: 'No' to alcoholGoodsImported: false " in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("tradingActivity" -> Json.obj("doesBusinessImportAlcohol" -> "No"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"alcoholGoodsImported\":false")
    }

    "transform doesBusinessImportAlcohol: 'Yes' to alcoholGoodsImported: true " in {
      val awrsModel = Json.parse(api4FrontendSOPString).as[AWRSFEModel]

      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"alcoholGoodsImported\":true")
    }
  }

  "toEtmpBusinessAddressForAwrs method of EtmpModelHelper " must {
    "transform to correct JSON element when placeOfBusinessLast3Years is Yes and operatingDuration is 2 to 5 years" in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("currentAddress")
      etmpJson should include("communicationDetails")
      etmpJson should include("differentOperatingAddresslnLast3Years")
      etmpJson should include("\"operatingDuration\":\"2 to 5 years")
    }
  }
  "toEtmpTypeOfAlcoholOrders method of EtmpModelHelper " must {
    "transform to correct JSON element for typeOfAlcoholOrders" in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("all")
      etmpJson should include("\"onlineOnly\":true")
      etmpJson should include("\"onlineAndTel\":true")
      etmpJson should include("onlineTelAndPhysical")
      etmpJson should include("\"other\":true")
      etmpJson should include("\"typeOfOrderOther\":\"Other alcohol order")
      etmpJson should include("typeOfOrderOther")
      etmpJson should include("\"typeOfProductOther\":\"Other product type")
      etmpJson should include("typeOfProductOther")
      etmpJson should include("\"typeOfWholesalerOther\":\"Other wholesaler type")
      etmpJson should include("typeOfWholesalerOther")
    }
  }

  "toEtmpBusinessAddressForAwrs method of EtmpModelHelper " must {

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is less than 2 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "Less than 2 years"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"0 to 2 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 2 to 4 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "2 to 4 years"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"2 to 5 years")
    }

    "transform to correct JSON element and count correct operatingDuration when placeOfBusinessLast3Years is Yes and operatingDuration is 5 to 9 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "5 to 9 years"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"5 to 10 years")
    }

    "transform to correct JSON element and count correct operatingDuration when placeOfBusinessLast3Years is Yes and operatingDuration is over 10 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "over 10 years"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"over 10 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 0 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "0"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"0 to 2 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 1 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "1"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"0 to 2 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 2 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "2"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"2 to 5 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 4 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "4"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"2 to 5 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 5 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "5"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"5 to 10 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 9 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "9"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"5 to 10 years")
    }

    "transform to correct JSON element placeOfBusinessLast3Years is Yes and operatingDuration is 20 years" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("placeOfBusiness" -> Json.obj("operatingDuration" -> "20"))), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"over 10 years")
    }

    "transform to correct JSON element and count correct operatingDuration when placeOfBusinessLast3Years is No" in {
      val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("businessContacts" ->
        Json.obj("placeOfBusinessLast3Years" -> "No", "operatingDuration" -> "2 to 5 years",
          "placeOfBusinessAddressLast3Years" -> Json.obj(
            "postcode" -> "AA1 1AA",
            "addressLine1" -> "10 Example Apartments",
            "addressLine2" -> "16 Example Crescent"
          ))
      )), api4FrontendSOPString)
      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("\"operatingDuration\":\"2 to 5 years")
    }
  }

  "toEtmpLegalEntity method of EtmpModelHelper " must {
    "return \"Sole Trader\" as Entity type" in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpLegalEntity(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include(LegalEntityType.SOLE_TRADER)
    }
  }

  "toEtmpLegalEntity method of EtmpModelHelper " must {
    "return \"Corporate Body\" as Entity type" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpLegalEntity(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include(LegalEntityType.CORPORATE_BODY)
    }
  }

  "toEtmpLegalEntity method of EtmpModelHelper " must {
    "return \"Limited Liability Partnership\" as Entity type" in {
      val awrsModel = api4FrontendLLPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpLegalEntity(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include(LegalEntityType.LIMITED_LIABILITY_PARTNERSHIP)
      val awrsModellp = api4FrontendLPJson.as[AWRSFEModel]
      val etmpJsonlp = TestEtmpModelHelper.toEtmpLegalEntity(awrsModellp.subscriptionTypeFrontEnd).toString()
      etmpJsonlp should include(LegalEntityType.LIMITED_LIABILITY_PARTNERSHIP)
    }
  }

  "toEtmpBusinessDetails method of EtmpModelHelper " must {

    "create valid etmp json" in {
      val awrsModel = api4FrontendLLPJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()
      TestUtil.validateJson(schemaPath, etmpJson) shouldBe true
    }

    "return \"soleProprietor\" as business type" in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessDetails(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include(ProprietorType.soleTrader)
    }
  }

  "toEtmpSuppliers method of EtmpModelHelper " must {
    "transform to correct Supplier JSON element " in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpSupplier(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("supplier")
      etmpJson should include("isSupplierVatRegistered")
      etmpJson should include("address")
      etmpJson should include("addressLine1")
      etmpJson should include("addressLine2")
      etmpJson should include("postalCode")
      etmpJson should include("countryCode")
    }
  }

  "toEtmpSupplierArray method of EtmpModelHelper " must {
    "transform to correct Supplier JSON element " in {
      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]
      TestEtmpModelHelper.toEtmpAdditionalBusinessInfo(awrsModel.subscriptionTypeFrontEnd).toString()
    }
  }

  "toEtmpPartnership method of EtmpModelHelper " must {
    "transform to correct JSON element and count correct number of partners" in {
      val awrsModel = api4FrontendPartnershipJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpPartnership(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("numberOfPartners")
      etmpJson should include("partnerDetails")
      etmpJson should include("entityType")
      etmpJson should include("partnerAddress")
      etmpJson should include("addressLine1")
      etmpJson should include("addressLine2")
      etmpJson should include("postalCode")
      etmpJson should include("countryCode")
      etmpJson should include("individual")
      etmpJson should include("name")
      etmpJson should include("firstName")
      etmpJson should include("lastName")
      etmpJson should include("\"doYouHaveNino\":true")
      etmpJson should include(testNino)
      etmpJson should include("\"numberOfPartners\":\"3")
    }

    "transform to correct ETMP Business Details JSON element with only one partnershipDetail - Individual" in {

      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "partnership" \ "partners", api4FrontendLLPString)
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

      val etmpson = TestEtmpModelHelper.toEtmpBaseBusinessDetails(awrsModel.subscriptionTypeFrontEnd)

      etmpson shouldBe Json.parse(commonIndPartnershipString)
    }

    "transform to correct ETMP Business Details JSON element with only one partnershipDetail - Corporate Body" in {
      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "partnership" \ "partners", api4FrontendLLPString)
      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" ->
          Json.obj("partnership" ->
            Json.obj("partners" ->
              Json.arr(
                Json.obj(
                  "entityType" -> "Corporate Body",
                  "partnerAddress" -> Json.obj(
                    "postcode" -> "AA1 1AA",
                    "addressLine1" -> "address line 1",
                    "addressLine2" -> "address line 2",
                    "addressLine3" -> "address line 3",
                    "addressLine4" -> "address line 4",
                    "countryCode" -> "GB"),
                  "companyNames" -> Json.obj(
                    "businessName" -> "company Name",
                    "doYouHaveTradingName" -> "Yes",
                    "tradingName" -> "trading Name"),
                  "doYouHaveVRN" -> "No",
                  "doYouHaveUTR" -> "No"
                )
              )
            )
          )
        ),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpson = TestEtmpModelHelper.toEtmpBaseBusinessDetails(awrsModel.subscriptionTypeFrontEnd)

      etmpson shouldBe Json.parse(commonCorpBodyPartnershipString)
    }

    "transform to correct ETMP Business Details JSON element with  only one partnershipDetail - Sole Trader" in {
      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "partnership" \ "partnerDetails", api4FrontendLLPString)
      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("partnership" -> Json.
            obj("partners" -> Json.arr(Json.obj()
              .++(Json.obj("entityType" -> "Sole Trader"))
              .++(Json.obj("partnerAddress" -> Json.
                obj("postcode" -> "AA1 1AA")
                .++(Json.obj("addressLine1" -> "address line 1"))
                .++(Json.obj("addressLine2" -> "address line 2"))
                .++(Json.obj("addressLine3" -> "address line 3"))
                .++(Json.obj("addressLine4" -> "address line 4"))
                .++(Json.obj("countryCode" -> "GB"))
              ))
              .++(
                Json.obj(
                  "companyNames" -> Json.obj(
                    "doYouHaveTradingName" -> "Yes",
                    "tradingName" -> "trading name")
                )
              )
              .++(Json.obj("firstName" -> "sole first name"))
              .++(Json.obj("lastName" -> "sole last name"))
              .++(Json.obj("doYouHaveNino" -> "No"))
              .++(Json.obj("doYouHaveVRN" -> "No"))
              .++(Json.obj("doYouHaveUTR" -> "No"))
            )))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpson = TestEtmpModelHelper.toEtmpBaseBusinessDetails(awrsModel.subscriptionTypeFrontEnd)

      etmpson shouldBe Json.parse(commonSOPPartnershipString)
    }

    "transform to correct ETMP Business Details JSON element with no Incorporation Details and no trading name" in {
      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "companyRegDetails", api4FrontendLLPString)
      val finalDeletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "isBusinessIncorporated", deletedJson)
      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("partnership" -> Json.
            obj("partners" -> Json.arr(Json.obj()
              .++(Json.obj("entityType" -> "Sole Trader"))
              .++(Json.obj("partnerAddress" -> Json.
                obj("postcode" -> "AA1 1AA")
                .++(Json.obj("addressLine1" -> "address line 1"))
                .++(Json.obj("addressLine2" -> "address line 2"))
                .++(Json.obj("addressLine3" -> "address line 3"))
                .++(Json.obj("addressLine4" -> "address line 4"))
                .++(Json.obj("countryCode" -> "GB"))
              ))
              .++(
                Json.obj(
                  "companyNames" -> Json.obj(
                    "doYouHaveTradingName" -> "No")
                )
              )
              .++(Json.obj("firstName" -> "sole first name"))
              .++(Json.obj("lastName" -> "sole last name"))
              .++(Json.obj("doYouHaveNino" -> "No"))
              .++(Json.obj("doYouHaveVRN" -> "No"))
              .++(Json.obj("doYouHaveUTR" -> "No"))
            )))),
        finalDeletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpson = TestEtmpModelHelper.toEtmpBaseBusinessDetails(awrsModel.subscriptionTypeFrontEnd)

      etmpson shouldBe Json.parse(commonSOPPartnershipNoRegString)
    }

    "transform to correct ETMP Business Details JSON element with groups date" in {
      val awrsModel = api4FrontendLLPGRPJson.as[AWRSFEModel]

      val etmpson = TestEtmpModelHelper.toEtmpBaseBusinessDetails(awrsModel.subscriptionTypeFrontEnd)

      val llpBusDetGrpUpdatedJson = Json.parse(updateJson(
        Json.obj("llpCorporateBody" -> Json.obj()
          .++(Json.obj("dateGrpRepJoined" -> LocalDate.now()))),
        commonLLPBusDetailsGroupString))

      etmpson shouldBe llpBusDetGrpUpdatedJson
    }
  }

  "toEtmpPartnershipBusinessDetails method of EtmpModelHelper " must {
    "transform to correct PartnershipBusinessDetails JSON element " in {
      val awrsModel = api4FrontendPartnershipJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBaseBusinessDetails(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should include("tradingName")
      etmpJson should include("doYouHaveVRN")
      etmpJson should include("vrn")
      etmpJson should include("doYouHaveUTR")
      etmpJson should include("utr")
      etmpJson should include("\"numberOfPartners\":\"3")
    }
  }

  "toEtmpBusinessDirectors method of EtmpModelHelper " must {
    "transform to correct coOfficialDetails - IndividualAsDirector JSON element " in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessDirectors(awrsModel.subscriptionTypeFrontEnd.businessDirectors.get).toString()
      etmpJson should include("individual")
      etmpJson should include("coOfficial")
      etmpJson should include("status")
      etmpJson should include("name")
      etmpJson should include("firstName")
      etmpJson should include("lastName")
      etmpJson should include("identification")
    }

    "transform to correct coOfficialDetails - CompanyAsDirector JSON element " in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessDirectors(awrsModel.subscriptionTypeFrontEnd.businessDirectors.get).toString()

      etmpJson should include("company")
      etmpJson should include("status")
      etmpJson should include("companyName")
      etmpJson should include("tradingName")
      etmpJson should include("doYouHaveVRN")
      etmpJson should include("doYouHaveUTR")
      etmpJson should include("utr")
      etmpJson should include("doYouHaveCRN")
      etmpJson should include("companyRegNumber")

    }

    "transform to correct order of coOfficialDetails JSON element " in {

      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessDirectors(awrsModel.subscriptionTypeFrontEnd.businessDirectors.get)

      etmpJson shouldBe commonDirectorsJson
    }

    "transform to correct address when no postcode, line 3 and line 4 are present " in {

      val businessAddressPath = JsPath \ "subscriptionTypeFrontEnd" \ "businessCustomerDetails" \ "businessAddress"
      val deletedJson = deleteFromJson(businessAddressPath, api4FrontendSOPString)
      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessCustomerDetails" -> Json.
            obj("businessAddress" ->
              Json.obj("line_1" -> "600 Example Street")
                .++(Json.obj("line_2" -> "New Suburb"))
                .++(Json.obj("country" -> "Spain"))
            ))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpBusinessAddressForAwrs(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpJson should not include "addressLine3"
      etmpJson should not include "addressLine4"
      etmpJson should not include "postalCode"
    }
  }

  "toEtmpCorporateBusinessDetails method of EtmpModelHelper " must {
    "transform to correct toEtmpCorporateBusinessDetails JSON element with groups date " in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("tradingName")
      etmpJson should include("doYouHaveVRN")
      etmpJson should include("doYouHaveUTR")
      etmpJson should include("incorporationDetails")
      etmpJson should include(LocalDate.now().toString)
    }

    "transform to correct toEtmpCorporateBusinessDetails JSON element without groups date " in {
      val awrsModel = api4FrontendLTDJson.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("tradingName")
      etmpJson should include("doYouHaveVRN")
      etmpJson should include("doYouHaveUTR")
      etmpJson should include("incorporationDetails")
      etmpJson should not include LocalDate.now().toString
    }

    "transform to correct toEtmpCorporateBusinessDetails with company Registration Number converted to Upper case " in {
      val deletedJson = deleteFromJson(JsPath \ "subscriptionTypeFrontEnd" \ "businessRegistrationDetails" \ "companyRegDetails" \ "companyRegistrationNumber", api4FrontendLTDString)

      val updatedJson = updateJson(Json.
        obj("subscriptionTypeFrontEnd" -> Json.
          obj("businessRegistrationDetails" -> Json.
            obj("companyRegDetails" -> Json.obj("companyRegistrationNumber" -> "7hkjhjh")
            ))),
        deletedJson)

      val awrsModel = Json.parse(updatedJson).as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()

      etmpJson should include("\"llpCorporateBody\":{\"incorporationDetails\":{\"isBusinessIncorporated\":true,\"companyRegistrationNumber\":\"7HKJHJH\"")
    }
  }

  "toGroupDeclaration method of EtmpModelHelper " must {
    "transform to correct llpCorporate details JSON element when group declaration object exists" in {
      val awrsModel = LTDGroupsJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toGroupDeclaration(awrsModel.subscriptionTypeFrontEnd).toString()

      etmpJson should include("\"creatingAGroup\":true")
      etmpJson should include("\"groupRepConfirmation\":true")
    }

    "transform to correct llpCorporate details JSON element when group declaration object does not exist" in {
      val awrsModel = api4FrontendLTDJson.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toGroupDeclaration(awrsModel.subscriptionTypeFrontEnd).toString()

      etmpJson should include("\"creatingAGroup\":false")
      etmpJson should include("\"groupRepConfirmation\":false")
    }
  }

  "noToTrue method of EtmpModelHelper " must {
    "transform No to true" in {
      EtmpModelHelper.NotoTrue("No") should be(true)
    }
    "transform Yes to false" in {
      EtmpModelHelper.NotoTrue("Yes") should be(false)
    }
    "transform _ to false" in {
      EtmpModelHelper.NotoTrue("") should be(false)
    }
  }

  "if changeIndicator model is included in the json, etmpModeHelper" must {
    "create json with changeIndicators " in {

      val API6Json = api6FrontendLTDJson

      val awrsModel = API6Json.as[AWRSFEModel]
      val etmpJson = TestEtmpModelHelper.toEtmpChangeIndicators(awrsModel.subscriptionTypeFrontEnd).toString()

      etmpJson should include("additionalBusinessInfoChanged")
      etmpJson should include("businessAddressChanged")
      etmpJson should include("businessDetailsChanged")
      etmpJson should include("contactDetailsChanged")
      etmpJson should include("coOfficialsChanged")
      etmpJson should include("declarationChanged")
      etmpJson should include("groupMembersChanged")
      etmpJson should include("partnersChanged")
      etmpJson should include("premisesChanged")
      etmpJson should include("suppliersChanged")

    }
  }

  "For API6 ETMP model" must {
    "create json with changeIndicators " in {
      val API6Json = api6FrontendLTDJson
      val awrsModel = API6Json.as[AWRSFEModel]
      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()
      etmpJson should include("changeIndicators")
      etmpJson should include("llpCorporateBody")

    }
  }

  "For API4 Sole Trader, ETMP model" must {
    "create json without changeIndicators and llpCorporateBody " in {

      val awrsModel = api4FrontendSOPJson.as[AWRSFEModel]

      val etmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter).toString()
      etmpJson should not include "changeIndicators"
      etmpJson should not include "llpCorporateBody"

    }
  }

  "toEtmpNewAWBusiness method of EtmpModelHelper " must {
    "return true for newAWBusiness Flag" in {
      val awrsModel = api4FrontendLTDNewBusinessJson.as[AWRSFEModel]
      val etmpBusinessJson = TestEtmpModelHelper.toEtmpNewAWBusiness(awrsModel.subscriptionTypeFrontEnd).toString()
      etmpBusinessJson should include("\"newAWBusiness\":true")
      etmpBusinessJson should include("\"proposedStartDate\":\"2016-10-30\"")
    }
  }

  "toEtmpCommunicationDetails method of EtmpModelHelper" must {
    "convert '+' to 00" when {
      "given a contact number that starts with +" in {
        val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("businessContacts" -> Json.obj("telephone" -> "+441234567890"))), api4FrontendSOPString)
        val awrsModel: AWRSFEModel = Json.parse(updatedJson).as[AWRSFEModel]

        val json = TestEtmpModelHelper.toEtmpCommunicationDetails(awrsModel.subscriptionTypeFrontEnd).toString
        json should include ("00441234567890")

      }
    }

    "keep the input the same" when {
      "given a contact number that does not start with +" in {
        val apiJson = api4FrontendSOPString
        val awrsModel: AWRSFEModel = Json.parse(apiJson).as[AWRSFEModel]

        val json = TestEtmpModelHelper.toEtmpCommunicationDetails(awrsModel.subscriptionTypeFrontEnd).toString
        json should include ("01234567891")
      }

      "give + appears anywhere else in number" in {
        val updatedJson = updateJson(Json.obj("subscriptionTypeFrontEnd" -> Json.obj("businessContacts" -> Json.obj("telephone" -> "1234567890+22"))), api4FrontendSOPString)
        val awrsModel: AWRSFEModel = Json.parse(updatedJson).as[AWRSFEModel]

        val json = TestEtmpModelHelper.toEtmpCommunicationDetails(awrsModel.subscriptionTypeFrontEnd).toString
        json should include ("1234567890+22")
      }
    }
  }

  "identificationCorpNumbersWithCRNType" must {
    "not pad a CRN and produce json" when {
      "the CRN is 8 digits" in {
        val identification: CorpNumbersWithCRNType = BusinessDirector(
          personOrCompany = "yes",
          directorsAndCompanySecretaries = "foo",
          doYouHaveCRN = Some("yes"),
          companyRegNumber = Some("12345678")
        )

        val json = TestEtmpModelHelper.identificationCorpNumbersWithCRNType(identification)
        (json \ "companyRegNumber").get.as[String] shouldBe "12345678"
      }

      "the CRN is a scottish company" in {
        val identification: CorpNumbersWithCRNType = BusinessDirector(
          personOrCompany = "yes",
          directorsAndCompanySecretaries = "foo",
          doYouHaveCRN = Some("yes"),
          companyRegNumber = Some("SC999999")
        )

        val json = TestEtmpModelHelper.identificationCorpNumbersWithCRNType(identification)
        (json \ "companyRegNumber").get.as[String] shouldBe "SC999999"
      }
    }

    "pad a CRN and produce json" when {
      "the CRN is 7 digits" in {
        val identification: CorpNumbersWithCRNType = BusinessDirector(
          personOrCompany = "yes",
          directorsAndCompanySecretaries = "foo",
          doYouHaveCRN = Some("yes"),
          companyRegNumber = Some("1234567")
        )

        val json = TestEtmpModelHelper.identificationCorpNumbersWithCRNType(identification)
        (json \ "companyRegNumber").get.as[String] shouldBe "01234567"
      }
    }
  }

  "identificationIncorporationDetails" must {
    "not pad a CRN and produce json" when {
      "the CRN is 8 digits" in {
        val identification: IncorporationDetails = GroupMember(
          companyNames = CompanyNames(None, None, None),
          isBusinessIncorporated = Some("Yes"),
          companyRegDetails = Some(CompanyRegDetails(
            companyRegistrationNumber = "12345678",
            dateOfIncorporation = "20/05/1970"
          )),
          address = None,
          doYouHaveVRN = None,
          vrn = None,
          doYouHaveUTR = None,
          utr = None,
          addAnotherGrpMember = None
        )

        val json = TestEtmpModelHelper.identificationIncorporationDetails(identification)
        (json \ "companyRegistrationNumber").get.as[String] shouldBe "12345678"
      }

      "the CRN is a scottish company" in {
        val identification: IncorporationDetails = GroupMember(
          companyNames = CompanyNames(None, None, None),
          isBusinessIncorporated = Some("Yes"),
          companyRegDetails = Some(CompanyRegDetails(
            companyRegistrationNumber = "SC999999",
            dateOfIncorporation = "20/05/1970"
          )),
          address = None,
          doYouHaveVRN = None,
          vrn = None,
          doYouHaveUTR = None,
          utr = None,
          addAnotherGrpMember = None
        )

        val json = TestEtmpModelHelper.identificationIncorporationDetails(identification)
        (json \ "companyRegistrationNumber").get.as[String] shouldBe "SC999999"
      }
    }

    "pad a CRN and produce json" when {
      "the CRN is 7 digits" in {
        val identification: IncorporationDetails = GroupMember(
          companyNames = CompanyNames(None, None, None),
          isBusinessIncorporated = Some("Yes"),
          companyRegDetails = Some(CompanyRegDetails(
            companyRegistrationNumber = "1234567",
            dateOfIncorporation = "20/05/1970"
          )),
          address = None,
          doYouHaveVRN = None,
          vrn = None,
          doYouHaveUTR = None,
          utr = None,
          addAnotherGrpMember = None
        )

        val json = TestEtmpModelHelper.identificationIncorporationDetails(identification)
        (json \ "companyRegistrationNumber").get.as[String] shouldBe "01234567"
      }
    }
  }

}
