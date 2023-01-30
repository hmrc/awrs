/*
 * Copyright 2023 HM Revenue & Customs
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

object AwrsPathConstants extends AwrsPathConstants

trait AwrsPathConstants {

  lazy val schemaPath = "/schema/API4EtmpSchema.json"

  // Frontend Json paths
  lazy val etmpCheckOrg = "/json/API4/etmpCheck/EtmpCheckOrg.json"
  lazy val etmpCheckIndividual = "/json/API4/etmpCheck/EtmpCheckIndividual.json"
  lazy val etmpCheckIndividualInvalid = "/json/API4/etmpCheck/EtmpCheckIndividualInvalid.json"
  lazy val etmpCheckOrganisationInvalid = "/json/API4/etmpCheck/EtmpCheckOrganisationInvalid.json"
  lazy val api4FrontendLTD = "/json/API4/frontend/AwrsLTD.json"
  lazy val api4FrontendSOP = "/json/API4/frontend/AwrsSOP.json"
  lazy val api4FrontendPartnership = "/json/API4/frontend/AwrsPartnership.json"
  lazy val api4FrontendLLP = "/json/API4/frontend/AwrsLLP.json"
  lazy val api4FrontendLLPGRP = "/json/API4/frontend/AwrsLLPGroup.json"
  lazy val api4FrontendLP = "/json/API4/frontend/AwrsLP.json"
  lazy val api4FrontendLTDNewBusiness = "/json/API4/frontend/AwrsLTDNewBusiness.json"
  lazy val api4FrontendLTDGRPWithCompanies = "/json/API4/frontend/AwrsLTDGroup.json"

  lazy val api5FrontendLLP = "/json/API5/frontend/AwrsLLP.json"
  lazy val api5FrontendPartnership = "/json/API5/frontend/AwrsPartnership.json"
  lazy val api5FrontendLLPGroup = "/json/API5/frontend/AwrsLLPGroup.json"
  lazy val api5FrontendLTDGroup = "/json/API5/frontend/AwrsLTDGroup.json"

  lazy val api6FrontendLTD = "/json/API6/frontend/AwrsLTD.json"
  lazy val api3Frontend = "/json/API3/frontend/API3.json"

  // ETMP Json paths

  lazy val api4EtmpLTD = "/json/API4/etmp/EtmpLTD.json"
  lazy val api4EtmpSOP = "/json/API4/etmp/EtmpSOP.json"
  lazy val api4EtmpPartnership = "/json/API4/etmp/EtmpPartnership.json"
  lazy val api4EtmpLLP = "/json/API4/etmp/EtmpLLP.json"
  lazy val api4EtmpLTDGRP = "/json/API4/etmp/EtmpLTDGroup.json"
  lazy val api4EtmpLLPGRP = "/json/API4/etmp/EtmpLLPGroup.json"
  lazy val api4EtmpLTDNewBusiness = "/json/API4/etmp/EtmpLTDNewBusiness.json"

  lazy val api5EtmpLLP = "/json/API5/etmp/EtmpLLP.json"
  lazy val api5EtmpLP = "/json/API5/etmp/EtmpLP.json"
  lazy val api5EtmpLLPGroup = "/json/API5/etmp/EtmpLLPGroups.json"
  lazy val api5EtmpLTD = "/json/API5/etmp/EtmpLTD.json"
  lazy val api5EtmpLTDGroup = "/json/API5/etmp/EtmpLTDGroups.json"
  lazy val api5EtmpPartnership = "/json/API5/etmp/EtmpPartnership.json"
  lazy val api5EtmpSOP = "/json/API5/etmp/EtmpSOP.json"
  lazy val api5EtmpSOPWithRef = "/json/API5/etmp/EtmpSOPWithAWRSRef.json"
  lazy val getRegDetailsOrgJson = "/json/getRegistrationDetails/EtmpNoRegimeOrg.json"
  lazy val getRegDetailsOrgWithRegimeJson = "/json/getRegistrationDetails/EtmpWithRegimeOrg.json"
  lazy val getRegDetailsIndividualJson = "/json/getRegistrationDetails/EtmpNoRegimeIndividual.json"
  lazy val getRegDetailsIndividualWithRegimeJson = "/json/getRegistrationDetails/EtmpWithRegimeIndividual.json"

  // Common Json paths

  lazy val commonDirectors = "/json/common/Directors.json"
  lazy val commonGroupMember = "/json/common/GroupMember.json"
  lazy val commonGroupMembers = "/json/common/GroupMembers.json"
  lazy val commonIndPartnership = "/json/common/IndividualPartnership.json"
  lazy val commonCorpBodyPartnership = "/json/common/CorporateBodyPartnership.json"
  lazy val commonSOPPartnership = "/json/common/SOPPartnership.json"
  lazy val commonSOPPartnershipNoReg = "/json/common/SOPPartnershipWithNoCompanyRegDetails.json"
  lazy val commonLLPBusDetailsGroup = "/json/common/LLPBusinessDetailsGroup.json"

  // Request Json paths

  lazy val api8Request = "/json/API8/request/Reason.json"
  lazy val api10Request = "/json/API10/request/GroupDisbanded.json"
  lazy val api10OtherReasonRequest = "/json/API10/request/OtherReason.json"

  // Success Json paths

  lazy val api4SuccessfulResponse = "/json/API4/etmp/SuccessfullResponse.json"

  lazy val api6SuccessfulResponse = "/json/API6/etmp/SuccessfulResponse.json"

  lazy val api8SuccessfulResponse = "/json/API8/response/Successful.json"

  lazy val api9SuccessfulResponse = "/json/API9/SuccessfulPending.json"
  lazy val api9SuccessfulResponseUsingCode = "/json/API9/SuccessfulPendingUsingCode.json"
  lazy val api9SuccessfulResponseWithMismatchCases = "/json/API9/SuccessfulPendingWithMismatchCases.json"
  lazy val api9SuccessfulDeRegResponse = "/json/API9/SuccessfulDeReg.json"

  lazy val api10SuccessfulResponse = "/json/API10/response/SuccessfulResponse.json"

  lazy val api11SuccessfulResponse = "/json/API11/SuccessfulResponse.json"
  lazy val api11SuccessfulResponseWithNewLine = "/json/API11/SuccessfulResponseWithBr.json"
  lazy val api11SuccessfulCDATAResponse = "/json/API11/SuccessfulCDATAResponse.json"
  lazy val api11SuccessfulCDATAEncodedResponse = "/json/API11/SuccessfulCDATAEncodedResponse.json"
  lazy val api11SuccessfulCDATAEncodedResponseWithNewLine = "/json/API11/SuccessfulCDATAEncodedResponseWithNewLine.json"

  // Failure Json paths

  lazy val api8FailureResponse = "/json/API8/response/Failure.json"
  lazy val api10FailureResponse = "/json/API10/response/FailureResponse.json"
  lazy val api11FailureResponse = "/json/API11/FailureResponse.json"

}
