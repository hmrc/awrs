/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json._
import utils.SessionUtils

object StatusOfPerson {
  val listOfStatus: Seq[String] = List("Director",
    "Company Secretary",
    "Director and Company Secretary",
    "Authorised Signatory")
}

object WholesalerType {
  val all = "06"
  val cashAndCarry = "01"
  val offTradeSupplierOnly = "02"
  val onTradeSupplierOnly = "03"
  val producer = "04"
  val broker = "05"
  val other = "99"
}

object AlcoholOrdersType {
  val all = "01"
  val internet = "02"
  val telephoneFax = "03"
  val faceToface = "04"
  val other = "99"
}

object TypeOfCustomers {
  val pubs = "01"
  val nightClubs = "02"
  val privateClubs = "03"
  val hotels = "04"
  val hospitalityCatering = "05"
  val restaurants = "06"
  val indepRetailers = "07"
  val nationalRetailers = "08"
  val public = "09"
  val otherWholesalers = "10"
  val exports = "11"
  val euDispatches = "12"
  val other = "99"
  val all = "13"
}

object ProductsSold {
  val all = "01"
  val beer = "05"
  val wine = "02"
  val spirits = "03"
  val cider = "04"
  val perry = "06"
  val other = "99"
}

object ProprietorType {
  val soleTrader = "soleProprietor"
  val nonProprietor = "nonProprietor"
}

object PartnerDetailType {
  val Sole_Trader = "Sole Trader"
  val Corporate_Body = "Corporate Body"
  val Individual = "Individual"
}

object LegalEntityType {
  val CORPORATE_BODY = "Corporate Body"
  val SOLE_TRADER = "Sole Trader"
  val PARTNERSHIP = "Partnership"
  val LIMITED_LIABILITY_PARTNERSHIP = "Limited Liability Partnership"
}

case class SuccessfulSubscriptionResponse(processingDate: String, awrsRegistrationNumber: String, etmpFormBundleNumber: String)

object SuccessfulSubscriptionResponse {
  implicit val formats: OFormat[SuccessfulSubscriptionResponse] = Json.format[SuccessfulSubscriptionResponse]
}

case class AWRSFEModel(subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd)

object AWRSFEModel extends EtmpModelHelper {

  def isLlpCorpBody(st: SubscriptionTypeFrontEnd): JsObject =
    st.legalEntity.get.legalEntity match {
      case Some("LTD" | "LLP" | "LP" | "LTD_GRP" | "LLP_GRP") =>
        Json.obj("llpCorporateBody" -> toGroupDeclaration(st))
      case _ => Json.obj()
    }

  def isGroupMemberDetails(st: SubscriptionTypeFrontEnd): JsObject =
    st.legalEntity.get.legalEntity match {
      case Some("LTD_GRP" | "LLP_GRP") => Json.obj("groupMemberDetails" -> toEtmpGroupMemberDetails(st))
      case _ => Json.obj()
    }

  implicit val etmpWriter: Writes[AWRSFEModel] = new Writes[AWRSFEModel] {

    def writes(feModel: AWRSFEModel): JsValue = {

      val subscriptionTypeFE = feModel.subscriptionTypeFrontEnd

      val changeIndicators =
        subscriptionTypeFE.changeIndicators match {
          case Some(indicators) => Json.obj("changeIndicators" -> toEtmpChangeIndicators(subscriptionTypeFE))
          case _ => Json.obj()
        }

      val subscriptionType =
        Json.obj(
          "legalEntity" -> toEtmpLegalEntity(subscriptionTypeFE))
          .++(toEtmpNewAWBusiness(subscriptionTypeFE))
          .++(isLlpCorpBody(subscriptionTypeFE))
          .++(Json.obj("businessDetails" -> toEtmpBusinessDetails(subscriptionTypeFE)))
          .++(isGroupMemberDetails(subscriptionTypeFE))
          .++(Json.obj(
            "businessAddressForAwrs" -> toEtmpBusinessAddressForAwrs(subscriptionTypeFE),
            "contactDetails" -> toEtmpContactDetails(subscriptionTypeFE),
            "additionalBusinessInfo" -> toEtmpAdditionalBusinessInfo(subscriptionTypeFE),
            "declaration" -> toEtmpDeclaration(subscriptionTypeFE)))

      Json.obj(
        "acknowledgmentReference" -> SessionUtils.getUniqueAckNo)
        .++(changeIndicators)
        .++(Json.obj("subscriptionType" -> subscriptionType))
    }

  }

  val etmpReader: Reads[AWRSFEModel] = new Reads[AWRSFEModel] {

    def reads(js: JsValue): JsResult[AWRSFEModel] =
      for {
        subscriptionTypeFrontEnd <- js.validate[SubscriptionTypeFrontEnd](SubscriptionTypeFrontEnd.reader)
      } yield {
        AWRSFEModel(subscriptionTypeFrontEnd = subscriptionTypeFrontEnd)
      }

  }

  implicit val formats: OFormat[AWRSFEModel] = Json.format[AWRSFEModel]

}
