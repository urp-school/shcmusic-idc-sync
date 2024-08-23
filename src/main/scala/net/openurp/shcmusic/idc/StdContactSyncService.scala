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

import net.openurp.shcmusic.idc.reader.StudentContactReader
import org.beangle.commons.lang.Objects
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.std.model.Student
import org.openurp.std.info.model.Contact

import java.time.Instant

/** 同步学生联系方式
 *
 * @param entityDao
 */
class StdContactSyncService(entityDao: EntityDao) extends Logging {

  def sync(token: String, endPoint: String): Unit = {
    val contacts = new StudentContactReader(token, endPoint).read()
    val cQuery = OqlBuilder.from[Array[Any]](classOf[Contact].getName, "c").select("c.std.code,c.email,c.mobile")
    val existContacts = entityDao.search(cQuery).map(x => x(0) -> x).toMap
    logger.info(s"开始同步${contacts.size}个学生的联系方式")
    var changedCnt = 0
    contacts foreach { contact =>
      val stdCode = contact.code
      existContacts.get(stdCode) match
        case None =>
          getContact(stdCode) foreach { c =>
            c.email = contact.email
            c.mobile = contact.mobile
            c.updatedAt = Instant.now
            entityDao.saveOrUpdate(c)
            changedCnt += 1
          }
        case Some(c) =>
          var changed = false
          contact.email foreach { email =>
            if !Objects.equals(email, c(1)) then
              getContact(stdCode).foreach { c =>
                c.email = Some(email)
                c.updatedAt = Instant.now
                entityDao.saveOrUpdate(c)
                changed = true
              }
          }
          contact.mobile foreach { mobile =>
            if !Objects.equals(mobile, c(2)) then
              getContact(stdCode).foreach { c =>
                c.mobile = Some(mobile)
                c.updatedAt = Instant.now
                entityDao.saveOrUpdate(c)
                changed = true
              }
          }
          if changed then changedCnt += 1
    }
    logger.info(s"变更了${changedCnt}个学生的联系方式")
  }

  def getContact(code: String): Option[Contact] = {
    entityDao.findBy(classOf[Student], "code", code).headOption match
      case None => None
      case Some(std) =>
        entityDao.findBy(classOf[Contact], "std", std).headOption match
          case None =>
            val c = new Contact
            c.std = std
            c.updatedAt = Instant.now
            entityDao.saveOrUpdate(c)
            Some(c)
          case Some(c) => Some(c)
  }
}
