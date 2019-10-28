/*
 * Copyright 2019 HM Revenue & Customs
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
import javax.inject.Inject
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import utils.AWRSFeatureSwitches

import scala.concurrent.{ExecutionContext, Future}

class EtmpRegimeService @Inject()(etmpConnector: EtmpConnector) {

  def getRegimeRefNumber(safeId: String, regime: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    etmpConnector.awrsRegime(safeId, regime).map { response =>
      (response.json \ "regimeIdentifiers").asOpt[List[JsValue]].flatMap { regimeIdentifiers =>
        regimeIdentifiers.headOption.flatMap { regimeJs =>
          (regimeJs \ "regimeRefNumber").asOpt[String]
        }
      }
    }
  }

  def checkETMPApi(safeId: String, regime: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[String]] = {
    if (AWRSFeatureSwitches.regimeCheck().enabled) {
      getRegimeRefNumber(safeId, regime) recover {
        case _ => None
      }
    } else {
      Future.successful(None)
    }
  }
}
