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

import com.google.gson.Gson
import org.beangle.commons.conversion.string.BooleanConverter
import org.beangle.commons.lang.Strings
import org.beangle.commons.logging.Logging
import org.beangle.commons.net.http.HttpUtils

import java.net.URI
import java.time.LocalDate
import java.util as ju

abstract class GraphDataReader(token: String, endPoint: String) extends Logging {

  def readTable(table: String, columns: String): ju.List[ju.Map[String, _]] = {
    val columnStr = Strings.split(columns).mkString(" ")
    val url = endPoint + s"?access_token=${token}"
    val q = s"""{"query":"{${table}{ edges{ node { ${columnStr} }}}}" }"""
    val rs = HttpUtils.invoke(URI.create(url).toURL, q, "application/json")
    val json = rs.getText
    val g = new Gson().fromJson(json, classOf[ju.Map[String, Object]])
    if (g.containsKey("errors")) {
      logger.error(json)
      ju.Collections.emptyList()
    } else {
      JsonHelper.get(g, s"data.${table}.edges.node").asInstanceOf[ju.List[ju.Map[String, _]]]
    }
  }

  def getDate2(data: ju.Map[String, _], path: String): Option[LocalDate] = {
    val dateStr = getString(data, path)
      if (null == dateStr || dateStr.isEmpty) None else Some(LocalDate.parse(dateStr.substring(0, 10)))
    }

  def getString(data: ju.Map[String, _], path: String): String = {
    data.get(path).asInstanceOf[String]
  }

  def getString2(data: ju.Map[String, _], path: String): Option[String] = {
    Option(data.get(path).asInstanceOf[String])
  }

  def getBoolean(data: ju.Map[String, _], path: String): Boolean = {
    data.get(path) match
      case null => false
      case b: Boolean => b
      case n: Number => n.intValue() == 1
      case s: String => BooleanConverter.apply(s)
  }
}
