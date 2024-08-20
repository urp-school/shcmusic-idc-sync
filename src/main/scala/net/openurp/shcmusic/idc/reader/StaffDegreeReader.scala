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

import net.openurp.shcmusic.idc.model.StaffDegreeData
import org.beangle.commons.collection.Collections

class StaffDegreeReader(token: String, endPoint: String) extends GraphDataReader(token, endPoint) {

  def read(): collection.Seq[StaffDegreeData] = {
    val rows = readTable("V_JZGXXJL_FOR_MIS", "ZGH XLDM XL XWLXDM XWLX SFZGXL SFZGXW")
    val datas = Collections.newBuffer[StaffDegreeData]
    val iter = rows.iterator()
    while (iter.hasNext) {
      val n = iter.next()
      val data = new StaffDegreeData
      data.code = getString(n, "ZGH")
      data.eduDegreeCode = getString(n, "XLDM")
      data.eduDegreeName = getString(n, "XL")
      data.degreeLevelCode = getString2(n, "XWLXDM")
      data.degreeLevelName = getString2(n, "XWLX")
      data.lastEduDegree = getBoolean(n, "SFZGXL")
      data.lastDegree = getBoolean(n, "SFZGXW")
      if (null != data.eduDegreeCode && (data.lastEduDegree || data.lastDegree)) {
        if (data.eduDegreeCode == "104") data.eduDegreeCode = "61"
        else if (data.eduDegreeCode == "101") data.eduDegreeCode = "71"
        else if (data.eduDegreeCode == "107") data.eduDegreeCode = "41"
        else if (data.eduDegreeCode == "109") data.eduDegreeCode = "31"

        datas.addOne(data)
      }
    }
    datas
  }

}
