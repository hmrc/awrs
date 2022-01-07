/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.{Format, Json, _}
import utils.Bool._
import utils.EtmpConstants
import utils.Utility._
import scala.language.reflectiveCalls
import scala.language.implicitConversions

import scala.util.{Failure, Success, Try}

trait ModelVersionControl {
  def modelVersion: String
}

sealed trait CorpNumbersType {
  def doYouHaveVRN: Option[String]

  def vrn: Option[String]

  def doYouHaveUTR: Option[String]

  def utr: Option[String]
}

sealed trait IndividualIdNumbersType extends CorpNumbersType {
  def doYouHaveNino: Option[String]

  def nino: Option[String]
}

sealed trait CorpNumbersWithCRNType extends CorpNumbersType {
  def doYouHaveCRN: Option[String]

  def companyRegNumber: Option[String]
}

sealed trait IncorporationDetails {
  def isBusinessIncorporated: Option[String]

  def companyRegDetails: Option[CompanyRegDetails]
}

case class BusinessDirector(personOrCompany: String,
                            firstName: Option[String] = None,
                            lastName: Option[String] = None,
                            doTheyHaveNationalInsurance: Option[String] = None,
                            nino: Option[String] = None,
                            passportNumber: Option[String] = None,
                            nationalID: Option[String] = None,
                            companyNames: Option[CompanyNames] = None,
                            doYouHaveUTR: Option[String] = None,
                            utr: Option[String] = None,
                            doYouHaveCRN: Option[String] = None,
                            companyRegNumber: Option[String] = None,
                            doYouHaveVRN: Option[String] = None,
                            vrn: Option[String] = None,
                            directorsAndCompanySecretaries: String,
                            otherDirectors: Option[String] = None
                           ) extends CorpNumbersWithCRNType

case class BusinessDirectors(directors: List[BusinessDirector],
                             modelVersion: String = BusinessDirectors.latestModelVersion
                            ) extends ModelVersionControl

case class Supplier(alcoholSuppliers: Option[String],
                    supplierName: Option[String],
                    vatRegistered: Option[String],
                    vatNumber: Option[String],
                    supplierAddress: Option[Address],
                    additionalSupplier: Option[String],
                    ukSupplier: Option[String])

case class Suppliers(suppliers: List[Supplier])

case class AdditionalBusinessPremises(additionalPremises: Option[String],
                                      additionalAddress: Option[Address],
                                      addAnother: Option[String])


case class AdditionalBusinessPremisesList(premises: List[AdditionalBusinessPremises])

case class Partner(entityType: Option[String],
                   firstName: Option[String],
                   lastName: Option[String],
                   companyNames: Option[CompanyNames] = None,
                   partnerAddress: Option[Address],
                   doYouHaveNino: Option[String],
                   nino: Option[String],
                   doYouHaveUTR: Option[String],
                   utr: Option[String],
                   isBusinessIncorporated: Option[String],
                   companyRegDetails: Option[CompanyRegDetails],
                   doYouHaveVRN: Option[String],
                   vrn: Option[String],
                   otherPartners: Option[String]) extends CorpNumbersType with IndividualIdNumbersType with IncorporationDetails

case class CompanyRegDetails(companyRegistrationNumber: String, dateOfIncorporation: String)

case class BusinessType(legalEntity: Option[String])

case class EtmpAddress(address: Address)

case class Address(
                    postcode: Option[String] = None,
                    addressLine1: String,
                    addressLine2: String,
                    addressLine3: Option[String] = None,
                    addressLine4: Option[String] = None,
                    addressCountryCode: Option[String] = None
                  ) {

  override def toString = {
    val line3display = addressLine3.fold("")(x => s"$x, ")
    val line4display = addressLine4.fold("")(x => s"$x, ")
    val postcodeDisplay = postcode.fold("")(x => s"$x, ")
    s"$addressLine1, $addressLine2, $line3display$line4display$postcodeDisplay"
  }

}

case class ApplicationDeclaration(declarationName: Option[String], declarationRole: Option[String])

case class BCAddress(
                      line_1: String,
                      line_2: String,
                      line_3: Option[String] = None,
                      line_4: Option[String] = None,
                      postcode: Option[String] = None,
                      country: String) {

  override def toString = {
    val line3display = line_3.fold("")(x => s"$x, ")
    val line4display = line_4.fold("")(x => s"$x, ")
    val postcodeDisplay = postcode.fold("")(x => s"$x, ")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }

}

case class ChangeIndicators(businessDetailsChanged: Boolean,
                            businessAddressChanged: Boolean,
                            contactDetailsChanged: Boolean,
                            additionalBusinessInfoChanged: Boolean,
                            partnersChanged: Boolean,
                            coOfficialsChanged: Boolean,
                            premisesChanged: Boolean,
                            suppliersChanged: Boolean,
                            groupMembersChanged: Boolean,
                            declarationChanged: Boolean)

case class NewAWBusiness(newAWBusiness: String, proposedStartDate: Option[String])

case class GroupDeclaration(groupRepConfirmation: Boolean)

case class GroupMembers(members: List[GroupMember],
                        modelVersion: String = GroupMembers.latestModelVersion
                       ) extends ModelVersionControl

case class CompanyNames(businessName: Option[String],
                        doYouHaveTradingName: Option[String],
                        tradingName: Option[String])

case class GroupMember(companyNames: CompanyNames,
                       isBusinessIncorporated: Option[String],
                       companyRegDetails: Option[CompanyRegDetails],
                       address: Option[Address],
                       doYouHaveVRN: Option[String],
                       vrn: Option[String],
                       doYouHaveUTR: Option[String],
                       utr: Option[String],
                       addAnotherGrpMember: Option[String]
                      ) extends CorpNumbersType with IncorporationDetails

object BCAddress {

  val reader = new Reads[BCAddress] {

    def reads(js: JsValue): JsResult[BCAddress] =
      for {
        line_1 <- (js \ "address" \ "addressLine1").validate[String]
        line_2 <- (js \ "address" \ "addressLine2").validate[String]
        line_3 <- JsSuccess((js \ "address" \ "addressLine3").asOpt[String])
        line_4 <- JsSuccess((js \ "address" \ "addressLine4").asOpt[String])
        postcode <- JsSuccess((js \ "address" \ "postalCode").asOpt[String])
        country <- (js \ "address" \ "countryCode").validate[String]
      } yield {
        BCAddress(line_1 = line_1, line_2 = line_2, line_3 = line_3, line_4 = line_4, postcode = postcode, country = country)
      }

  }

  implicit val formats = Json.format[BCAddress]

}

case class BusinessCustomerDetails(businessName: String,
                                   businessType: Option[String],
                                   businessAddress: BCAddress,
                                   sapNumber: String,
                                   safeId: String,
                                   isAGroup: Boolean,
                                   regimeRefNumber: Option[String],
                                   agentReferenceNumber: Option[String],
                                   firstName: Option[String] = None,
                                   lastName: Option[String] = None,
                                   utr: Option[String] = None)

case class EtmpRegistrationDetails(
                                    organisationName: Option[String],
                                    sapNumber: String,
                                    safeId: String,
                                    isAGroup: Option[Boolean],
                                    regimeRefNumber: String,
                                    agentReferenceNumber: Option[String],
                                    firstName: Option[String] = None,
                                    lastName: Option[String] = None)

object EtmpRegistrationDetails {
  val etmpReader: Reads[EtmpRegistrationDetails] = new Reads[EtmpRegistrationDetails] {
    def reads(js: JsValue): JsResult[EtmpRegistrationDetails] = {
      val regimeRefNumber: String = (js \ "regimeIdentifiers").asOpt[List[JsValue]].flatMap { regimeIdentifiers =>
        regimeIdentifiers.headOption.flatMap { regimeJs =>
          (regimeJs \ "regimeRefNumber").asOpt[String]
        }
      }.getOrElse(throw new RuntimeException("[EtmpRegistrationDetails][etmpReader][reads] No regime ref number"))

      val organisationName = (js \ "organisation" \ "organisationName").asOpt[String]
      val sapNumber = (js \ "sapNumber").as[String]
      val safeId = (js \ "safeId").as[String]
      val isAGroup = (js \ "organisation" \ "isAGroup").asOpt[Boolean]
      val agentReferenceNumber = (js \ "agentReferenceNumber").asOpt[String]
      val firstName = (js \"individual" \ "firstName").asOpt[String]
      val lastName = (js \ "individual" \ "lastName").asOpt[String]

      JsSuccess(EtmpRegistrationDetails(
        organisationName,
        sapNumber,
        safeId,
        isAGroup,
        regimeRefNumber,
        agentReferenceNumber,
        firstName,
        lastName
      ))
    }
  }
}

object BusinessCustomerDetails {



  val reader = new Reads[BusinessCustomerDetails] {

    def reads(js: JsValue): JsResult[BusinessCustomerDetails] =
      JsSuccess(BusinessCustomerDetails(businessName = "", businessType = None, businessAddress = BCAddress("", "", None, None, None, ""),
        sapNumber = "", safeId = "", isAGroup = false, regimeRefNumber = None, agentReferenceNumber = None, firstName = None, lastName = None))

  }

  implicit val formats = Json.format[BusinessCustomerDetails]

}

object EtmpAddress {

  val reader = new Reads[EtmpAddress] {

    def reads(js: JsValue): JsResult[EtmpAddress] =
      for {
        address <- (js \ "address").validate[Address](Address.reader)
      } yield {
        EtmpAddress(address = address)
      }

  }

  implicit val formats = Json.format[Address]

}

object Address {

  val reader = new Reads[Address] {

    def reads(js: JsValue): JsResult[Address] =
      for {
        addressLine1 <- (js \ "addressLine1").validate[String]
        addressLine2 <- (js \ "addressLine2").validate[String]
        addressLine3 <- JsSuccess((js \ "addressLine3").asOpt[String])
        addressLine4 <- JsSuccess((js \ "addressLine4").asOpt[String])
        postcode <- JsSuccess((js \ "postalCode").asOpt[String])
        countryCode <- JsSuccess((js \ "countryCode").asOpt[String])
      } yield {
        Address(postcode = postcode, addressLine1 = addressLine1, addressLine2 = addressLine2, addressLine3 = addressLine3,
          addressLine4 = addressLine4, addressCountryCode = if (countryCode.fold("")(x => x).equals("GB")) None else countryCode)
      }

  }

  implicit val formats = Json.format[Address]

}

object NewAWBusiness {

  val reader = new Reads[NewAWBusiness] {

    def reads(js: JsValue): JsResult[NewAWBusiness] =
      for {
        newAWBusiness <- JsSuccess((js \ "newAWBusiness").asOpt[Boolean])
        proposedStartDate <- JsSuccess((js \ "proposedStartDate").asOpt[String])
      } yield {
        // the check for "" is speced by AWRS-1413. This is due to an issue with ETMP sometimes sending the
        // data as "proposedStartDate": ""
        val parsedProposedStartDate = proposedStartDate match {
          case Some("") => None
          case _ => proposedStartDate
        }
        NewAWBusiness(newAWBusiness = booleanToString(newAWBusiness.fold(false)(x => x)).get, proposedStartDate = etmpToAwrsDateFormatterOrNone(parsedProposedStartDate))
      }

  }

  implicit val formats = Json.format[NewAWBusiness]

}

object CompanyRegDetails {

  val reader = new Reads[CompanyRegDetails] {

    def reads(js: JsValue): JsResult[CompanyRegDetails] =
      for {
        companyRegistrationNumber <- (js \ "companyRegistrationNumber").validate[String]
        dateOfIncorporation <- (js \ "dateOfIncorporation").validate[String]
      } yield {
        CompanyRegDetails(companyRegistrationNumber = companyRegistrationNumber, dateOfIncorporation = etmpToAwrsDateFormatter(dateOfIncorporation))
      }

  }

  implicit val formats = Json.format[CompanyRegDetails]
}

object BusinessRegistrationDetails {

  val reader = (legalEntity: Option[String]) => new Reads[BusinessRegistrationDetails] {

    def toBusinessRegistrationDetails(legalEntity: Option[String],
                                      doYouHaveNino: Option[String],
                                      nino: Option[String],
                                      isBusinessIncorporated: Option[String],
                                      companyRegDetails: Option[CompanyRegDetails],
                                      doYouHaveVRN: Option[String],
                                      vrn: Option[String],
                                      doYouHaveUTR: Option[String],
                                      utr: Option[String]) = {
      BusinessRegistrationDetails(legalEntity = legalEntity,
        doYouHaveNino = doYouHaveNino,
        nino = nino,
        isBusinessIncorporated = isBusinessIncorporated,
        companyRegDetails = companyRegDetails,
        doYouHaveVRN = doYouHaveVRN,
        vrn = vrn,
        doYouHaveUTR = doYouHaveUTR,
        utr = utr)
    }

    def reads(js: JsValue): JsResult[BusinessRegistrationDetails] = {
      val jsSubscriptionType = js \ "subscriptionType"
      val etmpLegalEntity = JsSuccess((jsSubscriptionType \ "legalEntity").asOpt[String])

      lazy val identification: JsResult[JsValue] =
        jsSubscriptionType \ "businessDetails" \ "soleProprietor" match {
          case JsUndefined() => (jsSubscriptionType \ "businessDetails" \ "nonProprietor" \ "identification").validate[JsValue]
          case _ => (jsSubscriptionType \ "businessDetails" \ "soleProprietor" \ "identification").validate[JsValue]
        }

      for {
        doYouHaveNino <- JsSuccess((identification.get \ "doYouHaveNino").asOpt[Boolean])
        nino <- JsSuccess((identification.get \ "nino").asOpt[String])
        isBusinessIncorporated <- JsSuccess((jsSubscriptionType \ "businessDetails" \ "llpCorporateBody" \ "incorporationDetails" \ "isBusinessIncorporated").asOpt[Boolean])
        companyRegDetails <- JsSuccess((jsSubscriptionType \ "businessDetails" \ "llpCorporateBody" \ "incorporationDetails").asOpt[CompanyRegDetails](CompanyRegDetails.reader))
        doYouHaveVRN <- JsSuccess((identification.get \ "doYouHaveVRN").asOpt[Boolean])
        vrn <- JsSuccess((identification.get \ "vrn").asOpt[String])
        doYouHaveUTR <- JsSuccess((identification.get \ "doYouHaveUTR").asOpt[Boolean])
        utr <- JsSuccess((identification.get \ "utr").asOpt[String])
      } yield {
        val doYouHaveNinoString = booleanToString(doYouHaveNino.fold(false)(x => x))
        val isBusinessIncorporatedString = booleanToString(isBusinessIncorporated.fold(false)(x => x))
        val doYouHaveVRNString = booleanToString(doYouHaveVRN.fold(false)(x => x))
        val doYouHaveUTRString = booleanToString(doYouHaveUTR.fold(false)(x => x))
        etmpLegalEntity.get.fold("")(x => x) match {
          case (LegalEntityType.SOLE_TRADER) => toBusinessRegistrationDetails(legalEntity, doYouHaveNinoString, nino, None, None, doYouHaveVRNString, vrn, doYouHaveUTRString, utr)
          case (LegalEntityType.CORPORATE_BODY) => toBusinessRegistrationDetails(legalEntity, None, None, isBusinessIncorporatedString, companyRegDetails, doYouHaveVRNString, vrn, doYouHaveUTRString, utr)
          case (LegalEntityType.PARTNERSHIP) => toBusinessRegistrationDetails(legalEntity, None, None, None, None, doYouHaveVRNString, vrn, doYouHaveUTRString, utr)
          case (LegalEntityType.LIMITED_LIABILITY_PARTNERSHIP) => toBusinessRegistrationDetails(legalEntity, None, None, isBusinessIncorporatedString, companyRegDetails, doYouHaveVRNString, vrn, doYouHaveUTRString, utr)
        }
      }
    }

  }

  implicit val formats = Json.format[BusinessRegistrationDetails]

}

object CompanyNames {

  val reader = new Reads[CompanyNames] {

    def reads(js: JsValue): JsResult[CompanyNames] =
      for {
        companyName <- JsSuccess((js \ "companyName").asOpt[String])
        tradingName <- JsSuccess((js \ "tradingName").asOpt[String])
      } yield {
        CompanyNames(
          businessName = companyName,
          doYouHaveTradingName = tradingName match {
            case Some(_) => Some("Yes")
            case None => Some("No")
          },
          tradingName = tradingName
        )
      }

  }

  implicit val formats = Json.format[CompanyNames]

  implicit class CompanyNamesGetUtil(companyNames: Option[CompanyNames]) {
    def businessName: Option[String] = companyNames.fold(None: Option[String])(_.businessName)

    def tradingName: Option[String] = companyNames.fold(None: Option[String])(_.tradingName)
  }

}


/*
 *  This object is used as a utility factory for CompanyNames,
 *  this object is required since the Json.format[T]) in play 2.3 only works when a single def apply exists in
 *  the companion object.
*/
object CompanyNamesFact {

  /*
  *  only specify an entityType if it can be a sole trader, since in that case doYouHaveTradingName needs to be populated
  */
  def apply(businessName: Option[String], tradingName: Option[String], entityType: Option[String] = None): Option[CompanyNames] =
  (businessName, tradingName) match {
    case (None, None) =>
      entityType match {
        // for sole traders only trading name is checked and it is an optional field
        // so return the inference of no based on its sole existence
        case Some(PartnerDetailType.Sole_Trader) =>
          Some(
            CompanyNames(
              businessName = None,
              doYouHaveTradingName = Some("No"),
              tradingName = None
            )
          )
        case _ => None
      }
    case _ => Some(
      CompanyNames(
        businessName = businessName,
        doYouHaveTradingName = Some(tradingName match {
          case Some(_) => "Yes"
          case None => "No"
        }),
        tradingName = tradingName
      )
    )
  }
}

object GroupMember {

  val reader = new Reads[GroupMember] {

    def reads(js: JsValue): JsResult[GroupMember] =
      for {
        names <- (js \ "names").validate[CompanyNames](CompanyNames.reader)
        isBusinessIncorporated <- JsSuccess((js \ "incorporationDetails" \ "isBusinessIncorporated").asOpt[Boolean])
        companyRegDetails <- JsSuccess((js \ "incorporationDetails").asOpt[CompanyRegDetails](CompanyRegDetails.reader))
        groupJoiningDate <- (js \ "groupJoiningDate").validate[String]
        address <- JsSuccess((js \ "address").asOpt[Address](Address.reader))
        doYouHaveVRN <- JsSuccess((js \ "identification" \ "doYouHaveVRN").asOpt[Boolean])
        vrn <- JsSuccess((js \ "identification" \ "vrn").asOpt[String])
        doYouHaveUTR <- JsSuccess((js \ "identification" \ "doYouHaveUTR").asOpt[Boolean])
        utr <- JsSuccess((js \ "identification" \ "utr").asOpt[String])
      } yield {
        GroupMember(
          companyNames = names,
          isBusinessIncorporated = booleanToString(isBusinessIncorporated.fold(false)(x => x)),
          companyRegDetails = companyRegDetails,
          address = address,
          doYouHaveVRN = booleanToString(doYouHaveVRN.fold(false)(x => x)),
          vrn = vrn,
          doYouHaveUTR = booleanToString(doYouHaveUTR.fold(false)(x => x)),
          utr = utr,
          //Stubbed, implementation provided at the level above"
          addAnotherGrpMember = Some("Yes")
        )
      }

  }

  implicit val formats = Json.format[GroupMember]

}

object GroupMembers {
  val latestModelVersion = "1.0"
  val reader = new Reads[GroupMembers] {

    def reads(js: JsValue): JsResult[GroupMembers] =
      for {
        groupMembers <- (js \ "subscriptionType" \ "groupMemberDetails" \ "groupMember").validate[List[GroupMember]](Reads.list(GroupMember.reader))
      } yield {
        GroupMembers(members = addAnotherGroupMember(groupMembers))
      }

  }

  def addAnotherGroupMember(members: List[GroupMember]): List[GroupMember] =
    members.zipWithIndex.map {
      case (x, i) if i == (members.size - 1) => x.copy(addAnotherGrpMember = Some("No"))
      case (x, i) => x
    }

  implicit val formats = Json.format[GroupMembers]

}

object GroupDeclaration {

  val reader = new Reads[GroupDeclaration] {

    def reads(js: JsValue): JsResult[GroupDeclaration] =
      for {
        groupRepConfirmation <- (js \ "subscriptionType" \ "llpCorporateBody" \ "groupRepConfirmation").validate[Boolean]
      } yield {
        GroupDeclaration(groupRepConfirmation = groupRepConfirmation)
      }

  }

  implicit val formats = Json.format[GroupDeclaration]

}

object AdditionalBusinessPremises {

  val reader = new Reads[AdditionalBusinessPremises] {

    def reads(js: JsValue): JsResult[AdditionalBusinessPremises] =
      for {
        additionalAddress <- JsSuccess((js \ "address").asOpt[Address](Address.reader))
      } yield {
        AdditionalBusinessPremises(
          additionalPremises = Some("Yes"),
          additionalAddress = additionalAddress,
          addAnother = Some("Yes")
        )
      }

  }

  implicit val formats = Json.format[AdditionalBusinessPremises]

}

object AdditionalBusinessPremisesList {

  val reader = new Reads[AdditionalBusinessPremisesList] {

    def reads(js: JsValue): JsResult[AdditionalBusinessPremisesList] =
      for {
        premises <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "premiseAddress").
          validate[List[AdditionalBusinessPremises]](Reads.list(AdditionalBusinessPremises.reader))
      } yield {
        AdditionalBusinessPremisesList(premises = isAdditionalPremises(premises))
      }

  }

  def isAdditionalPremises(premises: List[AdditionalBusinessPremises]): List[AdditionalBusinessPremises] =
    premises match {
      case h :: xt if premises.size == 1 => List(h, AdditionalBusinessPremises(additionalPremises = Some("No"), None, None))
      case h :: t =>
        premises.zipWithIndex.map {
          case (x, i) if i == (premises.size - 1) => x.copy(addAnother = Some("No"))
          case (x, i) => x
        }
    }

  implicit val formats = Json.format[AdditionalBusinessPremisesList]

}

object Partner {

  val reader = new Reads[Partner] {

    def toPartnerDetail(entityType: Option[String],
                        partnerAddress: Option[Address],
                        firstName: Option[String],
                        lastName: Option[String],
                        doYouHaveNino: Option[String],
                        nino: Option[String],
                        companyName: Option[String],
                        tradingName: Option[String],
                        isBusinessIncorporated: Option[String],
                        companyRegDetails: Option[CompanyRegDetails],
                        doYouHaveVRN: Option[String],
                        vrn: Option[String],
                        doYouHaveUTR: Option[String],
                        utr: Option[String],
                        otherPartners: Option[String]) =
      Partner(
        entityType = entityType,
        partnerAddress = partnerAddress,
        firstName = firstName,
        lastName = lastName,
        doYouHaveNino = doYouHaveNino,
        nino = nino,
        companyNames = CompanyNamesFact(companyName, tradingName, entityType),
        isBusinessIncorporated = isBusinessIncorporated,
        companyRegDetails = companyRegDetails,
        doYouHaveVRN = doYouHaveVRN,
        vrn = vrn,
        doYouHaveUTR = doYouHaveUTR,
        utr = utr,
        otherPartners = otherPartners
      )

    def reads(js: JsValue): JsResult[Partner] =
      for {
        entityType <- JsSuccess((js \ "entityType").asOpt[String])
        partnerAddress <- JsSuccess((js \ "partnerAddress").asOpt[Address](Address.reader))
        firstName <- JsSuccess((js \ "individual" \ "name" \ "firstName").asOpt[String])
        lastName <- JsSuccess((js \ "individual" \ "name" \ "lastName").asOpt[String])
        doYouHaveNino <- JsSuccess((js \ "individual" \ "doYouHaveNino").asOpt[Boolean])
        nino <- JsSuccess((js \ "individual" \ "nino").asOpt[String])
        companyName <- JsSuccess((js \ "names" \ "companyName").asOpt[String])
        tradingName <- JsSuccess((js \ "names" \ "tradingName").asOpt[String])
        doYouHaveVRN <- JsSuccess((js \ "identification" \ "doYouHaveVRN").asOpt[Boolean])
        vrn <- JsSuccess((js \ "identification" \ "vrn").asOpt[String])
        doYouHaveUTR <- JsSuccess((js \ "identification" \ "doYouHaveUTR").asOpt[Boolean])
        utr <- JsSuccess((js \ "identification" \ "utr").asOpt[String])
        isBusinessIncorporated <- JsSuccess((js \ "incorporationDetails" \ "isBusinessIncorporated").asOpt[Boolean])
        companyRegDetails <- JsSuccess((js \ "incorporationDetails").asOpt[CompanyRegDetails](CompanyRegDetails.reader))
        dateOfIncorporation <- JsSuccess((js \ "incorporationDetails" \ "dateOfIncorporation").asOpt[String])
        solTradingName <- JsSuccess((js \ "soleProprietor" \ "tradingName").asOpt[String])
        solFirstName <- JsSuccess((js \ "soleProprietor" \ "name" \ "firstName").asOpt[String])
        solLastName <- JsSuccess((js \ "soleProprietor" \ "name" \ "lastName").asOpt[String])
        solDoYouHaveNino <- JsSuccess((js \ "soleProprietor" \ "doYouHaveNino").asOpt[Boolean])
        solNino <- JsSuccess((js \ "soleProprietor" \ "nino").asOpt[String])
        solDoYouHaveVRN <- JsSuccess((js \ "soleProprietor" \ "identification" \ "doYouHaveVRN").asOpt[Boolean])
        solVrn <- JsSuccess((js \ "soleProprietor" \ "identification" \ "vrn").asOpt[String])
        soldoYouHaveUTR <- JsSuccess((js \ "soleProprietor" \ "identification" \ "doYouHaveUTR").asOpt[Boolean])
        solUtr <- JsSuccess((js \ "soleProprietor" \ "identification" \ "utr").asOpt[String])
      } yield {
        entityType.fold("")(x => x) match {
          case PartnerDetailType.Individual => toPartnerDetail(entityType, partnerAddress, firstName, lastName,
            booleanToString(doYouHaveNino.fold(false)(x => x)), nino, None, None, None, None, None, None, None, None, Some("Yes"))
          case PartnerDetailType.Sole_Trader => toPartnerDetail(entityType, partnerAddress, solFirstName, solLastName,
            booleanToString(solDoYouHaveNino.fold(false)(x => x)), solNino, None, solTradingName, None, None,
            booleanToString(solDoYouHaveVRN.fold(false)(x => x)), solVrn, booleanToString(soldoYouHaveUTR.fold(false)(x => x)), solUtr, Some("Yes"))
          case PartnerDetailType.Corporate_Body => toPartnerDetail(entityType, partnerAddress, None, None, None, None, companyName, tradingName,
            booleanToString(isBusinessIncorporated.fold(false)(x => x)), companyRegDetails, booleanToString(doYouHaveVRN.fold(false)(x => x)), vrn,
            booleanToString(doYouHaveUTR.fold(false)(x => x)), utr, Some("Yes"))
        }
      }

  }

  implicit val formats: Format[Partner] = Json.format[Partner]

}

object BusinessDirector {

  val reader = new Reads[BusinessDirector] {

    def reads(js: JsValue): JsResult[BusinessDirector] =
      for {
        individualStatus <- JsSuccess((js \ "individual" \ "status").asOpt[String])
        firstName <- JsSuccess((js \ "individual" \ "name" \ "firstName").asOpt[String])
        lastName <- JsSuccess((js \ "individual" \ "name" \ "lastName").asOpt[String])
        nino <- JsSuccess((js \ "individual" \ "identification" \ "nino").asOpt[String])
        passportNumber <- JsSuccess((js \ "individual" \ "identification" \ "passportNumber").asOpt[String])
        nationalID <- JsSuccess((js \ "individual" \ "identification" \ "nationalIdNumber").asOpt[String])
        companyStatus <- JsSuccess((js \ "company" \ "status").asOpt[String])
        businessName <- JsSuccess((js \ "company" \ "names" \ "companyName").asOpt[String])
        tradingName <- JsSuccess((js \ "company" \ "names" \ "tradingName").asOpt[String])
        doYouHaveVRN <- JsSuccess((js \ "company" \ "identification" \ "doYouHaveVRN").asOpt[Boolean])
        vrn <- JsSuccess((js \ "company" \ "identification" \ "vrn").asOpt[String])
        doYouHaveCRN <- JsSuccess((js \ "company" \ "identification" \ "doYouHaveCRN").asOpt[Boolean])
        companyRegNumber <- JsSuccess((js \ "company" \ "identification" \ "companyRegNumber").asOpt[String])
        doYouHaveUTR <- JsSuccess((js \ "company" \ "identification" \ "doYouHaveUTR").asOpt[Boolean])
        utr <- JsSuccess((js \ "company" \ "identification" \ "utr").asOpt[String])
      } yield {
        val (directorsAndCompanySecretaries, personOrCompany) = (individualStatus, companyStatus) match {
          case (Some(status), _) => (status, "person")
          case (_, Some(status)) => (status, "company")
          case _ => throw new RuntimeException("Etmp BusinessDirectors read error, neither individualStatus nor companyStatus are present")
        }
        val companyNames = CompanyNamesFact(businessName, tradingName)

        BusinessDirector(
          directorsAndCompanySecretaries = directorsAndCompanySecretaries,
          personOrCompany = personOrCompany,
          firstName = firstName, //to cater for when its none
          lastName = lastName, //to cater for when its none
          doTheyHaveNationalInsurance = if (companyStatus.isEmpty) if (nino.nonEmpty) Some("Yes") else Some("No") else None,
          nino = nino,
          passportNumber = passportNumber,
          nationalID = nationalID,
          otherDirectors = Some("Yes"),
          companyNames = companyNames,
          doYouHaveVRN = if (companyStatus.nonEmpty) booleanToString(doYouHaveVRN.fold(false)(x => x)) else None,
          vrn = vrn,
          doYouHaveCRN = if (companyStatus.nonEmpty) booleanToString(doYouHaveCRN.fold(false)(x => x)) else None,
          companyRegNumber = companyRegNumber,
          doYouHaveUTR = if (companyStatus.nonEmpty) booleanToString(doYouHaveUTR.fold(false)(x => x)) else None,
          utr = utr)
      }

  }

  implicit val formats: Format[BusinessDirector] = Json.format[BusinessDirector]

}


object BusinessDirectors {

  val latestModelVersion = "1.0"

  val reader = new Reads[BusinessDirectors] {

    def reads(js: JsValue): JsResult[BusinessDirectors] =
      for {
        businessDirectors <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "partnerCorporateBody" \ "coOfficialDetails" \ "coOfficial").validate[List[BusinessDirector]](Reads.list(BusinessDirector.reader))
      } yield {
        BusinessDirectors(businessDirectors)
      }

  }

  implicit val formats = Json.format[BusinessDirectors]

}

object Supplier {

  val reader = new Reads[Supplier] {

    def reads(js: JsValue): JsResult[Supplier] =
      for {
        supplierName <- JsSuccess((js \ "name").asOpt[String])
        isSupplierVatRegistered <- JsSuccess((js \ "isSupplierVatRegistered").asOpt[Boolean])
        vatNumber <- JsSuccess((js \ "vrn").asOpt[String])
        supplierAddress <- JsSuccess((js \ "address").asOpt[Address](Address.reader))
      } yield {
        Supplier(
          alcoholSuppliers = Some("Yes"),
          supplierName = supplierName,
          vatRegistered = if (supplierAddress.get.addressCountryCode.isDefined) None else booleanToString(isSupplierVatRegistered.fold(false)(x => x)),
          vatNumber = vatNumber,
          supplierAddress = supplierAddress,
          additionalSupplier = Some("Yes"),
          ukSupplier = if (supplierAddress.get.addressCountryCode.isDefined) Some("No") else Some("Yes")
        )
      }

  }

  implicit val formats = Json.format[Supplier]

}

object Suppliers {

  val reader = new Reads[Suppliers] {

    def reads(js: JsValue): JsResult[Suppliers] =
      for {
        suppliers <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "suppliers" \ "supplier").validate[List[Supplier]](Reads.list(Supplier.reader))
      } yield {
        Suppliers(suppliers = isAdditionalSupplier(suppliers))
      }

  }

  def isAdditionalSupplier(suppliers: List[Supplier]): List[Supplier] =
    suppliers.zipWithIndex.map {
      case (x, i) if i == (suppliers.size - 1) => x.copy(additionalSupplier = Some("No"))
      case (x, i) => x
    }

  implicit val formats = Json.format[Suppliers]

}

object ApplicationDeclaration {

  val reader = new Reads[ApplicationDeclaration] {

    def reads(js: JsValue): JsResult[ApplicationDeclaration] =
      for {
        declarationName <- JsSuccess((js \ "subscriptionType" \ "declaration" \ "nameOfPerson").asOpt[String])
        declarationRole <- JsSuccess((js \ "subscriptionType" \ "declaration" \ "statusOfPerson").asOpt[String])
      } yield {
        ApplicationDeclaration(declarationName = declarationName, declarationRole = declarationRole)
      }

  }

  implicit val formats = Json.format[ApplicationDeclaration]

}

object BusinessType {

  val reader = new Reads[BusinessType] {

    def reads(js: JsValue): JsResult[BusinessType] =
      for {
        legalEntity <- JsSuccess((js \ "subscriptionType" \ "legalEntity").asOpt[String])
        isAgroup <- JsSuccess((js \ "subscriptionType" \ "llpCorporateBody" \ "creatingAGroup").asOpt[Boolean])
      } yield {
        BusinessType(legalEntity = convertLegalEntity(legalEntity, isAgroup.fold(false)(x => x)))
      }

  }

  def convertLegalEntity(legalEntity: Option[String], creatingAGroup: Boolean): Option[String] =
    (legalEntity.fold("")(x => x), creatingAGroup) match {
      case (LegalEntityType.SOLE_TRADER, false) => Some("SOP")
      case (LegalEntityType.SOLE_TRADER, true) => Some("Sole Trader")
      case (LegalEntityType.CORPORATE_BODY, false) => Some("LTD")
      case (LegalEntityType.CORPORATE_BODY, true) => Some("LTD_GRP")
      case (LegalEntityType.PARTNERSHIP, false) => Some("Partnership")
      case (LegalEntityType.PARTNERSHIP, true) => Some("Partnership")
      case (LegalEntityType.LIMITED_LIABILITY_PARTNERSHIP, false) => Some("LLP")
      case (LegalEntityType.LIMITED_LIABILITY_PARTNERSHIP, true) => Some("LLP_GRP")
      case _ => throw new Exception("")
    }

  implicit val formats = Json.format[BusinessType]

}

case class Partners(partners: List[Partner],
                    modelVersion: String = Partners.latestModelVersion
                   ) extends ModelVersionControl

object Partners {

  val latestModelVersion = "1.0"

  val reader = new Reads[Partners] {

    def reads(js: JsValue): JsResult[Partners] =
      for {
        partnerDetails <- (js \ "subscriptionType" \ "businessDetails" \ "partnership" \ "partnerDetails").validate[List[Partner]](Reads.list(Partner.reader))
      } yield {
        Partners(partners = isAdditionalPartner(partnerDetails))
      }

  }

  def isAdditionalPartner(partnerDetails: List[Partner]): List[Partner] =
    partnerDetails.zipWithIndex.map {
      case (x, i) if i == (partnerDetails.size - 1) => x.copy(otherPartners = Some("No"))
      case (x, i) => x
    }

  val writer = new Writes[Partners] {
    def writes(partners: Partners): JsValue = Json.obj("partners" -> Json.toJson(partners.partners))
  }

  implicit val formats = Json.format[Partners]

}

object ChangeIndicators {
  implicit val formats = Json.format[ChangeIndicators]
}

object BusinessDetailsEntityTypes extends Enumeration {
  val SoleTrader = Value("SoleTrader")
  val CorporateBody = Value("CorporateBody")
  val GroupRep = Value("GroupRep")
  val Llp = Value("Llp")
  val Partnership = Value("Partnership")

  val reader = new Reads[BusinessDetailsEntityTypes.Value] {

    def reads(js: JsValue): JsResult[BusinessDetailsEntityTypes.Value] = js match {
      case JsString(s) =>
        Try(BusinessDetailsEntityTypes.withName(s)) match {
          case Success(value) => JsSuccess(value)
          case Failure(e) => JsError(s"Enumeration expected of type: '${BusinessDetailsEntityTypes.getClass}', but it does not appear to contain the value: '$s'")
        }
      case _ => JsError("String value expected")
    }

  }

  implicit val writer = new Writes[BusinessDetailsEntityTypes.Value] {

    def writes(entityType: BusinessDetailsEntityTypes.Value): JsValue = Json.toJson(entityType.toString)

  }

  implicit def autoToString(businessEntityType: BusinessDetailsEntityTypes.Value): String = businessEntityType.toString

}


case class TradingActivity(wholesalerType: List[String],
                           otherWholesaler: Option[String],
                           typeOfAlcoholOrders: List[String],
                           otherTypeOfAlcoholOrders: Option[String],
                           doesBusinessImportAlcohol: Option[String],
                           thirdPartyStorage: Option[String],
                           doYouExportAlcohol: Option[String],
                           exportLocation: Option[List[String]])

case class Products(mainCustomers: List[String],
                    otherMainCustomers: Option[String],
                    productType: List[String],
                    otherProductType: Option[String])

case class BusinessDetails(doYouHaveTradingName: Option[String],
                           tradingName: Option[String],
                           newAWBusiness: Option[NewAWBusiness])

case class BusinessRegistrationDetails(legalEntity: Option[String],
                                       doYouHaveNino: Option[String],
                                       nino: Option[String],
                                       isBusinessIncorporated: Option[String],
                                       companyRegDetails: Option[CompanyRegDetails],
                                       doYouHaveVRN: Option[String],
                                       vrn: Option[String],
                                       doYouHaveUTR: Option[String],
                                       utr: Option[String]) extends CorpNumbersType with IndividualIdNumbersType with IncorporationDetails

case class BusinessContacts(contactAddressSame: Option[String],
                            contactAddress: Option[Address],
                            contactFirstName: String,
                            contactLastName: String,
                            email: String,
                            telephone: String,
                            modelVersion: String = BusinessContacts.latestModelVersion
                           ) extends ModelVersionControl

case class PlaceOfBusiness(mainPlaceOfBusiness: Option[String],
                           mainAddress: Option[Address],
                           placeOfBusinessLast3Years: Option[String],
                           placeOfBusinessAddressLast3Years: Option[Address],
                           operatingDuration: String,
                           modelVersion: String = PlaceOfBusiness.latestModelVersion
                          ) extends ModelVersionControl

case class CheckRegimeModel(businessCustomerDetails: BusinessCustomerDetails,
                            legalEntity: String)

object CheckRegimeModel {
  implicit val formats: Format[CheckRegimeModel] = Json.format[CheckRegimeModel]
}

case class SubscriptionTypeFrontEnd(
                                     legalEntity: Option[BusinessType],
                                     businessPartnerName: String,
                                     groupDeclaration: Option[GroupDeclaration],
                                     businessCustomerDetails: Option[BusinessCustomerDetails],
                                     businessDetails: Option[BusinessDetails],
                                     businessRegistrationDetails: Option[BusinessRegistrationDetails],
                                     businessContacts: Option[BusinessContacts],
                                     placeOfBusiness: Option[PlaceOfBusiness],
                                     partnership: Option[Partners],
                                     groupMembers: Option[GroupMembers],
                                     additionalPremises: Option[AdditionalBusinessPremisesList],
                                     businessDirectors: Option[BusinessDirectors],
                                     tradingActivity: Option[TradingActivity],
                                     products: Option[Products],
                                     suppliers: Option[Suppliers],
                                     applicationDeclaration: Option[ApplicationDeclaration],
                                     changeIndicators: Option[ChangeIndicators],
                                     modelVersion: String = SubscriptionTypeFrontEnd.latestModelVersion
                                   ) extends ModelVersionControl

object BusinessDetails {

  val reader = (legalEntity: Option[String]) => new Reads[BusinessDetails] {

    def reads(js: JsValue): JsResult[BusinessDetails] =
      for {
        tradingName <- legalEntity match {
          case Some("SOP") => JsSuccess((js \ "subscriptionType" \ "businessDetails" \ "soleProprietor" \ "tradingName").asOpt[String])
          case _ => JsSuccess((js \ "subscriptionType" \ "businessDetails" \ "nonProprietor" \ "tradingName").asOpt[String])
        }
        newAWBusiness <- JsSuccess((js \ "subscriptionType").asOpt[NewAWBusiness](NewAWBusiness.reader))
      } yield {
        BusinessDetails(
          doYouHaveTradingName = tradingName match {
            case Some(_) => Some("Yes")
            case None => Some("No")
          },
          tradingName = tradingName,
          newAWBusiness = newAWBusiness)
      }

  }

  implicit val formats: Format[BusinessDetails] = Json.format[BusinessDetails]

}


object BusinessContacts {

  val latestModelVersion = "1.1"

  val reader = new Reads[BusinessContacts] {

    def reads(js: JsValue): JsResult[BusinessContacts] =
      for {
        contactAddressSame <- JsSuccess((js \ "subscriptionType" \ "contactDetails" \ "useAlternateContactAddress").asOpt[Boolean])
        contactAddress <- JsSuccess((js \ "subscriptionType" \ "contactDetails" \ "address").asOpt[Address](Address.reader))
        firstName <- (js \ "subscriptionType" \ "contactDetails" \ "name" \ "firstName").validate[String]
        lastName <- (js \ "subscriptionType" \ "contactDetails" \ "name" \ "lastName").validate[String]
        email <- (js \ "subscriptionType" \ "contactDetails" \ "communicationDetails" \ "email").validate[String]
        telephone <- JsSuccess((js \ "subscriptionType" \ "contactDetails" \ "communicationDetails" \ "telephone").asOpt[String])
      } yield {
        BusinessContacts(
          contactAddressSame = trueToNoOrFalseToYes(contactAddressSame.fold(false)(x => x)),
          contactAddress = contactAddress,
          contactFirstName = firstName,
          contactLastName = lastName,
          email = email,
          telephone = telephone.fold("")(x => x)
        )
      }

  }

  implicit val formats: Format[BusinessContacts] = Json.format[BusinessContacts]

}

object PlaceOfBusiness extends EtmpConstants {

  val latestModelVersion = "1.0"

  val reader = new Reads[PlaceOfBusiness] {

    def reads(js: JsValue): JsResult[PlaceOfBusiness] =
      for {
        mainAddress <- JsSuccess((js \ "subscriptionType" \ "businessAddressForAwrs" \ "currentAddress").asOpt[Address](Address.reader))
        premiseFirstAddress <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "premiseAddress") (0).validate[EtmpAddress](EtmpAddress.reader)
        placeOfBusinessLast3Years <- JsSuccess((js \ "subscriptionType" \ "businessAddressForAwrs" \ "differentOperatingAddresslnLast3Years").asOpt[Boolean])
        placeOfBusinessAddressLast3Years <- JsSuccess((js \ "subscriptionType" \ "businessAddressForAwrs" \ "previousAddress").asOpt[Address](Address.reader))
        operatingDuration <- (js \ "subscriptionType" \ "businessAddressForAwrs" \ "operatingDuration").validate[String]
      } yield {
        PlaceOfBusiness(
          mainPlaceOfBusiness = Some("Yes"),
          mainAddress = mainAddress,
          placeOfBusinessLast3Years = trueToNoOrFalseToYes(placeOfBusinessLast3Years.fold(false)(x => x)),
          placeOfBusinessAddressLast3Years = placeOfBusinessAddressLast3Years,
          operatingDuration = EtmpModelHelper.convertEtmpToArwsOperatingDuration(operatingDuration)
        )
      }

  }

  implicit val formats: Format[PlaceOfBusiness] = Json.format[PlaceOfBusiness]

}

object TradingActivity {

  val reader = new Reads[TradingActivity] {

    def reads(js: JsValue): JsResult[TradingActivity] =
      for {
        wholesalerType <- typeOfWholeSaler(js)
        otherwholeSalerType <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfWholesaler" \ "typeOfWholesalerOther").asOpt[String])
        typeOfAlcoholOrders <- typeOfAlcoholOrders(js)
        otherTypeOfAlcoholOrders <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfAlcoholOrders" \ "typeOfOrderOther").asOpt[String])
        doesBusinessImportAlcohol <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "alcoholGoodsImported").asOpt[Boolean])
        doYouExportAlcohol <- doYouExportAlcohol(js)
        exportLocation <- exportLocation(js)
        thirdPartyStorage <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "thirdPartyStorageUsed").asOpt[Boolean])
      } yield {
        TradingActivity(wholesalerType = wholesalerType,
          otherWholesaler = otherwholeSalerType,
          typeOfAlcoholOrders = typeOfAlcoholOrders,
          otherTypeOfAlcoholOrders = otherTypeOfAlcoholOrders,
          doesBusinessImportAlcohol = booleanToString(doesBusinessImportAlcohol.fold(false)(x => x)),
          thirdPartyStorage = booleanToString(thirdPartyStorage.fold(false)(x => x)),
          doYouExportAlcohol = doYouExportAlcohol,
          exportLocation = exportLocation
        )
      }

  }

  def doYouExportAlcohol(js: JsValue): JsResult[Option[String]] =
    for {
      alcoholGoodsExported <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "alcoholGoodsExported").asOpt[Boolean])
      euDispatches <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "euDispatches").asOpt[Boolean])
    } yield {
      (alcoholGoodsExported, euDispatches) match {
        case (Some(false), Some(false)) => Some("No")
        case _ => Some("Yes")
      }
    }

  def exportLocation(js: JsValue): JsResult[Option[List[String]]] =
    for {
      alcoholGoodsExported <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "alcoholGoodsExported").asOpt[Boolean])
      euDispatches <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "euDispatches").asOpt[Boolean])
    } yield {
      typeOfExports(alcoholGoodsExported, euDispatches)
    }

  def typeOfExports(alcoholGoodsExported: Option[Boolean], euDispatches: Option[Boolean]) =
    (alcoholGoodsExported, euDispatches) match {
      case (Some(true), Some(true)) => Some(List("euDispatches", "outsideEU"))
      case (Some(true), Some(false)) => Some(List("outsideEU"))
      case (Some(false), Some(true)) => Some(List("euDispatches"))
      case (Some(false), Some(false)) => None
      case (None, None) => None
      case (None, Some(false)) => None
      case (None, Some(true))=> None
      case (Some(false), None) => None
      case (Some(true), None) => None
    }

  def typeOfWholeSaler(js: JsValue): JsResult[List[String]] =
    for {
      cashAndCarry <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfWholesaler" \ "cashAndCarry").validate[Boolean]
      offTradeSupplierOnly <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfWholesaler" \ "offTradeSupplierOnly").validate[Boolean]
      onTradeSupplierOnly <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfWholesaler" \ "onTradeSupplierOnly").validate[Boolean]
      all <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfWholesaler" \ "all").validate[Boolean]
      other <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfWholesaler" \ "other").validate[Boolean]
    } yield {
      List(cashAndCarry ? WholesalerType.cashAndCarry | "",
        offTradeSupplierOnly ? WholesalerType.offTradeSupplierOnly | "",
        onTradeSupplierOnly ? WholesalerType.onTradeSupplierOnly | "",
        all ? WholesalerType.all | "",
        other ? WholesalerType.other | "").filter(_ != "")
    }

  def typeOfAlcoholOrders(js: JsValue): JsResult[List[String]] =
    for {
      onlineOnly <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfAlcoholOrders" \ "onlineOnly").validate[Boolean]
      onlineAndTel <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfAlcoholOrders" \ "onlineAndTel").validate[Boolean]
      onlineTelAndPhysical <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfAlcoholOrders" \ "onlineTelAndPhysical").validate[Boolean]
      all <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfAlcoholOrders" \ "all").validate[Boolean]
      other <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfAlcoholOrders" \ "other").validate[Boolean]
    } yield {
      List(onlineOnly ? AlcoholOrdersType.internet | "",
        onlineAndTel ? AlcoholOrdersType.telephoneFax | "",
        onlineTelAndPhysical ? AlcoholOrdersType.faceToface | "",
        all ? AlcoholOrdersType.all | "",
        other ? AlcoholOrdersType.other | "").filter(_ != "")
    }

  def typeOfCustomers(js: JsValue) =
    for {
      pubs <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "pubs").validate[Boolean]
      nightClubs <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "nightClubs").validate[Boolean]
      privateClubs <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "privateClubs").validate[Boolean]
      hotels <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "hotels").validate[Boolean]
      hospitality <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "hospitalityCatering").validate[Boolean]
      restaurants <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "restaurants").validate[Boolean]
      indepRetailers <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "indepRetailers").validate[Boolean]
      nationalRetailers <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "nationalRetailers").validate[Boolean]
      public <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "public").validate[Boolean]
      otherWholesalers <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "otherWholesalers").validate[Boolean]
      all <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "all").validate[Boolean]
      other <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "other").validate[Boolean]
    } yield {
      List(pubs ? TypeOfCustomers.pubs | "",
        nightClubs ? TypeOfCustomers.nightClubs | "",
        privateClubs ? TypeOfCustomers.privateClubs | "",
        hotels ? TypeOfCustomers.hotels | "",
        hospitality ? TypeOfCustomers.hospitalityCatering | "",
        restaurants ? TypeOfCustomers.restaurants | "",
        indepRetailers ? TypeOfCustomers.indepRetailers | "",
        nationalRetailers ? TypeOfCustomers.nationalRetailers | "",
        public ? TypeOfCustomers.public | "",
        otherWholesalers ? TypeOfCustomers.otherWholesalers | "",
        all ? TypeOfCustomers.all | "",
        other ? TypeOfCustomers.other | "").filter(_ != "")
    }

  implicit val formats = Json.format[TradingActivity]

}

object Products {

  val reader = new Reads[Products] {

    def reads(js: JsValue): JsResult[Products] =
      for {
        mainCustomers <- typeOfCustomers(js)
        otherMainCustomers <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "typeOfCustomerOther").asOpt[String])
        productType <- typeOfProduct(js)
        otherProductType <- JsSuccess((js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "typeOfProductOther").asOpt[String])
      } yield {
        Products(
          mainCustomers = mainCustomers,
          otherMainCustomers = otherMainCustomers,
          productType = productType,
          otherProductType = otherProductType
        )
      }

  }

  def typeOfCustomers(js: JsValue): JsResult[List[String]] =
    for {
      pubs <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "pubs").validate[Boolean]
      nightClubs <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "nightClubs").validate[Boolean]
      privateClubs <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "privateClubs").validate[Boolean]
      hotels <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "hotels").validate[Boolean]
      hospitality <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "hospitalityCatering").validate[Boolean]
      restaurants <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "restaurants").validate[Boolean]
      indepRetailers <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "indepRetailers").validate[Boolean]
      nationalRetailers <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "nationalRetailers").validate[Boolean]
      public <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "public").validate[Boolean]
      otherWholesalers <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "otherWholesalers").validate[Boolean]
      all <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "all").validate[Boolean]
      other <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "typeOfCustomers" \ "other").validate[Boolean]
    } yield {
      List(pubs ? TypeOfCustomers.pubs | "",
        nightClubs ? TypeOfCustomers.nightClubs | "",
        privateClubs ? TypeOfCustomers.privateClubs | "",
        hotels ? TypeOfCustomers.hotels | "",
        hospitality ? TypeOfCustomers.hospitalityCatering | "",
        restaurants ? TypeOfCustomers.restaurants | "",
        indepRetailers ? TypeOfCustomers.indepRetailers | "",
        nationalRetailers ? TypeOfCustomers.nationalRetailers | "",
        public ? TypeOfCustomers.public | "",
        otherWholesalers ? TypeOfCustomers.otherWholesalers | "",
        all ? TypeOfCustomers.all | "",
        other ? TypeOfCustomers.other | "").filter(_ != "")
    }

  def typeOfProduct(js: JsValue): JsResult[List[String]] =
    for {
      beer <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "beer").validate[Boolean]
      wine <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "wine").validate[Boolean]
      spirits <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "spirits").validate[Boolean]
      cider <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "cider").validate[Boolean]
      perry <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "perry").validate[Boolean]
      all <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "all").validate[Boolean]
      other <- (js \ "subscriptionType" \ "additionalBusinessInfo" \ "all" \ "productsSold" \ "other").validate[Boolean]
    } yield {
      List(beer ? ProductsSold.beer | "",
        wine ? ProductsSold.wine | "",
        spirits ? ProductsSold.spirits | "",
        cider ? ProductsSold.cider | "",
        perry ? ProductsSold.perry | "",
        all ? ProductsSold.all | "",
        other ? ProductsSold.other | "").filter(_ != "")
    }

  implicit val formats = Json.format[Products]

}

object SubscriptionTypeFrontEnd {

  val latestModelVersion = "1.0"

  val reader = new Reads[SubscriptionTypeFrontEnd] {

    def reads(js: JsValue): JsResult[SubscriptionTypeFrontEnd] =
      for {
        legalEntity <- JsSuccess(js.asOpt[BusinessType](BusinessType.reader))
        businessPartnerName <- (js \ "subscriptionType" \ "businessPartnerName").validate[String]
        groupDeclaration <- JsSuccess(js.asOpt[GroupDeclaration](GroupDeclaration.reader))
        businessCustomerDetails <- JsSuccess(js.asOpt[BusinessCustomerDetails](BusinessCustomerDetails.reader))
        businessDetails <- JsSuccess(js.asOpt[BusinessDetails](BusinessDetails.reader(legalEntity.get.legalEntity)))
        businessRegistrationDetails <- JsSuccess(js.asOpt[BusinessRegistrationDetails](BusinessRegistrationDetails.reader(legalEntity.get.legalEntity)))
        businessContacts <- JsSuccess(js.asOpt[BusinessContacts](BusinessContacts.reader))
        placeOfBusiness <- JsSuccess(js.asOpt[PlaceOfBusiness](PlaceOfBusiness.reader))
        groupMemberDetails <- JsSuccess(js.asOpt[GroupMembers](GroupMembers.reader))
        additionalPremises <- JsSuccess(js.asOpt[AdditionalBusinessPremisesList](AdditionalBusinessPremisesList.reader))
        businessDirectors <- JsSuccess(js.asOpt[BusinessDirectors](BusinessDirectors.reader))
        partnership <- JsSuccess(js.asOpt[Partners](Partners.reader))
        tradingActivity <- JsSuccess(js.asOpt[TradingActivity](TradingActivity.reader))
        products <- JsSuccess(js.asOpt[Products](Products.reader))
        suppliers <- JsSuccess(js.asOpt[Suppliers](Suppliers.reader))
        applicationDeclaration <- JsSuccess(js.asOpt[ApplicationDeclaration](ApplicationDeclaration.reader))
      } yield {
        SubscriptionTypeFrontEnd(
          legalEntity = legalEntity,
          businessCustomerDetails = businessCustomerDetails,
          businessPartnerName = businessPartnerName,
          businessDetails = businessDetails,
          businessRegistrationDetails = businessRegistrationDetails,
          businessContacts = businessContacts,
          placeOfBusiness = placeOfBusiness,
          groupDeclaration = legalEntity.get.legalEntity match {
            case Some("LTD_GRP" | "LLP_GRP") => groupDeclaration
            case _ => None
          },
          groupMembers = legalEntity.get.legalEntity match {
            case Some("LTD_GRP" | "LLP_GRP") => groupMemberDetails
            case _ => None
          },
          partnership = partnership,
          additionalPremises = additionalPremises,
          businessDirectors = legalEntity.get.legalEntity match {
            case Some("LTD" | "LTD_GRP") => Some(amendLastDirector(businessDirectors.reduce((x, y) => x)))
            case _ => None
          },
          tradingActivity = tradingActivity,
          products = products,
          suppliers = hasSupplier(suppliers),
          applicationDeclaration = applicationDeclaration,
          changeIndicators = None,
          modelVersion = latestModelVersion
        )
      }

  }

  def amendLastDirector(businessDirectors: BusinessDirectors): BusinessDirectors =
    BusinessDirectors(businessDirectors.directors.zipWithIndex.map {
      case (x, i) if i == (businessDirectors.directors.size - 1) => x.copy(otherDirectors = Some("No"))
      case (x, i) => x
    })

  def hasSupplier(suppliers: Option[Suppliers]): Option[Suppliers] =
    suppliers match {
      case Some(x) => Some(x)
      case _ => Some(Suppliers(List(Supplier(alcoholSuppliers = Some("No"), None, None, None, None, None, None))))
    }

  implicit val formats = Json.format[SubscriptionTypeFrontEnd]

}
