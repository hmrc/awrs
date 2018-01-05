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

package services

import connectors.EtmpConnector
import metrics.AwrsMetrics
import models.ApiType
import play.api.libs.json.JsValue

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

object WithdrawalService extends WithdrawalService {
  val etmpConnector: EtmpConnector = EtmpConnector
  override val metrics = AwrsMetrics
}

trait WithdrawalService {
  val etmpConnector: EtmpConnector
  val metrics: AwrsMetrics

  def withdrawal(data: JsValue, awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(ApiType.API8Withdrawal)
    etmpConnector.withdrawal(awrsRefNo, data)
  }
}
