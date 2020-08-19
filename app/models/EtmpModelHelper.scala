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

package models

import org.joda.time.LocalDate
import play.api.libs.json._
import utils.{EtmpConstants, Utility}
import play.api.libs.json.JodaWrites._

object EtmpModelHelper extends EtmpModelHelper

trait EtmpModelHelper extends EtmpConstants {

  private lazy val VrnPrefix = "GB"
  private lazy val EmptyString = ""

  def identificationIndividualIdNumbersType(identification: IndividualIdNumbersType): JsObject =
    Json.obj("doYouHaveNino" -> yestoTrue(identification.doYouHaveNino.fold("")(x => x)))
      .++(identification.nino.fold(Json.obj())(x => Json.obj("nino" -> x)))
      .++(identificationCorpNumbersType(identification)) // this adds the vrn and utr

  def identificationCorpNumbersType(identification: CorpNumbersType): JsObject =
    Json.obj("doYouHaveVRN" -> yestoTrue(identification.doYouHaveVRN.fold("")(x => x)))
      .++(identification.vrn.fold(Json.obj())(x => Json.obj("vrn" -> removeVrnPrefix(x))))
      .++(Json.obj("doYouHaveUTR" -> yestoTrue(identification.doYouHaveUTR.fold("")(x => x))))
      .++(identification.utr.fold(Json.obj())(x => Json.obj("utr" -> x)))

  def identificationCorpNumbersWithCRNType(identification: CorpNumbersWithCRNType): JsObject =
    identificationCorpNumbersType(identification) // this adds the vrn and utr
      .++(Json.obj("doYouHaveCRN" -> yestoTrue(identification.doYouHaveCRN.fold("")(x => x))))
      .++(identification.companyRegNumber.fold(Json.obj()) {companyRegNo =>
        val regexPattern = """^[0-9]{7}$"""
        val paddedCompanyRegNumber = if (companyRegNo.matches(regexPattern)) {
          "0" + companyRegNo
        } else {
          companyRegNo
        }

        Json.obj("companyRegNumber" -> paddedCompanyRegNumber)
      })

  def identificationIncorporationDetails(identification: IncorporationDetails): JsObject =
    identification.isBusinessIncorporated match {
      case Some("Yes") =>
        val crn = identification.companyRegDetails.reduceLeft((x, y) => x).companyRegistrationNumber.toUpperCase
        val regexPattern = """^[0-9]{7}$"""
        val paddedCompanyRegNumber = if (crn.matches(regexPattern)) {
          "0" + crn
        } else {
          crn
        }

        Json.obj(
          "isBusinessIncorporated" -> true,
          "companyRegistrationNumber" -> paddedCompanyRegNumber,
          "dateOfIncorporation" -> Utility.awrsToEtmpDateFormatter(identification.companyRegDetails.reduceLeft((x, y) => x).dateOfIncorporation)
        )
      case _ => Json.obj("isBusinessIncorporated" -> false)
    }

  @inline def ifExistsThenPopulate(key: String, value: Option[String]) =
    value match {
      case Some(v) => Json.obj(key -> v)
      case _ => Json.obj()
    }

  def toEtmpLegalEntity(st: SubscriptionTypeFrontEnd): JsValue = {
    val legalEntity = st.legalEntity.reduceLeft((x, y) => x).legalEntity

    JsString(legalEntity match {
      case Some("LTD") | Some("LTD_GRP") => LegalEntityType.CORPORATE_BODY
      case Some("Partnership") => LegalEntityType.PARTNERSHIP
      case Some("LLP") | Some("LP") | Some("LLP_GRP") => LegalEntityType.LIMITED_LIABILITY_PARTNERSHIP
      case _ => LegalEntityType.SOLE_TRADER
    })
  }

  def toEtmpBusinessDetails(st: SubscriptionTypeFrontEnd): JsValue =
    st.legalEntity.reduceLeft((x, y) => x).legalEntity match {
      case Some("SOP") => toEtmpSoleTraderBusinessDetails(st)
      case _ => toEtmpBaseBusinessDetails(st)
    }

  def toEtmpNewAWBusiness(st: SubscriptionTypeFrontEnd): JsObject = {

    val newAWBusiness = st.businessDetails.get.newAWBusiness.fold(NewAWBusiness("No", None))(x => x)

    val dateJsonObject = newAWBusiness.proposedStartDate match {
      case Some(date) => Json.obj("proposedStartDate" -> JsString(Utility.awrsToEtmpDateFormatter(date)))
      case _ => Json.obj()
    }

    Json.obj("newAWBusiness" -> yestoTrue(newAWBusiness.newAWBusiness))
      .++(dateJsonObject)
  }

  val yestoTrue = (s: String) => s match {
    case "Yes" => true
    case "No" => false
    case _ => false
  }

  val NotoTrue = (s: String) => s match {
    case "No" => true
    case "Yes" => false
    case _ => false
  }

  def toGroupDeclaration(st: SubscriptionTypeFrontEnd): JsValue = {
    val groupDeclarationFlag =
      st.legalEntity.get.legalEntity.get match {
        case "LTD_GRP" | "LLP_GRP" => true
        case _ => false
      }

    Json.obj(
      "creatingAGroup" -> groupDeclarationFlag,
      "groupRepConfirmation" -> groupDeclarationFlag)
  }

  def createLLPCorporateBody(st: SubscriptionTypeFrontEnd) =
    st.legalEntity.reduceLeft((x, y) => x).legalEntity match {
      case Some("Partnership") => Json.obj()
      case _ =>
        val businessDetails = st.businessDetails.reduceLeft((x, y) => x)
        val businessRegistrationDetails = st.businessRegistrationDetails.reduceLeft((x, y) => x)
        val incorporationDetails = identificationIncorporationDetails(businessRegistrationDetails)
        val llpCorporateBody = Json.obj("incorporationDetails" -> incorporationDetails).++(createDateGroupRepresantativeJoined(st))

        Json.obj("llpCorporateBody" -> llpCorporateBody)
    }

  def toEtmpSoleTraderBusinessDetails(st: SubscriptionTypeFrontEnd): JsValue = {
    val soleTraderBusinessDetails = st.businessDetails.reduceLeft((x, y) => x)
    val businessRegistrationDetails = st.businessRegistrationDetails.reduceLeft((x, y) => x)
    val identification = identificationIndividualIdNumbersType(businessRegistrationDetails)
    val soleTrader =
      ifExistsThenPopulate("tradingName", soleTraderBusinessDetails.tradingName) ++
        Json.obj(
          "identification" -> identification)

    Json.obj(ProprietorType.soleTrader -> soleTrader)
  }

  def toEtmpBaseBusinessDetails(st: SubscriptionTypeFrontEnd): JsValue = {
    val businessDetails = st.businessDetails.reduceLeft((x, y) => x)
    val businessRegistrationDetails = st.businessRegistrationDetails.reduceLeft((x, y) => x)
    val identification = identificationCorpNumbersType(businessRegistrationDetails)
    val partnership =
      st.legalEntity match {
        case Some(BusinessType(Some("LLP") | Some("LLP_GRP") | Some("LP") | Some("Partnership"))) =>
          Json.obj("partnership" -> toEtmpPartnership(st))
        case _ => Json.obj()
      }
    val nonProprietor =
      ifExistsThenPopulate("tradingName", businessDetails.tradingName) ++
        Json.obj(
          "identification" -> identification)

    Json.obj(ProprietorType.nonProprietor -> nonProprietor)
      .++(partnership)
      .++(createLLPCorporateBody(st))
  }

  def createDateGroupRepresantativeJoined(st: SubscriptionTypeFrontEnd): JsObject =
    st.legalEntity.get.legalEntity match {
      case Some("LTD_GRP" | "LLP_GRP") => Json.obj("dateGrpRepJoined" -> LocalDate.now().toString)
      case _ => Json.obj()
    }

  def toEtmpGroupMemberDetails(st: SubscriptionTypeFrontEnd): JsValue = {
    val groupMembers = st.groupMembers.reduceLeft((x, y) => x)

    Json.obj("numberOfGrpMembers" -> groupMembers.members.size.toString)
      .++(toEtmpGroupMembers(st))
  }

  def toEtmpGroupMembers(st: SubscriptionTypeFrontEnd): JsObject = {
    val groupMembers = st.groupMembers.reduceLeft((x, y) => x)
    val x =
      for {
        groupMembers <- groupMembers.members
      } yield {
        toEtmpGroupMember(groupMembers)
      }
    val memberArray = x.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))

    Json.obj("groupMember" -> memberArray)
  }

  def toEtmpGroupMember(groupMember: GroupMember): JsValue = {
    val names =
      ifExistsThenPopulate("companyName", groupMember.companyNames.businessName) ++ ifExistsThenPopulate("tradingName", groupMember.companyNames.tradingName)
    val incorporationDetails = identificationIncorporationDetails(groupMember)
    val identification = identificationCorpNumbersType(groupMember)

    Json.obj(
      "names" -> names,
      "incorporationDetails" -> incorporationDetails,
      "groupJoiningDate" -> LocalDate.now(),
      "address" -> toEtmpAddress(groupMember.address.reduceLeft((x, y) => y)),
      "identification" -> identification)
  }

  def copyBusinessAddressToMainAddress(st: SubscriptionTypeFrontEnd): Address = {

    val placeOfBusiness = st.placeOfBusiness.reduceLeft((x, y) => x)
    yestoTrue(placeOfBusiness.mainPlaceOfBusiness.fold("")(x => x)) match {
      case true =>
        val businessAndAddress = st.businessCustomerDetails.reduceLeft((x, y) => x).businessAddress

        placeOfBusiness.copy(mainAddress = Some(Address(postcode = businessAndAddress.postcode,
          addressLine1 = businessAndAddress.line_1,
          addressLine2 = businessAndAddress.line_2,
          addressLine3 = businessAndAddress.line_3,
          addressLine4 = businessAndAddress.line_4))).mainAddress.reduceLeft((x, y) => y)
      case false => placeOfBusiness.mainAddress.reduceLeft((x, y) => y)
    }

  }

  def toEtmpCommunicationDetails(st: SubscriptionTypeFrontEnd): JsValue = {
    val businessContacts = st.businessContacts.reduceLeft((x, y) => x)
    val preTelephoneNumberRegex = "^[+]".r
    val preTelephoneNumber = "00"

    Json.obj(
      "email" -> businessContacts.email,
      "telephone" -> preTelephoneNumberRegex.replaceFirstIn(businessContacts.telephone, preTelephoneNumber))
  }

  val getPlaceOfBusinessLast3Years = (placeOfBusinessLast3Years: String, placeOfBusinessAddressLast3Years: Option[Address]) =>
    placeOfBusinessLast3Years match {
      case "No" => (true, Json.obj("previousAddress" -> toEtmpAddress(placeOfBusinessAddressLast3Years.reduceLeft((x, y) => y))))
      case _ => (false, Json.obj())
    }

  def toEtmpBusinessAddressForAwrs(st: SubscriptionTypeFrontEnd): JsValue = {
    val placeOfBusiness = st.placeOfBusiness.reduceLeft((x, y) => x)
    val (differentOperatingAddresslnLast3Years, previousAddress) =
      getPlaceOfBusinessLast3Years(placeOfBusiness.placeOfBusinessLast3Years.fold("")(x => x), placeOfBusiness.placeOfBusinessAddressLast3Years)

    Json.obj(
      "currentAddress" -> toEtmpAddress(copyBusinessAddressToMainAddress(st)),
      "communicationDetails" -> toEtmpCommunicationDetails(st),
      "operatingDuration" -> matchDuration(placeOfBusiness.operatingDuration),
      "differentOperatingAddresslnLast3Years" -> differentOperatingAddresslnLast3Years)
      .++(previousAddress)
  }

  private def matchDuration(duration: String): String = {
    val operationDurationPattern = """^[0-9]*$"""
    val awrsToEtmpYearRange = Map(LessThanTwoYearsAwrs -> ZeroToTwoYearsEtmp,
      TwoToFourYearsAwrs -> TwoToFiveYearsEtmp,
      FiveToNineYearsAwrs -> FiveToTenYearsEtmp,
      OverTenYearsAwrs -> OverTenYearsEtmp
    )

    duration.matches(operationDurationPattern) match {
      case true => convertOperatingDuration(duration)
      case _ => awrsToEtmpYearRange.getOrElse(duration, duration)
    }
  }

  private def convertOperatingDuration(duration: String): String = {

    duration.toInt match {
      case x: Int if x >= 0 && x < 2 => ZeroToTwoYearsEtmp
      case x: Int if x >= 2 && x < 5 => TwoToFiveYearsEtmp
      case x: Int if x >= 5 && x < 10 => FiveToTenYearsEtmp
      case x: Int if x >= 10 => OverTenYearsEtmp
      case _ => throw new IllegalArgumentException("Duration must be positive")

    }
  }

  def convertEtmpToArwsOperatingDuration(duration: String): String = {
    val etmpToAwrsYearRange = Map(ZeroToTwoYearsEtmp -> LessThanTwoYearsAwrs,
      TwoToFiveYearsEtmp -> TwoToFourYearsAwrs,
      FiveToTenYearsEtmp -> FiveToNineYearsAwrs,
      OverTenYearsEtmp -> OverTenYearsAwrs)
    etmpToAwrsYearRange.getOrElse(duration, duration)
  }

  def toEtmpName(st: SubscriptionTypeFrontEnd): JsValue = {
    val businessDetails = st.businessContacts.reduceLeft((x, y) => x)

    Json.obj(
      "firstName" -> businessDetails.contactFirstName,
      "lastName" -> businessDetails.contactLastName)
  }

  def toEtmpAddress(address: Address): JsValue =
    ifExistsThenPopulate("postalCode", address.postcode)
      .++(Json.obj(
        "addressLine1" -> address.addressLine1,
        "addressLine2" -> address.addressLine2))
      .++(ifExistsThenPopulate("addressLine3", address.addressLine3))
      .++(ifExistsThenPopulate("addressLine4", address.addressLine4))
      .++(Json.obj("countryCode" -> address.addressCountryCode.fold("GB")(x => x)))

  def toEtmpContactDetails(st: SubscriptionTypeFrontEnd): JsValue = {
    val businessDetails = st.businessContacts.reduceLeft((x, y) => x)
    val address = businessDetails.contactAddressSame match {
      case Some("No") => Json.obj("address" -> toEtmpAddress(businessDetails.contactAddress.reduceLeft((x, y) => y)))
      case _ => Json.obj()
    }

    Json.obj(
      "name" -> toEtmpName(st),
      "useAlternateContactAddress" -> NotoTrue(businessDetails.contactAddressSame.fold("")(x => x)))
      .++(address)
      .++(Json.obj("communicationDetails" -> toEtmpCommunicationDetails(st)))
  }

  def toEtmpPremises(st: SubscriptionTypeFrontEnd): JsValue = {
    val additionalPremises = st.additionalPremises.reduceLeft((x, y) => x)
    val premiseAddressList = additionalPremises.premises.flatMap(x => x.additionalPremises match {
      case Some("Yes") => x.additionalAddress
      case _ => None
    })
    val additionalPremisesUpdated: List[Address] = copyBusinessAddressToMainAddress(st) :: premiseAddressList

    val x =
      for {
        additionalBusinessPremise <- additionalPremisesUpdated
      } yield {
        Json.obj("address" -> toEtmpAddress(additionalBusinessPremise))
      }

    x.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
  }

  def toEtmpSupplierDetails(sp: Supplier): JsValue =
    Json.obj(
      "name" -> sp.supplierName,
      "isSupplierVatRegistered" -> yestoTrue(sp.vatRegistered.fold("No")(x => x)))
      .++(ifExistsThenPopulate("vrn", removeVrnPrefix(sp.vatNumber)))
      .++(Json.obj("address" -> toEtmpAddress(sp.supplierAddress.reduceLeft((x, y) => y))))


  def toEtmpSupplier(st: SubscriptionTypeFrontEnd): JsValue = {
    val suppliers = st.suppliers.reduceLeft((x, y) => x)

    val suppliersJson =
      for {
        supplier <- suppliers.suppliers
      } yield {
        toEtmpSupplierDetails(supplier)
      }

    Json.obj("supplier" -> JsArray(suppliersJson))
  }

  def toEtmpAdditionalBusinessInfo(st: SubscriptionTypeFrontEnd): JsValue = {
    val tradingActivity = st.tradingActivity.reduceLeft((x, y) => x)
    val products = st.products.reduceLeft((x, y) => x)
    val premises = st.additionalPremises.reduceLeft((x, y) => x)

    def ifOtherThen(hasOther: Boolean, otherFieldKey: => String, otherFieldValue: => Option[String]) =
      if (hasOther) {
        Json.obj(otherFieldKey -> otherFieldValue.fold("")(x => x))
      } else {
        Json.obj()
      }

    val typeOfWholesaler = {
      val hasOther = tradingActivity.wholesalerType.contains(WholesalerType.other)

      Json.obj(
        "cashAndCarry" -> tradingActivity.wholesalerType.contains(WholesalerType.cashAndCarry),
        "offTradeSupplierOnly" -> tradingActivity.wholesalerType.contains(WholesalerType.offTradeSupplierOnly),
        "onTradeSupplierOnly" -> tradingActivity.wholesalerType.contains(WholesalerType.onTradeSupplierOnly),
        "all" -> tradingActivity.wholesalerType.contains(WholesalerType.all),
        "other" -> hasOther)
        .++(ifOtherThen(hasOther, "typeOfWholesalerOther", tradingActivity.otherWholesaler))
    }

    val typeOfAlcoholOrders = {
      val hasOther = tradingActivity.typeOfAlcoholOrders.contains(AlcoholOrdersType.other)

      Json.obj(
        "onlineOnly" -> tradingActivity.typeOfAlcoholOrders.contains(AlcoholOrdersType.internet),
        "onlineAndTel" -> tradingActivity.typeOfAlcoholOrders.contains(AlcoholOrdersType.telephoneFax),
        "onlineTelAndPhysical" -> tradingActivity.typeOfAlcoholOrders.contains(AlcoholOrdersType.faceToface),
        "all" -> tradingActivity.typeOfAlcoholOrders.contains(AlcoholOrdersType.all),
        "other" -> hasOther)
        .++(ifOtherThen(hasOther, "typeOfOrderOther", tradingActivity.otherTypeOfAlcoholOrders))
    }

    val typeOfCustomers = {
      val hasOther = products.mainCustomers.contains(TypeOfCustomers.other)

      Json.obj(
        "pubs" -> products.mainCustomers.contains(TypeOfCustomers.pubs),
        "nightClubs" -> products.mainCustomers.contains(TypeOfCustomers.nightClubs),
        "privateClubs" -> products.mainCustomers.contains(TypeOfCustomers.privateClubs),
        "hotels" -> products.mainCustomers.contains(TypeOfCustomers.hotels),
        "hospitalityCatering" -> products.mainCustomers.contains(TypeOfCustomers.hospitalityCatering),
        "restaurants" -> products.mainCustomers.contains(TypeOfCustomers.restaurants),
        "indepRetailers" -> products.mainCustomers.contains(TypeOfCustomers.indepRetailers),
        "nationalRetailers" -> products.mainCustomers.contains(TypeOfCustomers.nationalRetailers),
        "public" -> products.mainCustomers.contains(TypeOfCustomers.public),
        "otherWholesalers" -> products.mainCustomers.contains(TypeOfCustomers.otherWholesalers),
        "all" -> products.mainCustomers.contains(TypeOfCustomers.all),
        "other" -> hasOther)
        .++(ifOtherThen(hasOther, "typeOfCustomerOther", products.otherMainCustomers))
    }

    val productsSold = {
      val hasOther = products.productType.contains(ProductsSold.other)

      Json.obj(
        "beer" -> products.productType.contains(ProductsSold.beer),
        "wine" -> products.productType.contains(ProductsSold.wine),
        "spirits" -> products.productType.contains(ProductsSold.spirits),
        "cider" -> products.productType.contains(ProductsSold.cider),
        "perry" -> products.productType.contains(ProductsSold.perry),
        "all" -> products.productType.contains(ProductsSold.all),
        "other" -> products.productType.contains(ProductsSold.other))
        .++(ifOtherThen(hasOther, "typeOfProductOther", products.otherProductType))
    }

    val numberOfPremises =
      premises.premises.head.additionalPremises match {
        case Some("No") => "1"
        case _ => (premises.premises.size + 1).toString
      }

    // the mapping behaviour of alcoholGoodsExported and euDispatches is as specified by AWRS-446
    // "alcoholGoodsExported" is solely mapped to "outsideEU" and "euDispatches" solely to "euDispatches"
    val all =
    Json.obj(
      "typeOfWholesaler" -> typeOfWholesaler,
      "typeOfAlcoholOrders" -> typeOfAlcoholOrders,
      "typeOfCustomers" -> typeOfCustomers,
      "productsSold" -> productsSold,
      "numberOfPremises" -> numberOfPremises,
      "premiseAddress" -> toEtmpPremises(st),
      "thirdPartyStorageUsed" -> yestoTrue(tradingActivity.thirdPartyStorage.fold("")(x => x)),
      "alcoholGoodsExported" -> tradingActivity.exportLocation.fold(List(""))(x => x).contains("outsideEU"),
      "euDispatches" -> tradingActivity.exportLocation.fold(List(""))(x => x).contains("euDispatches"))
      .++(createEtmpSupplierJson(st))
      .++(Json.obj("alcoholGoodsImported" -> yestoTrue(tradingActivity.doesBusinessImportAlcohol.fold("")(x => x))))

    val legalEntity = st.legalEntity match {
      case Some(BusinessType(Some("SOP"))) => Json.obj()
      case Some(BusinessType(_)) => toEtmpAdditionalBusinessInfoPartnerCorporateBody(st)
    }

    legalEntity
      .++(Json.obj("all" -> all))
  }

  def createEtmpSupplierJson(st: SubscriptionTypeFrontEnd) =
    st.suppliers match {
      case Some(_) =>
        st.suppliers.get.suppliers.head.alcoholSuppliers match {
          case Some("Yes") => toEtmpSuppliers(st)
          case _ => Json.obj()
        }
      case _ => Json.obj()
    }

  def toEtmpSuppliers(st: SubscriptionTypeFrontEnd) = Json.obj("suppliers" -> toEtmpSupplier(st))

  def toEtmpPartnership(st: SubscriptionTypeFrontEnd) = {
    val partners = st.partnership

    partners match {
      case None => Json.obj("numberOfPartners" -> "0")
      case _ =>
        Json.obj(
          "numberOfPartners" -> partners.reduceLeft((x, y) => x).partners.size.toString,
          "partnerDetails" -> toEtmpPartnerDetail(partners.get.partners))
    }
  }

  def toEtmpPartnerDetail(partnerDetail: List[Partner]): JsValue =
    JsArray(partnerDetail map (x => buildPartnersArray(x)))

  def buildPartnersArray(partnerDetail: Partner): JsValue =
    partnerDetail.entityType match {
      case Some("Individual") => toEtmpPartnerDetailsIndividual(partnerDetail)
      case Some("Corporate Body") => toEtmpPartnerDetailsCompany(partnerDetail)
      case Some("Sole Trader") => toEtmpPartnerDetailsSoleTrader(partnerDetail)
    }

  def toEtmpPartnerDetailsIndividual(partnerDetail: Partner) = {
    val name = Json.obj("firstName" -> partnerDetail.firstName, "lastName" -> partnerDetail.lastName)
    val individual =
      Json.obj(
        "name" -> name,
        "doYouHaveNino" -> yestoTrue(partnerDetail.doYouHaveNino.fold("")(x => x)))
        .++(ifExistsThenPopulate("nino", partnerDetail.nino))

    Json.obj(
      "entityType" -> partnerDetail.entityType,
      "partnerAddress" -> toEtmpAddress(partnerDetail.partnerAddress.reduceLeft((x, y) => y)),
      "individual" -> individual)
  }

  def toEtmpPartnerDetailsCompany(partnerDetail: Partner) = {
    val names = ifExistsThenPopulate("companyName", partnerDetail.companyNames.businessName) ++
      ifExistsThenPopulate("tradingName", partnerDetail.companyNames.tradingName)
    val incorporationDetails = identificationIncorporationDetails(partnerDetail)
    val identification = identificationCorpNumbersType(partnerDetail)

    Json.obj(
      "entityType" -> partnerDetail.entityType,
      "partnerAddress" -> toEtmpAddress(partnerDetail.partnerAddress.reduceLeft((x, y) => y)),
      "names" -> names,
      "identification" -> identification,
      "incorporationDetails" -> incorporationDetails)
  }

  def toEtmpPartnerDetailsSoleTrader(partnerDetail: Partner) = {
    val name = ifExistsThenPopulate("firstName", partnerDetail.firstName) ++ ifExistsThenPopulate("lastName", partnerDetail.lastName)
    val identification = identificationCorpNumbersType(partnerDetail)
    val soleProprietor =
      partnerDetail.companyNames.fold(Json.obj())(x => x.tradingName.fold(Json.obj())(y => Json.obj("tradingName" -> y)))
        .++(Json.obj(
          "name" -> name,
          "doYouHaveNino" -> yestoTrue(partnerDetail.doYouHaveNino.fold("")(x => x))))
        .++(ifExistsThenPopulate("nino", partnerDetail.nino))
        .++(Json.obj("identification" -> identification))

    Json.obj(
      "entityType" -> partnerDetail.entityType,
      "partnerAddress" -> toEtmpAddress(partnerDetail.partnerAddress.reduceLeft((x, y) => y)),
      "soleProprietor" -> soleProprietor)
  }

  def buildDirectorsArray(coOfficial: BusinessDirector): JsValue =
    coOfficial.personOrCompany match {
      case "company" => toEtmpCoOfficialCompanyDetails(coOfficial)
      case _ => toEtmpCoOfficialIndividualDetails(coOfficial)
    }

  def toEtmpDeclaration(st: SubscriptionTypeFrontEnd): JsValue = {
    val declaration = st.applicationDeclaration

    val statusofPerson = declaration.get.declarationRole.fold("Other")(x => StatusOfPerson.listOfStatus.find(_
      .equalsIgnoreCase(x)).fold("Other")(s => s))

    val personStatusOther =
      statusofPerson match {
        case "Other" => Json.obj("personStatusOther" -> declaration.get.declarationRole.fold("")(x => x))
        case _ => Json.obj()
      }

    Json.obj(
      "nameOfPerson" -> declaration.get.declarationName.fold("")(x => x),
      "statusOfPerson" -> statusofPerson)
      .++(personStatusOther)
      .++(Json.obj("informationIsAccurateAndComplete" -> true))
  }

  def toEtmpBusinessDirectors(businessDirectors: BusinessDirectors): JsValue = {
    val officials = businessDirectors.directors map (x => buildDirectorsArray(x))

    Json.obj("coOfficial" -> JsArray(officials))
  }

  def toEtmpCoOfficialIndividualDetails(coOfficial: BusinessDirector): JsValue = {
    val name = Json.obj("firstName" -> coOfficial.firstName, "lastName" -> coOfficial.lastName)
    val identification =
      ifExistsThenPopulate("nino", coOfficial.nino)
        .++(ifExistsThenPopulate("passportNumber", coOfficial.passportNumber))
        .++(ifExistsThenPopulate("nationalIdNumber", coOfficial.nationalID))

    val individual =
      Json.obj(
        "status" -> coOfficial.directorsAndCompanySecretaries,
        "name" -> name,
        "identification" -> identification
      )

    Json.obj("individual" -> individual)
  }

  def toEtmpCoOfficialCompanyDetails(coOfficial: BusinessDirector): JsValue = {
    val names = ifExistsThenPopulate("companyName", coOfficial.companyNames.businessName) ++
      ifExistsThenPopulate("tradingName", coOfficial.companyNames.tradingName)
    val identification = identificationCorpNumbersWithCRNType(coOfficial)

    val company =
      Json.obj(
        "status" -> coOfficial.directorsAndCompanySecretaries,
        "names" -> names,
        "identification" -> identification)

    Json.obj("company" -> company)
  }

  def toEtmpAdditionalBusinessInfoPartnerCorporateBody(st: SubscriptionTypeFrontEnd) = {
    val businessDirectors = st.businessDirectors

    businessDirectors match {
      case None =>
        val numberOfCoOfficials = Json.obj("numberOfCoOfficials" -> "0")

        Json.obj("partnerCorporateBody" -> numberOfCoOfficials)
      case _ =>
        val partnerCorporateBody =
          Json.obj(
            "numberOfCoOfficials" -> businessDirectors.reduceLeft((x, y) => x).directors.size.toString,
            "coOfficialDetails" -> toEtmpBusinessDirectors(businessDirectors.get))

        Json.obj("partnerCorporateBody" -> partnerCorporateBody)
    }
  }

  def toEtmpChangeIndicators(st: SubscriptionTypeFrontEnd) = {
    val changeIndicator = st.changeIndicators.reduceLeft((x, y) => x)

    Json.obj(
      "additionalBusinessInfoChanged" -> changeIndicator.additionalBusinessInfoChanged,
      "businessAddressChanged" -> changeIndicator.businessAddressChanged,
      "businessDetailsChanged" -> changeIndicator.businessDetailsChanged,
      "contactDetailsChanged" -> changeIndicator.contactDetailsChanged,
      "coOfficialsChanged" -> changeIndicator.coOfficialsChanged,
      "declarationChanged" -> changeIndicator.declarationChanged,
      "groupMembersChanged" -> changeIndicator.groupMembersChanged,
      "partnersChanged" -> changeIndicator.partnersChanged,
      "premisesChanged" -> changeIndicator.premisesChanged,
      "suppliersChanged" -> changeIndicator.suppliersChanged)
  }

  private def removePrefix(prefix: String, stringToSantise: String) = stringToSantise.replace(prefix, EmptyString)

  private def removeVrnPrefix(vrnToSanitise: String) = removePrefix(VrnPrefix, vrnToSanitise)

  private def removeVrnPrefix(vrnToSanitise: Option[String]): Option[String] = vrnToSanitise match {
    case Some(vrn) => Some(removeVrnPrefix(vrn))
    case _ => None
  }

}
