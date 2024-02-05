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

package uk.gov.hmrc.helpers.utils

import play.api.libs.json.{JsValue, Json}

trait IntegrationData {

  val safeId: String = "XE0001234567890"
  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val statusURI = "/status"
  val enrolmentRef = "XAAW00000123456"
  val enrolmentKey = s"HMRC-AWRS-ORG~AWRSRefNumber~$enrolmentRef"
  val regimeURI = "/registration/details"

  val etmpRegistrationResultInd: String =
    """
      |{
      |  "sapNumber": "1234567890",
      |  "safeId": "XE0001234567890",
      |  "regimeIdentifiers": [
      |    {
      |      "regimeName": "AWRS",
      |      "regimeRefNumber": "XAAW00000123456"
      |    }
      |  ],
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnIndividual": true,
      |  "individual": {
      |        "firstName": "Example",
      |        "lastName": "Exampleson",
      |        "dateOfBirth": "1980-12-25"
      |        },
      |  "addressDetails": {
      |    "addressLine1": "100 SomeStreet",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": "Surrey",
      |    "addressLine4": "London",
      |    "postalCode": "AA1 1AA",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {}
      |}
    """.stripMargin

  val etmpRegistrationResultOrg: String =
    """
      |{
      |  "sapNumber": "1234567890",
      |  "safeId": "XE0001234567890",
      |  "regimeIdentifiers": [
      |    {
      |      "regimeName": "AWRS",
      |      "regimeRefNumber": "XAAW00000123456"
      |    }
      |  ],
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnIndividual": false,
      |  "organisation": {
      |        "organisationName": "Trading Trade"
      |        },
      |  "addressDetails": {
      |    "addressLine1": "100 SomeStreet",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": "Surrey",
      |    "addressLine4": "London",
      |    "postalCode": "AA1 1AA",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {
      |    "regimeName": "AWRS",
      |    "phoneNumber": "01332752856",
      |    "mobileNumber": "07782565326",
      |    "faxNumber": "01332754256",
      |    "emailAddress": "stephen@manncorpone.co.uk"
      |  }
      |}
    """.stripMargin

  val awrsSubscriptionDataOrg: JsValue = Json.parse(
    """{
      |  "subscriptionTypeFrontEnd": {
      |    "legalEntity": {
      |      "legalEntity": "LTD"
      |    },
      |    "businessPartnerName": "BusinessPartner",
      |    "groupDeclaration": {
      |      "groupRepConfirmation": true
      |    },
      |    "businessCustomerDetails": {
      |      "businessName": "Trading Trade",
      |      "businessType": "Corporate Body",
      |      "businessAddress": {
      |        "line_1": "1 Example Street",
      |        "line_2": "Exampe View",
      |        "line_3": "Exampe Town",
      |        "line_4": "Exampeshire",
      |        "postcode": "AA1 1AA",
      |        "country": "GB"
      |      },
      |      "sapNumber": "1234567890",
      |      "safeId": "XE0001234567890",
      |      "isAGroup": false
      |    },
      |    "businessDetails": {
      |      "doYouHaveTradingName": "Yes",
      |      "tradingName": "Trading name",
      |      "newAWBusiness": {
      |        "newAWBusiness": "No"
      |      }
      |    },
      |    "businessRegistrationDetails": {
      |      "legalEntity": "LTD",
      |      "isBusinessIncorporated": "Yes",
      |      "companyRegDetails": {
      |        "companyRegistrationNumber": "55555555",
      |        "dateOfIncorporation": "01/01/2015"
      |      },
      |      "doYouHaveVRN": "Yes",
      |      "vrn": "000000000",
      |      "doYouHaveUTR": "Yes",
      |      "utr": "$utr"
      |    },
      |    "placeOfBusiness": {
      |      "mainPlaceOfBusiness": "Yes",
      |      "placeOfBusinessLast3Years": "Yes",
      |      "operatingDuration": "over 10 years",
      |      "modelVersion" : "1.0"
      |    },
      |    "businessContacts": {
      |      "contactAddressSame": "Yes",
      |      "contactFirstName": "first name",
      |      "contactLastName": "last name",
      |      "email": "Email@email.com",
      |      "telephone": "07000111222",
      |      "modelVersion" : "1.1"
      |    },
      |    "additionalPremises": {
      |      "premises": [
      |        {
      |          "additionalPremises": "Yes",
      |          "additionalAddress": {
      |            "postcode": "BB1 1BB",
      |            "addressLine1": "150 Example Avenue",
      |            "addressLine2": "Exampletown"
      |          },
      |          "addAnother": "No"
      |        }
      |      ]
      |    },
      |    "businessDirectors": {
      |      "directors": [
      |        {
      |          "directorsAndCompanySecretaries": "Director",
      |          "personOrCompany": "person",
      |          "firstName": "sdfsd",
      |          "lastName": "sdfsdfsd",
      |          "doTheyHaveNationalInsurance": "Yes",
      |          "nino": "$nino",
      |          "otherDirectors": "Yes"
      |        },
      |        {
      |          "directorsAndCompanySecretaries": "Director and Company Secretary",
      |          "personOrCompany": "person",
      |          "firstName": "Example",
      |          "lastName": "Exampleson",
      |          "doTheyHaveNationalInsurance": "Yes",
      |          "nino": "$nino",
      |          "otherDirectors": "No"
      |        }
      |      ],
      |      "modelVersion" : "1.0"
      |    },
      |    "tradingActivity": {
      |      "wholesalerType": [
      |        "06",
      |        "01",
      |        "02",
      |        "03",
      |        "04",
      |        "05"
      |      ],
      |      "typeOfAlcoholOrders": [
      |        "01",
      |        "02",
      |        "03",
      |        "04"
      |      ],
      |      "doesBusinessImportAlcohol": "Yes",
      |      "doYouExportAlcohol": "Yes",
      |      "exportLocation": [
      |        "euDispatches",
      |        "outsideEU"
      |      ]
      |    },
      |    "products": {
      |      "mainCustomers": [
      |        "01",
      |        "02",
      |        "03",
      |        "04",
      |        "05",
      |        "06",
      |        "07",
      |        "08"
      |      ],
      |      "doesBusinessImportAlcohol": "Yes",
      |      "productType": [
      |        "02",
      |        "03",
      |        "05",
      |        "06",
      |        "99"
      |      ],
      |      "otherProductType": "rwerwrwerew"
      |    },
      |    "suppliers": {
      |      "suppliers": [
      |        {
      |          "alcoholSuppliers": "Yes",
      |          "supplierName": "dfgdfgfd",
      |          "vatRegistered": "No",
      |          "supplierAddress": {
      |            "postcode": "BB1 1BB",
      |            "addressLine1": "150 Example Avenue",
      |            "addressLine2": "Example Avenue"
      |          },
      |          "additionalSupplier": "No"
      |        }
      |      ]
      |    },
      |    "applicationDeclaration": {
      |      "declarationName": "AWRS application",
      |      "declarationRole": "AWRS application1"
      |    },
      |    "modelVersion": "1.0"
      |  }
      |}
      |""".stripMargin)


  val awrsSubscriptionDataInd: JsValue = Json.parse(
    """{
      |  "subscriptionTypeFrontEnd": {
      |    "legalEntity": {
      |      "legalEntity": "SOP"
      |    },
      |    "businessPartnerName": "BusinessPartner",
      |    "businessCustomerDetails": {
      |      "businessName": "Example Name",
      |      "businessType": "Sole Trader",
      |      "businessAddress": {
      |        "line_1": "1 Example Street",
      |        "line_2": "Exampe View",
      |        "line_3": "Exampe Town",
      |        "line_4": "Exampeshire",
      |        "postcode": "AA1 1AA",
      |        "country": "GB"
      |      },
      |      "sapNumber": "1234567890",
      |      "safeId": "XE0001234567890",
      |      "isAGroup": false,
      |      "agentReferenceNumber": "01234567890",
      |      "firstName": "Example",
      |      "lastName": "Exampleson"
      |    },
      |    "businessDetails": {
      |      "doYouHaveTradingName": "Yes",
      |      "tradingName": "Example Trading",
      |      "newAWBusiness": {
      |        "newAWBusiness": "No"
      |      }
      |    },
      |    "businessRegistrationDetails": {
      |      "legalEntity": "SOP",
      |      "doYouHaveNino": "Yes",
      |      "nino": "$nino",
      |      "doYouHaveVRN": "Yes",
      |      "vrn": "000000000",
      |      "doYouHaveUTR": "Yes",
      |      "utr": "$utr"
      |    },
      |    "placeOfBusiness": {
      |      "mainPlaceOfBusiness": "Yes",
      |      "placeOfBusinessLast3Years": "Yes",
      |      "operatingDuration": "2 to 5 years",
      |      "modelVersion" : "1.0"
      |    },
      |    "businessContacts": {
      |      "contactAddressSame": "Yes",
      |      "contactFirstName": "Example",
      |      "contactLastName": "Exampleson",
      |      "email": "Example@gmail.com",
      |      "confirmEmail": "Example@gmail.com",
      |      "telephone": "01234567891",
      |      "modelVersion" : "1.0"
      |    },
      |    "additionalPremises": {
      |      "premises": [
      |        {
      |          "additionalPremises": "Yes",
      |          "additionalAddress": {
      |            "postcode": "AA1 1AA",
      |            "addressLine1": "82 Example Street",
      |            "addressLine2": "Exampledon"
      |          },
      |          "addAnother": "Yes"
      |        },
      |        {
      |          "additionalPremises": "Yes",
      |          "additionalAddress": {
      |            "postcode": "AA1 1AA",
      |            "addressLine1": "82 Example Street",
      |            "addressLine2": "Exampledon"
      |          },
      |          "addAnother": "No"
      |        }
      |      ]
      |    },
      |    "tradingActivity": {
      |      "wholesalerType": [
      |        "01",
      |        "02",
      |        "03",
      |        "04",
      |        "05",
      |        "99"
      |      ],
      |      "otherWholesaler": "Other wholesaler type",
      |      "typeOfAlcoholOrders": [
      |        "02",
      |        "03",
      |        "99"
      |      ],
      |      "otherTypeOfAlcoholOrders": "Other alcohol order",
      |      "doesBusinessImportAlcohol": "Yes",
      |      "thirdPartyStorage": "Yes",
      |      "doYouExportAlcohol": "Yes",
      |      "exportLocation": [
      |        "euDispatches",
      |        "outsideEU"
      |      ]
      |    },
      |    "products": {
      |      "mainCustomers": [
      |        "02",
      |        "03"
      |      ],
      |      "productType": [
      |        "03",
      |        "99"
      |      ],
      |      "otherProductType": "Other product type"
      |    },
      |    "suppliers": {
      |      "suppliers": [
      |        {
      |          "alcoholSuppliers": "Yes",
      |          "supplierName": "Example Supplier",
      |          "vatRegistered": "No",
      |          "supplierAddress": {
      |            "postcode": "BB1 1BB",
      |            "addressLine1": "3 Example Road",
      |            "addressLine2": "Exampledon"
      |          },
      |          "additionalSupplier": "Yes"
      |        },
      |        {
      |          "alcoholSuppliers": "Yes",
      |          "supplierName": "ExampleName",
      |          "supplierAddress": {
      |            "postcode": "BB1 1BB",
      |            "addressLine1": "3 Example Road",
      |            "addressLine2": "Exampledon"
      |          },
      |          "additionalSupplier": "No"
      |        }
      |      ]
      |    },
      |    "applicationDeclaration": {
      |      "declarationName": "Example Exampleson",
      |      "declarationRole": "Other"
      |    },
      |    "modelVersion": "1.0"
      |  }
      |}
      |""".stripMargin)

  val subscriptionSuccessResponse: String =
    """{
      |"processingDate":"2015-12-17T09:30:47Z",
      |"etmpFormBundleNumber":"123456789012345",
      |"awrsRegistrationNumber": "XAAW00000123456"
      |}""".stripMargin

  val checkEtmpPostDataOrg: JsValue = Json.parse(
    """{
      |  "businessCustomerDetails": {
      |    "businessName": "Trading Trade",
      |    "businessType": "Corporate Body",
      |    "businessAddress": {
      |      "line_1": "1 Example Street",
      |      "line_2": "Exampe View",
      |      "postcode": "AA1 1AA",
      |      "country": "GB"
      |    },
      |    "sapNumber": "1234567890",
      |    "safeId": "XE0001234567890",
      |    "isAGroup": false,
      |    "utr": "utr"
      |  },
      |  "legalEntity": "LTD"
      |}
      |""".stripMargin
  )

  val checkEtmpPostDataInd: JsValue = Json.parse(
    """{
      |  "businessCustomerDetails": {
      |    "businessName": "",
      |    "firstName": "Example",
      |    "lastName": "Exampleson",
      |    "businessType": "SOP",
      |    "businessAddress": {
      |      "line_1": "1 Example Street",
      |      "line_2": "Exampe View",
      |      "postcode": "AA1 1AA",
      |      "country": "GB"
      |    },
      |    "sapNumber": "1234567890",
      |    "safeId": "XE0001234567890",
      |    "isAGroup": false,
      |    "utr": "utr"
      |  },
      |  "legalEntity": "SOP"
      |}
      |""".stripMargin
  )

  val etmpBusinessDetailsData: String = s"""{
    "sapNumber": "1234567890",
    "safeId": "XE0001234567890",
    "agentReferenceNumber": "AARN1234567",
    "regimeIdentifiers": [
    {
      "regimeName": "AWRS",
      "regimeRefNumber": "XAAW00000123456"
    },
    {
      "regimeRefNumber": "XAML00000123456"
    }
    ],
    "nonUKIdentification": {
      "idNumber": "123456",
      "issuingInstitution": "France Institution",
      "issuingCountryCode": "FR"
    },
    "isEditable": true,
    "isAnAgent": false,
    "isAnIndividual": false,
    "organisation": {
      "organisationName": "ACME Trading",
      "isAGroup": false,
      "organisationType": "Corporate body"
    },
    "addressDetails": {
      "addressLine1": "100 SomeStreet",
      "addressLine2": "Wokingham",
      "addressLine3": "Surrey",
      "addressLine4": "London",
      "postalCode": "DH14EJ",
      "countryCode": "GB"
    },
    "contactDetails": {
      "regimeName": "AWRS",
      "phoneNumber": "01332752856",
      "mobileNumber": "07782565326",
      "faxNumber": "01332754256",
      "emailAddress": "stephen@manncorpone.co.uk"
    }
  }""".stripMargin

}