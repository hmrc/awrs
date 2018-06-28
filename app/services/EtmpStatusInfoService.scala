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
import models.{StatusInfoResponseType, StatusInfoSuccessResponseType}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EtmpStatusInfoService extends EtmpStatusInfoService {
  override val etmpConnector: EtmpConnector = EtmpConnector
}

trait EtmpStatusInfoService {
  val etmpConnector: EtmpConnector

  private val cdataPattern = """^<!\[[Cc][Dd][Aa][Tt][Aa]\[.*?\]\]>$"""
  private[services] def isInCDATATag(secureCommText: String): Boolean =
    secureCommText.matches(cdataPattern)

  // this function only strips the outter CData tag
  private[services] def stripCData(secureCommText: String): String =
    isInCDATATag(secureCommText) match {
      case true => secureCommText.replaceAll("""^<!\[[Cc][Dd][Aa][Tt][Aa]\[""", "").replaceAll("""\]\]>$""", "")
      case false => secureCommText
    }

  def getStatusInfo(awrsRefNo: String, contactNumber: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    etmpConnector.getStatusInfo(awrsRefNo, contactNumber) map {
      response =>
        response.status match {
          case _ => response
        }
    }
//TODO decode secure com text for success and call this method on controller after a successful response. This should be before stripCData is ran
  def decode(statusInfoResponseType: StatusInfoResponseType): StatusInfoResponseType = {
    statusInfoResponseType match {
      case data: StatusInfoSuccessResponseType ⇒ data.copy(secureCommText = stripCData(data.secureCommText))
      case data ⇒ data
    }
  }
}
