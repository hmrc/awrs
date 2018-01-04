/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}
import utils.Utility._

case class SubscriptionStatusType(processingDate: String,
                                  formBundleStatus: FormBundleStatus,
                                  deRegistrationDate: Option[String],
                                  groupBusinessPartner: Boolean,
                                  businessContactNumber: Option[String],
                                  safeId : Option[String])

object SubscriptionStatusType {

  val reader = new Reads[SubscriptionStatusType] {

    def reads(js: JsValue): JsResult[SubscriptionStatusType] = {

      for {
        processingDate <- (js \ "processingDate").validate[String]
        formBundleStatus <- (js \ "formBundleStatus").validate[FormBundleStatus]
        deRegistrationDate <- (js \ "deRegistrationDate").validateOpt[String]
        groupBusinessPartner <- (js \ "groupBusinessPartner").validate[Boolean]
        businessContactNumber <- (js \ "businessContactNumber").validateOpt[String]
        safeId <- (js \ "safeId").validateOpt[String]
      } yield {
        SubscriptionStatusType(processingDate,
          formBundleStatus = formBundleStatus,
          deRegistrationDate = deRegistrationDate,
          groupBusinessPartner = groupBusinessPartner,
          businessContactNumber = businessContactNumber,
          safeId = safeId
        )
      }
    }
  }
  implicit val formats = Json.format[SubscriptionStatusType]
}

sealed trait FormBundleStatus {
  def code: String

  def name: String

  override def toString: String = f"$name($code)"
}

object FormBundleStatus {

  implicit val reader: Reads[FormBundleStatus] = new Reads[FormBundleStatus] {
    def reads(json: JsValue): JsResult[FormBundleStatus] =
      JsSuccess(json match {
        case JsString(codeorName) => apply(codeorName)
        case _ => apply("-01")
      })
  }

  implicit val writer: Writes[FormBundleStatus] = new Writes[FormBundleStatus] {
    def writes(v: FormBundleStatus): JsValue = JsString(v.code)
  }

  def apply(codeOrName: String): FormBundleStatus = codeOrName.trim().toLowerCase match {
    case NoStatus.code | NoStatus.name => NoStatus
    case Pending.code | Pending.name => Pending
    case Withdrawal.code | Withdrawal.name => Withdrawal
    case Approved.code | Approved.name => Approved
    case ApprovedWithConditions.code | ApprovedWithConditions.name => ApprovedWithConditions
    case Rejected.code | Rejected.name => Rejected
    case RejectedUnderReviewOrAppeal.code | RejectedUnderReviewOrAppeal.name => RejectedUnderReviewOrAppeal
    case Revoked.code | Revoked.name => Revoked
    case RevokedUnderReviewOrAppeal.code | RevokedUnderReviewOrAppeal.name => RevokedUnderReviewOrAppeal
    case DeRegistered.code | DeRegistered.name => DeRegistered
    case _ => NotFound(codeOrName)
  }
}

case object NoStatus extends FormBundleStatus {
  val code = "00"
  val name = "None".toLowerCase
}

case object Pending extends FormBundleStatus {
  val code = "01"
  val name = "Pending".toLowerCase
}

case object Withdrawal extends FormBundleStatus {
  val code = "02"
  val name = "Withdrawal".toLowerCase
}

case object Approved extends FormBundleStatus {
  val code = "04"
  val name = "Approved".toLowerCase
}

case object ApprovedWithConditions extends FormBundleStatus {
  val code = "05"
  val name = "Approved with Conditions".toLowerCase
}

case object Rejected extends FormBundleStatus {
  val code = "06"
  val name = "Rejected".toLowerCase
}

case object RejectedUnderReviewOrAppeal extends FormBundleStatus {
  val code = "07"
  val name = "Rejected under Review/Appeal".toLowerCase
}

case object Revoked extends FormBundleStatus {
  val code = "08"
  val name = "Revoked".toLowerCase
}

case object RevokedUnderReviewOrAppeal extends FormBundleStatus {
  val code = "09"
  val name = "Revoked under Review/Appeal".toLowerCase
}

case object DeRegistered extends FormBundleStatus {
  val code = "10"
  val name = "De-Registered".toLowerCase
}

case class NotFound(code: String) extends FormBundleStatus {
  val name = "Not Found".toLowerCase
}
