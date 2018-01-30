AWRS
====

[![Build Status](https://travis-ci.org/hmrc/awrs.svg)](https://travis-ci.org/hmrc/awrs) [ ![Download](https://api.bintray.com/packages/hmrc/releases/awrs/images/download.svg) ](https://bintray.com/hmrc/releases/awrs/_latestVersion)

This service provides the backend endpoints for the [Alcohol Wholesale Registration Scheme Frontend project](https://github.com/hmrc/awrs-frontend), allowing a customer to apply for apply for the Alcohol Wholesale Registration Scheme.

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## List of APIs

| PATH | Supported Methods | Description |
| --------------- | --------------- | --------------- |
| /org/:orgRef/awrs/send-data | POST |
| /sa/:utr/awrs/send-data | POST |
| /org/:orgRef/awrs/lookup/:awrsRefNo | GET |
| /sa/:utr/awrs/lookup/:awrsRefNo | GET |
| /org/:orgRef/awrs/status/:awrsRefNo | GET |
| /sa/:utr/awrs/status/:awrsRefNo | GET |
| /org/:orgRef/awrs/update/:awrsRefNo | PUT |
| /sa/:utr/awrs/update/:awrsRefNo | PUT |
| /:busType/:utr/awrs/status-info/:awrsRefNo/:contactNumber | GET |
| /:busType/:utr/awrs/de-registration/:awrsRefNo | POST |
| /:busType/:utr/awrs/withdrawal/:awrsRefNo | POST |
| /org/:orgRef/:awrsRefNo/registration-details/:safeId | PUT |

where,

| Parameter | Description |
| --------------- | --------------- |
| orgRef | bla |
| utr | bla |
| awrsRefNo | bla |
| busType | bla |
| contactNumber | bla |
| safeId | bla |

## Usages with request and response

### POST /org/k0LcJx3AeNgNGD750vfogI5xs20/awrs/send-data
**Valid request body:-**
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







