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

class DepartData {
  var code: String = _
  var name: String = _
  var parentCode: Option[String] = None
  var enabled: Boolean = _

  def layerKey: String = {
    if parentCode.isEmpty then code
    else parentCode.get + "_" + code
  }

  override def toString: String = {
    s"Depart(code=$code, name=$name, parentCode=$parentCode, enabled=$enabled)"
  }
}
