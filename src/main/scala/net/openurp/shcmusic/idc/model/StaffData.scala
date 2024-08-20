/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openurp.shcmusic.idc.model

import java.time.LocalDate

class StaffData {
  var code: String = _
  var name: String = _
  var birthday: Option[LocalDate] = None
  var genderName: String = _
  var idTypeName: String = _
  var idNumber: Option[String] = None
  var departCode: String = _
  var departName: String = _
  var politicalStatusCode: Option[String] = None
  var mobile: Option[String] = None
  var email: Option[String] = None
  var statusCode: String = _
  var statusName: String = _
  var staffTypeCode: String = _
  var degreeCode: Option[String] = None
  var degreeName: Option[String] = None
  var beginOn: Option[LocalDate] = None
  var endOn: Option[LocalDate] = None

  def getDegreeLevelName: Option[String] = {
    if (degreeName.isDefined) {
      if (degreeName.get.contains("学士")) {
        return Some("学士")
      } else if (degreeName.get.contains("硕士")) {
        return Some("硕士")
      } else if (degreeName.get.contains("博士")) {
        return Some("博士")
      } else {
        return None
      }
    }
    None
  }

  override def toString: String = {
    s"Staff(code=$code, name=$name, genderName=$genderName, idTypeName=$idTypeName, idNumber=$idNumber," +
      s" departCode=$departCode, departName=$departName, politicalStatusCode=$politicalStatusCode," +
      s" mobile=$mobile, statusCode=$statusCode, statusName=$statusName, email=$email staffTypeCode=$staffTypeCode)"
  }
}
