/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chrism.cli

import java.{lang => jl}

import org.apache.commons.lang3.StringUtils

private object OptionValidator {

  def isValidShortOption(opt: String): Boolean = opt != null && isValidShortOptionOrNull(opt)

  def isValidLongOption(opt: String): Boolean = opt != null && isValidLongOptionOrNull(opt)

  def isValidShortOptionOrNull(opt: String): Boolean = isValidOptionOrNull(opt, isAlphaNumeric)

  def isValidLongOptionOrNull(opt: String): Boolean = isValidOptionOrNull(opt, isAlphaNumericOrHyphen)

  private def isValidOptionOrNull(opt: String, charValidator: Char => Boolean): Boolean =
    if (opt == null) {
      true
    } else {
      var valid = StringUtils.isNotBlank(opt)
      if (valid) {
        var i = 0
        while (valid && i < opt.length) {
          val c = opt.charAt(i)
          if (i == 0) {
            if (isHyphen(c)) {
              valid = false
            }
          }
          if (valid) {
            if (!charValidator(c)) {
              valid = false
            }
          }
          i += 1
        }
      }
      valid
    }

  def validateShortOption(opt: String): Unit =
    validateOption(opt, isAlphaNumeric)

  def validateLongOption(opt: String): Unit =
    validateOption(opt, isAlphaNumericOrHyphen)

  private def validateOption(opt: String, charValidator: Char => Boolean): Unit =
    if (!isValidOptionOrNull(opt, charValidator)) {
      throw new InvalidOptionException(s"$opt is not a valid option!")
    }

  private def isAlphaNumeric(c: Char): Boolean = jl.Character.isAlphabetic(c) || jl.Character.isDigit(c)

  private def isHyphen(c: Char): Boolean = c == '-'

  private def isAlphaNumericOrHyphen(c: Char): Boolean = isAlphaNumeric(c) || isHyphen(c)
}
