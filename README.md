AWRS
====

[![Build Status](https://travis-ci.org/hmrc/awrs.svg)](https://travis-ci.org/hmrc/awrs) [ ![Download](https://api.bintray.com/packages/hmrc/releases/awrs/images/download.svg) ](https://bintray.com/hmrc/releases/awrs/_latestVersion)

This service provides the backend endpoints for the [Alcohol Wholesale Registration Scheme Frontend project](https://github.com/hmrc/awrs-frontend), allowing a customer to apply for the Alcohol Wholesale Registration Scheme.

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## List of APIs

| PATH | Supported Methods | Description |
| --------------- | --------------- | --------------- |
| /org/:orgRef/awrs/send-data | POST | Send organisation application data to ETMP |
| /sa/:utr/awrs/send-data | POST | Send individual application data to ETMP |
| /org/:orgRef/awrs/lookup/:awrsRefNo | GET | Lookup organisation application data in ETMP |
| /sa/:utr/awrs/lookup/:awrsRefNo | GET | Lookup individual application data in ETMP |
| /org/:orgRef/awrs/status/:awrsRefNo | GET | Checks the status of organisation application in ETMP |
| /sa/:utr/awrs/status/:awrsRefNo | GET | Checks the status of individual application in ETMP |
| /org/:orgRef/awrs/update/:awrsRefNo | PUT | Update organisation application data in ETMP |
| /sa/:utr/awrs/update/:awrsRefNo | PUT | Update individual application data in ETMP |
| /:busType/:utr/awrs/status-info/:awrsRefNo/:contactNumber | GET | Gets status info for organisation or individual from ETMP |
| /:busType/:utr/awrs/de-registration/:awrsRefNo | POST | De-register an organisation or individual from ETMP |
| /:busType/:utr/awrs/withdrawal/:awrsRefNo | POST | Withdraw an organisation or individual from ETMP |
| /org/:orgRef/:awrsRefNo/registration-details/:safeId | PUT | Update group registration details in ETMP |

where,

| Parameter | Description | Valid values | Example |
| --------------- | --------------- | --------------- | --------------- |
| orgRef | the organisation reference | string | k0LcJx3AeNgNGD750vfogI5xs20 |
| utr | the unique tax reference number | string | 8560005977 |
| awrsRefNo | the awrs reference number | string | XOAW00000111031 |
| busType | an indicator whether organisation or individual | org or sa | org
| contactNumber | contact phone number | string | 0998772363 |
| safeId | the safe ID | string | XE0001234567890 |

and possible responses are:-

| Response code | Message |
| --------------- | --------------- |
| 200 | OK |
| 404 | Not Found |
| 400 | Bad request |
| 503 | Service unavailable |
| 500 | Internal server error |

**Valid request body for all above POSTs and PUTs:-**
```json
{
   "subscriptionTypeFrontEnd":{
      "legalEntity":{
         "legalEntity":"LTD",
         "isOrgAccount":true
      },
      "businessPartnerName":"Real Business Inc",
      "businessCustomerDetails":{
         "businessName":"Real Business Inc",
         "businessType":"corporate body",
         "businessAddress":{  
            "line_1":"23 High Street",
            "line_2":"Park View",
            "line_3":"Gloucester",
            "line_4":"Gloucestershire",
            "postcode":"NE98 1ZZ",
            "country":"GB"
         },
         "sapNumber":"1234567890",
         "safeId":"XE0001234567890",
         "isAGroup":false,
         "agentReferenceNumber":"JARN1234567",
         "utr":"9999202780"
      },
      "businessDetails":{
         "doYouHaveTradingName":"No",
         "newAWBusiness":{  
            "newAWBusiness":"No"
         }
      },
      "businessRegistrationDetails":{
         "legalEntity":"LTD",
         "doYouHaveUTR":"Yes",
         "utr":"9999202780",
         "isBusinessIncorporated":"Yes",
         "companyRegDetails":{  
            "companyRegistrationNumber":"11111111",
            "dateOfIncorporation":"01/01/2016"
         },
         "doYouHaveVRN":"No"
      },
      "businessContacts":{
         "contactFirstName":"G",
         "contactLastName":"Smythe",
         "telephone":"099877564",
         "email":"xxx@xxx.com",
         "contactAddressSame":"Yes",
         "modelVersion":"1.1"
      },
      "placeOfBusiness":{
         "mainPlaceOfBusiness":"No",
         "mainAddress":{  
            "addressLine1":"23 High Street",
            "addressLine2":"Park View",
            "addressLine3":"Gloucester",
            "addressLine4":"Gloucestershire",
            "postcode":"NE981ZZ"
         },
         "placeOfBusinessLast3Years":"Yes",
         "operatingDuration":"0 to 2 years",
         "modelVersion":"1.0"
      },
      "additionalPremises":{
         "premises":[  
            {  
               "additionalPremises":"No"
            }
         ]
      },
      "businessDirectors":{
         "directors":[  
            {  
               "personOrCompany":"person",
               "firstName":"H",
               "lastName":"Hudson",
               "doTheyHaveNationalInsurance":"Yes",
               "nino":"QQ121212C",
               "directorsAndCompanySecretaries":"Director",
               "otherDirectors":"No"
            }
         ],
         "modelVersion":"1.0"
      },
      "tradingActivity":{
         "wholesalerType":[  
            "01"
         ],
         "typeOfAlcoholOrders":[  
            "02"
         ],
         "doesBusinessImportAlcohol":"No",
         "thirdPartyStorage":"No",
         "doYouExportAlcohol":"No"
      },
      "products":{
         "mainCustomers":[  
            "01"
         ],
         "productType":[  
            "02"
         ]
      },
      "suppliers":{
         "suppliers":[  
            {  
               "alcoholSuppliers":"No"
            }
         ]
      },
      "applicationDeclaration":{
         "declarationName":"Bill",
         "declarationRole":"Bloggs",
         "confirmation":true
      },
      "modelVersion":"1.1"
   }
}
```

### Examples

| Method | URI |
| --------------- | --------------- |
| POST | /org/k0LcJx3AeNgNGD750vfogI5xs20/awrs/send-data |
| POST | /sa/3984535715/awrs/send-data |
| GET | /org/k0LcJx3AeNgNGD750vfogI5xs20/awrs/lookup/XOAW00000111007 |
| GET | /sa/3984535715/awrs/lookup/XOAW00000111007 |
| GET | /org/k0LcJx3AeNgNGD750vfogI5xs20/awrs/status/XOAW00000111007 |
| GET | /sa/3984535715/awrs/status/XOAW00000111007 |
| PUT | /org/k0LcJx3AeNgNGD750vfogI5xs20/awrs/update/XOAW00000111007 |
| PUT | /sa/3984535715/awrs/update/XOAW00000111007 |
| GET | /sa/3984535715/awrs/status-info/XOAW00000111007/09288347362 |
| POST | /org/3984535715/awrs/de-registration/XOAW00000111007 |
| POST | /org/3984535715/awrs/withdrawal/XOAW00000111007 |
| POST | /org/3984535715/XOAW00000111007/registration-details/XE0001234567890 |

