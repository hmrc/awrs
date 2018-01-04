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

import org.joda.time.LocalDate
import play.api.libs.json._
import utils._

case class WithdrawalRequest(reason: Option[String], reasonOther: Option[String])

  object WithdrawalRequest {

    implicit val writer = new Writes[WithdrawalRequest] {

      def writes(feModel: WithdrawalRequest): JsValue = {
        val returnJson =
          Json.obj()
            .++(Json.obj("acknowledgmentReference" -> SessionUtils.getUniqueAckNo))
            .++(Json.obj("withdrawalDate" -> LocalDate.now().toString("yyyy-MM-dd")))
            .++(feModel.reason.fold(Json.obj())(x => Json.obj("withdrawalReason" -> feModel.reason)))
            .++(feModel.reasonOther.fold(Json.obj())(x => Json.obj("withdrawalReasonOthers" -> feModel.reasonOther)))

        returnJson
      }
    }
    implicit val reader = Json.reads[WithdrawalRequest]
  }
