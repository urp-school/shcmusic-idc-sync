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

import net.openurp.shcmusic.idc.model.StaffData
import org.beangle.commons.collection.Collections

import java.time.LocalDate

class StaffReader(token: String, endPoint: String) extends GraphDataReader(token, endPoint) {

  def read(): collection.Seq[StaffData] = {
    val rows = readTable("V_JZG_FOR_MIS", "ZGH XM XB CSRQ SFZJH ZJH XKSDM XKSMC ZZMMDM SJH DQZTDM DQZTMC DZXX JZGLBDM")
    val datas = Collections.newBuffer[StaffData]
    val iter = rows.iterator()
    while (iter.hasNext) {
      val n = iter.next()
      val data = new StaffData
      data.code = getString(n, "ZGH")
      data.name = getString(n, "XM")
      data.genderName = getString(n, "XB")
      data.birthday = getDate2(n, "CSRQ")
      data.idTypeName = getString(n, "SFZJH")
      data.idNumber = getString2(n, "ZJH")
      data.departCode = getString(n, "XKSDM")
      data.departName = getString(n, "XKSMC")
      data.politicalStatusCode = getString2(n, "ZZMMDM")
      data.mobile = getString2(n, "SJH")
      data.statusCode = getString(n, "DQZTDM")
      data.statusName = getString(n, "DQZTMC")
      data.staffTypeCode = getString(n, "JZGLBDM")
      data.email = getString2(n, "DZXX")

      if (data.endOn.isEmpty && Set("离退休", "去世", "离校", "不报到").contains(data.statusName)) {
        data.endOn = Some(LocalDate.now.minusDays(1))
      }
      if (null != data.departCode) {
        datas.addOne(data)
      }
    }
    datas
  }

}
