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

package utils

import org.joda.time.LocalDate
import uk.gov.hmrc.domain._
import utils.TestUtil._

object AwrsTestJson extends AwrsTestJson {
  // We only want one test nino and utr throughout, therefore assign a value in the object declaration
  lazy val testNino = new Generator().nextNino.nino
  lazy val testUtr = new SaUtrGenerator().nextSaUtr.utr
  lazy val testGrpJoinDate = LocalDate.now().toString()
  lazy val testRefNo = "DummmyRef"
  lazy val testSafeId = "DummmyRef"
}

trait AwrsTestJson extends AwrsPathConstants {

  // Frontend Json
  lazy val etmpCheckOrganisationString= loadWithDummy(etmpCheckOrg)
  lazy val etmpCheckIndividualString= loadWithDummy(etmpCheckIndividual)
  lazy val etmpCheckIndividualInvalidString = loadWithDummy(etmpCheckIndividualInvalid)
  lazy val etmpCheckOrganisationInvalidString = loadWithDummy(etmpCheckOrganisationInvalid)
  lazy val api4FrontendSOPString = loadWithDummy(api4FrontendSOP)
  lazy val api4FrontendLTDString = loadWithDummy(api4FrontendLTD)
  lazy val api4FrontendPartnershipString = loadWithDummy(api4FrontendPartnership)
  lazy val api4FrontendLLPString = loadWithDummy(api4FrontendLLP)
  lazy val api4FrontendLPString = loadWithDummy(api4FrontendLP)
  lazy val api4FrontendLLPGRPString = loadWithDummy(api4FrontendLLPGRP)
  lazy val api4FrontendLTDGRPWithCompaniesString = loadWithDummy(api4FrontendLTDGRPWithCompanies)

  lazy val api4FrontendSOPJson = loadAndParseJsonWithDummyData(api4FrontendSOP)
  lazy val api4FrontendLTDJson = loadAndParseJsonWithDummyData(api4FrontendLTD)
  lazy val api4FrontendLTDNewBusinessJson = loadAndParseJsonWithDummyData(api4FrontendLTDNewBusiness)
  lazy val api4FrontendPartnershipJson = loadAndParseJsonWithDummyData(api4FrontendPartnership)
  lazy val api4FrontendLLPJson = loadAndParseJsonWithDummyData(api4FrontendLLP)
  lazy val api4FrontendLPJson = loadAndParseJsonWithDummyData(api4FrontendLP)
  lazy val api4FrontendLLPGRPJson = loadAndParseJsonWithDummyData(api4FrontendLLPGRP)
  lazy val api4FrontendLTDGRPWithCompaniesJson = loadAndParseJsonWithDummyData(api4FrontendLTDGRPWithCompanies)

  lazy val api5FrontendPartnershipJson = loadAndParseJsonWithDummyData(api5FrontendPartnership)
  lazy val api5FrontendLLPJson = loadAndParseJsonWithDummyData(api5FrontendLLP)
  lazy val api5FrontendLLPGroupJson = loadAndParseJsonWithDummyData(api5FrontendLLPGroup)
  lazy val api5FrontendLTDGroupJson = loadAndParseJsonWithDummyData(api5FrontendLTDGroup)

  lazy val api6FrontendLTDJson = loadAndParseJsonWithDummyData(api6FrontendLTD)

  lazy val api3FrontendJson = loadAndParseJsonWithDummyData(api3Frontend)

  // Etmp Json
  lazy val getRegDetailsOrg = loadWithDummy(getRegDetailsOrgJson)
  lazy val getRegDetailsOrgWithRegime = loadWithDummy(getRegDetailsOrgWithRegimeJson)
  lazy val getRegDetailsIndividual = loadWithDummy(getRegDetailsIndividualJson)
  lazy val getRegDetailsIndividualWithRegime = loadWithDummy(getRegDetailsIndividualWithRegimeJson)
  lazy val api4EtmpLTDString = loadWithDummy(api4EtmpLTD)
  lazy val api4EtmpSOPString = loadWithDummy(api4EtmpSOP)
  lazy val api4EtmpPartnershipString = loadWithDummy(api4EtmpPartnership)
  lazy val api4EtmpLLPString = loadWithDummy(api4EtmpLLP)
  lazy val api4EtmpLTDGRPString = loadWithDummy(api4EtmpLTDGRP)
  lazy val api4EtmpLLPGRPString = loadWithDummy(api4EtmpLLPGRP)
  lazy val api4EtmpLTDNewBusinessString = loadWithDummy(api4EtmpLTDNewBusiness)

  lazy val api5EtmpLPString = loadWithDummy(api5EtmpLP)
  lazy val api5EtmpLLPGroupString = loadWithDummy(api5EtmpLLPGroup)
  lazy val api5EtmpLTDGroupString = loadWithDummy(api5EtmpLTDGroup)
  lazy val api5EtmpLTDString = loadWithDummy(api5EtmpLTD)
  lazy val api5EtmpPartnershipString = loadWithDummy(api5EtmpPartnership)
  lazy val api5EtmpSOPString = loadWithDummy(api5EtmpSOP)
  lazy val api5EtmpLLPString = loadWithDummy(api5EtmpLLP)

  lazy val api4EtmpLTDJson = loadAndParseJsonWithDummyData(api4EtmpLTD)
  lazy val api4EtmpPartnershipJson = loadAndParseJsonWithDummyData(api4EtmpPartnership)
  lazy val api4EtmpLLPJson = loadAndParseJsonWithDummyData(api4EtmpLLP)
  lazy val api4EtmpLTDGRPJson = loadAndParseJsonWithDummyData(api4EtmpLTDGRP)
  lazy val api4EtmpLLPGRPJson = loadAndParseJsonWithDummyData(api4EtmpLLPGRP)
  lazy val api4EtmpLTDNewBusinessJson = loadAndParseJsonWithDummyData(api4EtmpLTDNewBusiness)
  lazy val api4EtmpSOPJson = loadAndParseJsonWithDummyData(api4EtmpSOP)

  lazy val api5EtmpLPJson = loadAndParseJsonWithDummyData(api5EtmpLP)
  lazy val api5EtmpLTDJson = loadAndParseJsonWithDummyData(api5EtmpLTD)
  lazy val api5EtmpPartnershipJson = loadAndParseJsonWithDummyData(api5EtmpPartnership)
  lazy val api5EtmpSOPJson = loadAndParseJsonWithDummyData(api5EtmpSOP)
  lazy val api5EtmpLLPJson = loadAndParseJsonWithDummyData(api5EtmpLLP)

  // Request Json

  lazy val api8RequestJson = loadAndParseJsonWithDummyData(api8Request)
  lazy val api10RequestJson = loadAndParseJsonWithDummyData(api10Request)
  lazy val api10OtherReasonRequestJson = loadAndParseJsonWithDummyData(api10OtherReasonRequest)

  // Success Response Json

  lazy val api4SuccessResponse = loadAndParseJsonWithDummyData(api4SuccessfulResponse)

  lazy val api6SuccessResponseJson = loadAndParseJsonWithDummyData(api6SuccessfulResponse)

  lazy val api8SuccessfulResponseJson = loadAndParseJsonWithDummyData(api8SuccessfulResponse)

  lazy val api9SuccessfulResponseJson = loadAndParseJsonWithDummyData(api9SuccessfulResponse)
  lazy val api9SuccessfulResponseUsingCodeJson = loadAndParseJsonWithDummyData(api9SuccessfulResponseUsingCode)
  lazy val api9SuccessfulResponseWithMismatchCasesJson = loadAndParseJsonWithDummyData(api9SuccessfulResponseWithMismatchCases)
  lazy val api9SuccessfulDeRegResponseJson = loadAndParseJsonWithDummyData(api9SuccessfulDeRegResponse)

  lazy val api10SuccessfulResponseJson = loadAndParseJsonWithDummyData(api10SuccessfulResponse)

  lazy val api11SuccessfulResponseJson = loadAndParseJsonWithDummyData(api11SuccessfulResponse)
  lazy val api11SuccessfulResponseJsonWithNewLine = loadAndParseJsonWithDummyData(api11SuccessfulResponseWithNewLine)
  lazy val api11SuccessfulCDATAResponseJson = loadAndParseJsonWithDummyData(api11SuccessfulCDATAResponse)
  lazy val api11SuccessfulCDATAEncodedResponseJson = loadAndParseJsonWithDummyData(api11SuccessfulCDATAEncodedResponse)
  lazy val api11SuccessfulCDATAEncodedResponseJsonWithNewLine = loadAndParseJsonWithDummyData(api11SuccessfulCDATAEncodedResponseWithNewLine)

  // Failure Response Json

  lazy val api11FailureResponseString = loadWithDummy(api11FailureResponse)

  lazy val api8FailureResponseJson = loadAndParseJsonWithDummyData(api8FailureResponse)
  lazy val api10FailureResponseJson = loadAndParseJsonWithDummyData(api10FailureResponse)
  lazy val api11FailureResponseJson = loadAndParseJsonWithDummyData(api11FailureResponse)

  // Common Json

  lazy val commonDirectorsJson = loadAndParseJsonWithDummyData(commonDirectors)
  lazy val commonGroupMemberString = loadWithDummy(commonGroupMember)
  lazy val commonGroupMembersString = loadWithDummy(commonGroupMembers)
  lazy val commonIndPartnershipString = loadWithDummy(commonIndPartnership)
  lazy val commonCorpBodyPartnershipString = loadWithDummy(commonCorpBodyPartnership)
  lazy val commonSOPPartnershipString = loadWithDummy(commonSOPPartnership)
  lazy val commonSOPPartnershipNoRegString = loadWithDummy(commonSOPPartnershipNoReg)
  lazy val commonLLPBusDetailsGroupString = loadWithDummy(commonLLPBusDetailsGroup)

}
