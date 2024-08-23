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

import org.beangle.cdi.Container
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.EntityDao
import org.beangle.ems.app.EmsApp

import java.io.FileInputStream
import javax.sql.DataSource

object SyncMain extends Logging {

  def main(args: Array[String]): Unit = {
    var accessTokenUrl: String = null
    var clientId: String = null
    var clientSecret: String = null
    var endPoint: String = null

    EmsApp.getAppFile foreach { file =>
      val is = new FileInputStream(file)
      val app = scala.xml.XML.load(is)
      (app \\ "graphql") foreach { e =>
        accessTokenUrl = (e \ "@accessTokenUrl").text.trim
        clientId = (e \ "@clientId").text.trim
        clientSecret = (e \ "@clientSecret").text.trim
        endPoint = (e \ "@endPoint").text.trim
      }
      is.close()
    }
    if (null != accessTokenUrl && null != clientId && null != clientSecret && null != endPoint) {
      val config = Oauth2Config(accessTokenUrl, clientId, clientSecret)
      DaoHelper.main { (entityDao: EntityDao) =>
        Oauth2Helper.fetchAccessToken(config) foreach { token =>
          val dataSource = Container.ROOT.getBean(classOf[DataSource]).get
          new DepartSyncService(entityDao).sync(token, endPoint)
          new StaffSyncService(entityDao).sync(token, endPoint)
          new StdContactSyncService(entityDao).sync(token, endPoint)
          val css = new UserContactSyncService(dataSource)
          css.init()
          css.sync()
        }
      }
    } else {
      logger.error("Graphql config not found")
    }
  }

}
