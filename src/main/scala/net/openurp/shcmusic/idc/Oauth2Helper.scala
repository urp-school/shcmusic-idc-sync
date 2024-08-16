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

package net.openurp.shcmusic.idc

import com.google.gson.Gson
import org.beangle.commons.logging.Logging
import org.beangle.commons.net.http.HttpUtils

import java.net.URI
import java.util as ju

object Oauth2Helper extends Logging {

  def fetchAccessToken(config: Oauth2Config): Option[String] = {
    val url = config.accessTokenUrl + "?grant_type=client_credentials"
    val rs = HttpUtils.invoke(URI.create(url).toURL, "", "application/x-www-form-urlencoded", config.clientId, config.clientSecret)
    if (rs.isOk) {
      val g = new Gson().fromJson(rs.getText, classOf[ju.Map[String, Object]])
      val keyName = "access_token"
      val token = g.get(keyName)
      if (null == token) {
        logger.error(rs.getText)
        None
      } else Some(token.toString)
    } else {
      logger.error(rs.getText)
      None
    }
  }
}
