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

import net.openurp.shcmusic.idc.model.StaffTitleData
import org.beangle.commons.collection.Collections

class StaffTitleReader(token: String, endPoint: String) extends GraphDataReader(token, endPoint) {

  def read(): collection.Seq[StaffTitleData] = {
    val rows = readTable("V_JZG_ZYJSZW_FOR_MIS", "ZGH PRQSRQ PRZYJSZWDM PRZYJSZWJBDM PRZZRQ SFXZW")
    val datas = Collections.newBuffer[StaffTitleData]
    val iter = rows.iterator()
    while (iter.hasNext) {
      val n = iter.next()
      val data = new StaffTitleData
      data.code = getString(n, "ZGH")
      data.beginOn = getDate2(n, "PRQSRQ")
      data.titleCode = getString(n, "PRZYJSZWDM")
      data.endOn = getDate2(n, "PRZZRQ")
      data.active = getBoolean(n, "SFXZW")
      if (null != data.titleCode) {
        datas.addOne(data)
      }
    }
    val staffTitles = datas.groupBy(_.code)
    val datas2 = Collections.newBuffer[StaffTitleData]
    staffTitles foreach { case (code, titles) =>
      titles.groupBy(_.titleCode) foreach { case (titleCode, tl) =>
        if (tl.size == 1) datas2.addAll(tl)
        else if (tl.size > 1) datas2.addOne(tl.filter(_.beginOn.nonEmpty).minBy(_.beginOn.get))
      }
    }
    datas2
  }

}
