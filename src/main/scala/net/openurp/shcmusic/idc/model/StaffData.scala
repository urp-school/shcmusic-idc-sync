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
  var beginOn: Option[LocalDate] = None
  var endOn: Option[LocalDate] = None

  override def toString: String = {
    s"Staff(code=$code, name=$name, genderName=$genderName, idTypeName=$idTypeName, idNumber=$idNumber," +
      s" departCode=$departCode, departName=$departName, politicalStatusCode=$politicalStatusCode," +
      s" mobile=$mobile, statusCode=$statusCode, statusName=$statusName, email=$email staffTypeCode=$staffTypeCode)"
  }
}
