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

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

object DecodeText {

  def decodeBase64Text(text: String): String = {
    val decodedBytes = Base64.getDecoder.decode(text.getBytes(UTF_8))
    new String(decodedBytes, UTF_8)
  }

}
