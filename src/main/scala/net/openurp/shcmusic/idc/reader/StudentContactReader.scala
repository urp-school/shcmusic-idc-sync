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

package net.openurp.shcmusic.idc.reader

import net.openurp.shcmusic.idc.model.StudentContactData
import org.beangle.commons.collection.Collections

class StudentContactReader(token: String, endPoint: String) extends GraphDataReader(token, endPoint) {
  def read(): collection.Seq[StudentContactData] = {
    val rows = readTable("V_YJS_FOR_MIS", "XH SJH DZXX")
    val datas = Collections.newBuffer[StudentContactData]
    val iter = rows.iterator()
    while (iter.hasNext) {
      val n = iter.next()
      val data = new StudentContactData
      data.code = getString(n, "XH")
      data.mobile = getString2(n, "SJH")
      data.email = getString2(n, "DZXX")
      if (data.mobile.nonEmpty) {
        datas.addOne(data)
      }
    }
    datas
  }

}
