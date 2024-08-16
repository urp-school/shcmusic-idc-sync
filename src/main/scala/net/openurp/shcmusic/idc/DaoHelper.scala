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

import org.beangle.cdi.spring.context.DefaultContextInitializer
import org.beangle.data.dao.EntityDao
import org.beangle.data.orm.hibernate.SessionHelper
import org.hibernate.SessionFactory

object DaoHelper {

  def main(execute: (entityDao: EntityDao) => Unit): Unit = {
    val container = DefaultContextInitializer(null,null).init()
    val entityDao = container.getBean(classOf[EntityDao]).get
    val sessionFactory = container.getBean(classOf[SessionFactory]).get
    val session = SessionHelper.openSession(sessionFactory)
    try {
      execute(entityDao)
    } finally {
      SessionHelper.closeSession(session.session)
    }
  }

}
