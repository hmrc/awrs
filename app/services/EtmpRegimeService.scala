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

package services

import connectors.{EnrolmentStoreConnector, EtmpConnector}
import javax.inject.Inject
import models.{BusinessCustomerDetails, BusinessRegistrationDetails, EnrolmentVerifiers, EtmpRegistrationDetails}
import play.api.Logger
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions, User}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AWRSFeatureSwitches
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EtmpRegimeService @Inject()(etmpConnector: EtmpConnector,
                                  val enrolmentStoreConnector: EnrolmentStoreConnector,
                                  val authConnector: AuthConnector) extends AuthorisedFunctions {

  private val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"

  def getEtmpBusinessDetails(safeId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EtmpRegistrationDetails]] = {
    etmpConnector.awrsRegime(safeId).map { response =>
      Try(EtmpRegistrationDetails.etmpReader.reads(response.json)) match {
        case Success(value)   => value.asOpt
        case Failure(e)       =>
          Logger.info(s"[EtmpRegimeService][getEtmpBusinessDetails] Could not read ETMP response - $e")
          None
      }
    }
  }

  def handleDuplicateSubscription(etmpRegistrationDetails: EtmpRegistrationDetails,
                                  businessCustomerDetails: BusinessCustomerDetails)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EtmpRegistrationDetails]] = {
    authorised(User).retrieve(Retrievals.affinityGroup){ affGroup =>
      Future(getEtmpRegistrationDetails(affGroup, businessCustomerDetails, etmpRegistrationDetails))
    } recover {
      case _ =>
        None
    }
  }

  private def createVerifiers(safeId: String, utr: Option[String], businessType: String, postcode: String) = {
    val utrTuple = businessType match {
      case "SOP" => "SAUTR" -> utr.getOrElse("")
      case _ => "CTUTR" -> utr.getOrElse("")
    }
    val verifierTuples = Seq(
      "Postcode" -> postcode,
      "SAFEID" -> safeId
    ) :+ utrTuple

    EnrolmentVerifiers(verifierTuples: _*)
  }

  def upsertEacdEnrolment(safeId: String,
                          utr: Option[String],
                          businessType: String,
                          postcode: String,
                          awrsRefNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~$awrsRefNumber"
    val enrolmentVerifiers = createVerifiers(safeId, utr, businessType, postcode)
    enrolmentStoreConnector.upsertEnrolment(enrolmentKey, enrolmentVerifiers)
  }

  def checkETMPApi(safeId: String, businessCustomerDetails: BusinessCustomerDetails, businessRegistrationDetails: BusinessRegistrationDetails)
                  (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[EtmpRegistrationDetails]] = {
    val businessType = businessRegistrationDetails.legalEntity.getOrElse("")
    val postcode = businessCustomerDetails.businessAddress.postcode.getOrElse("").replaceAll("\\s+", "")

    if (!AWRSFeatureSwitches.regimeCheck().enabled) {
      Future.successful(None)
    } else {
      getEtmpBusinessDetails(safeId) flatMap {
        case Some(etmpRegDetails) =>
          handleDuplicateSubscription(etmpRegDetails, businessCustomerDetails) flatMap {
            case Some(_) =>
              upsertEacdEnrolment(
                safeId,
                businessRegistrationDetails.utr,
                businessType,
                postcode,
                etmpRegDetails.regimeRefNumber
              ) map { response =>
                response.status match {
                  case NO_CONTENT => Some(etmpRegDetails)
                  case status =>
                    Logger.warn(s"[EtmpRegimeService][checkETMPApi] Failed to upsert to EACD - status: $status")
                    None
                }
              }
            case None =>
              Future.successful(None)
          }
      } recover {
        case e: Exception =>
          Logger.warn(s"[EtmpRegimeService][checkETMPApi] Failed to check ETMP api :${e.getMessage}")
          None
      }
    }
  }

  def compareOptionalStrings(bcdValue: Option[String], erdValue: Option[String]): Boolean = {
    if(bcdValue.isEmpty && erdValue.isEmpty) {
      true
    } else {
      bcdValue.map(_.toUpperCase).contains(erdValue.map(_.toUpperCase).getOrElse(""))
    }
  }

  private[services] def matchIndividual(bcd: BusinessCustomerDetails, erd: EtmpRegistrationDetails): Boolean = {
    Map(
      "sapNum"    -> (bcd.sapNumber.toUpperCase == erd.sapNumber.toUpperCase),
      "safeId"    -> (bcd.safeId.toUpperCase == erd.safeId.toUpperCase),
      "firstName" -> compareOptionalStrings(bcd.firstName, erd.firstName),
      "lastName"  -> compareOptionalStrings(bcd.lastName, erd.lastName)
    ).partition{case (_,v) => v} match {
      case (_, failures) if failures.isEmpty => true
      case (_, failures) =>
        Logger.warn(s"[matchIndividual] Could not match following details for individual: $failures")
        false
    }

  }

  private[services] def matchOrg(bcd: BusinessCustomerDetails, erd: EtmpRegistrationDetails): Boolean = {
    Map(
      "businessName" -> erd.organisationName.map(_.toUpperCase).contains(bcd.businessName.toUpperCase),
      "sapNumber" -> (bcd.sapNumber.toUpperCase == erd.sapNumber.toUpperCase),
      "safeId" -> (bcd.safeId.toUpperCase == erd.safeId.toUpperCase),
      "agentRef" -> compareOptionalStrings(bcd.agentReferenceNumber, erd.agentReferenceNumber)
    ).partition{case (_, v) => v} match {
      case (_, failures) if failures.isEmpty => true
      case (_, failures) =>
        Logger.warn(s"[matchOrg] Could not match following details for organisation: $failures")
        false
    }
  }

  private[services] def getEtmpRegistrationDetails(affinityGroup: Option[AffinityGroup],
                                         bcd: BusinessCustomerDetails,
                                         erd: EtmpRegistrationDetails): Option[EtmpRegistrationDetails] = {
    affinityGroup match {
      case Some(AffinityGroup.Individual)   if matchIndividual(bcd, erd) => Some(erd)
      case Some(AffinityGroup.Organisation) if matchOrg(bcd, erd)        => Some(erd)
      case _ => None
    }
  }
}
