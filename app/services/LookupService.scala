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

package services

import connectors.{EtmpConnector, HipConnector}
import play.api.Logging
import play.api.http.Status
import play.api.libs.json._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, Utility}

private object LookupService {
  // DES: response fields
  private val processingDate = "processingDate"
  private val awrsRegistrationNumber = "awrsRegistrationNumber"
  private val businessAddress = "businessAddress"
  private val postcode = "postcode"

  // HIP: response fields
  private val processingDateTime = "processingDateTime"
  private val awrsRegNumber = "awrsRegNumber"
  private val address = "address"
  private val postalCode = "postalCode"
}

class LookupService @Inject()(etmpConnector: EtmpConnector,
                              hipConnector: HipConnector)(implicit ec: ExecutionContext) extends Logging {

  import LookupService._

  def lookupApplication(awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      hipConnector.lookup(awrsRefNo) map {
        case HttpResponse(Status.CREATED, body, headers) =>
          HttpResponse(
            status = Status.OK,
            body = Json.stringify(updateResponseForHip(responseJson = Json.parse(body))),
            headers = headers
          )
        case HttpResponse(failedStatusCode, body, headers) =>
          logger.error(s"[DeRegistrationService][deRegistration] Failure response from HIP endpoint: $failedStatusCode")
          HttpResponse(
            status = failedStatusCode,
            body = body,
            headers = headers
          )
      }
    } else {
      etmpConnector.lookup(awrsRefNo) map {
        response =>
          response.status match {
            case _ => response
          }
      }
    }
  }

  private def updateResponseForHip(responseJson: JsValue): JsValue = {
    def updateAddress(outerKey: String, outerObj: JsObject): JsObject = {
      (outerObj \ outerKey).toOption match {
        case Some(innerValue) =>
          (innerValue \ address).toOption match {
            case Some(addressValue) =>
              val newAddressValue = (addressValue \ postalCode).toOption match {
                case Some(postalCodeValue) =>
                  addressValue.as[JsObject] + (postcode -> postalCodeValue) - postalCode
                case None => throw new RuntimeException(s"Received response is missing the '$postalCode' key in the '$address' node.")
              }
              innerValue.as[JsObject] + (businessAddress -> newAddressValue.as[JsValue]) - address
            case None => throw new RuntimeException(s"Received response is missing the '$address' key in the '$outerKey' node.")
          }
        case None => throw new RuntimeException(s"Received response is missing the '$outerKey' key in the 'success' node.")
      }
    }

    val successJsObject: JsObject = Utility.stripSuccessNode(responseJson)

    val updatedDateTimeJsObj = (successJsObject \ processingDateTime).toOption match {
      case Some(processingDateTimeValue) => (successJsObject + (processingDate -> processingDateTimeValue)) - processingDateTime
      case None => throw new RuntimeException(s"Received response is missing the '$processingDateTime' key in the 'success' node.")
    }

    val updatedAWRSRegJsObj = (updatedDateTimeJsObj \ awrsRegNumber).toOption match {
      case Some(awrsNo) => (updatedDateTimeJsObj + (awrsRegistrationNumber -> awrsNo)) - awrsRegNumber
      case None => throw new RuntimeException(s"Received response is missing the '$awrsRegNumber' key in the 'success' node.")
    }

    val updatedWholesalerAddress = updateAddress("wholesaler", updatedAWRSRegJsObj)

    updateAddress("groupMembers", updatedWholesalerAddress)
  }
}
