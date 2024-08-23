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

import org.beangle.commons.bean.Initializing
import org.beangle.commons.logging.Logging
import org.beangle.ems.app.dao.AppDataSourceFactory
import org.beangle.jdbc.query.JdbcExecutor

import javax.sql.DataSource

class UserContactSyncService(dataSource: DataSource) extends Initializing, Logging {

  var platformDataSource: DataSource = _

  override def init(): Unit = {
    val ds = new AppDataSourceFactory()
    ds.name = "platform"
    ds.init()
    platformDataSource = ds.result
  }

  def sync(): Unit = {
    val executor = new JdbcExecutor(dataSource)
    val staffEmailUpdateCount = executor.update("update base.users u set email=(select s.email from base.staffs s" +
      " where s.code=u.code) where exists(select * from base.staffs s where s.code=u.code" +
      " and s.email is not null and s.email <> coalesce(u.email,'--'))")
    logger.info(s"更新教职工邮件${staffEmailUpdateCount}条")

    val staffMobileUpdateCount = executor.update("update base.users u set mobile=(select s.mobile from base.staffs s" +
      " where s.code=u.code) where exists(select * from base.staffs s where s.code=u.code" +
      " and s.mobile is not null and s.mobile <> coalesce(u.mobile,'--'))")
    logger.info(s"更新教职工手机${staffMobileUpdateCount}条")

    val stdEmailUpdateCount = executor.update("update base.users u set email=(select c.email from base.students s," +
      " std.contacts c where s.code=u.code and c.std_id=s.id) " +
      " where exists(select * from base.students s,std.contacts c where s.code=u.code and c.std_id=s.id" +
      " and c.email is not null and c.email <> coalesce(u.email,'--'))")
    logger.info(s"更新学生邮件${stdEmailUpdateCount}条")

    val stdMobileUpdateCount = executor.update("update base.users u set mobile=(select c.mobile from base.students s," +
      " std.contacts c where s.code=u.code and c.std_id=s.id) " +
      " where exists(select * from base.students s,std.contacts c where s.code=u.code and c.std_id=s.id" +
      " and c.mobile is not null and c.mobile <> coalesce(u.mobile,'--'))")
    logger.info(s"更新学生邮件${stdMobileUpdateCount}条")

    val plantformExecutor = new JdbcExecutor(platformDataSource)
    val openurpUserContacts = executor.query("select u.code,u.email,u.mobile from base.users u where u.email is not null or u.mobile is not null").map(x => (x(0), x)).toMap
    val platformUserContacts = plantformExecutor.query("select code,email,mobile from ems.usr_users where email is not null or mobile is not null").map(x => (x(0), x)).toMap
    openurpUserContacts foreach { case (code, contact) =>
      platformUserContacts.get(code) foreach { pc =>
        if (pc(1) != contact(1) || pc(2) != contact(2)) {
          if (contact(1) != null && pc(1) != contact(1)) {
            plantformExecutor.update(s"update ems.usr_users set email=? where code=?", contact(1), code)
            logger.info(s"更新${code}的邮件")
          }
          if (contact(2) != null && pc(2) != contact(2)) {
            plantformExecutor.update(s"update ems.usr_users set mobile=? where code=?", contact(2), code)
            logger.info(s"更新${code}的手机")
          }
        }
      }
    }
  }
}
