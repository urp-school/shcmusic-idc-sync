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

import net.openurp.shcmusic.idc.reader.DepartReader
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.EntityDao
import org.openurp.base.model.{Department, School}

import java.time.{Instant, LocalDate}

class DepartSyncService(entityDao: EntityDao) extends Logging {

  def sync(token: String, endPoint: String): Unit = {
    val departs = new DepartReader(token, endPoint).read()
    val departMap = Collections.newMap[String, Department]
    val school = entityDao.getAll(classOf[School]).head
    //同步组织架构
    departs.sortBy(_.layerKey) foreach { d =>
      val depart = entityDao.findBy(classOf[Department], "school" -> school, "code" -> d.code)
        .headOption.getOrElse(new Department)
      if (!depart.persisted) {
        depart.code = d.code
        depart.school = school
        depart.beginOn = LocalDate.now
        depart.updatedAt = Instant.now
      }
      depart.name = d.name
      if !d.enabled then {
        if null == depart.endOn then depart.endOn = Some(LocalDate.now)
      }
      d.parentCode foreach { c =>
        depart.parent = departMap.get(c)
      }
      if null == depart.indexno then depart.indexno = d.code
      entityDao.saveOrUpdate(depart)
      departMap.put(depart.code, depart)
    }
    //重新生成部门的排序
    reindexDepart(school)
    logger.info(s"同步了${departs.size}个部门")
  }

  def reindexDepart(school: School): Unit = {
    val departs = entityDao.findBy(classOf[Department], "school", school)
    val roots = departs.filter(_.parent.isEmpty).sortBy(_.code).toBuffer
    var index = 1
    val len = String.valueOf(roots.length).length
    roots foreach { root =>
      root.indexno = Strings.leftPad(index.toString, len, '0')
      initDepartIndexno(root, root.indexno + ".")
      index += 1
    }
    entityDao.saveOrUpdate(departs)
  }

  private def initDepartIndexno(depart: Department, prefix: String): Unit = {
    var i = 1
    val len = String.valueOf(depart.children.length).length
    depart.children.sortBy(_.code) foreach { d =>
      d.indexno = prefix + Strings.leftPad(i.toString, len, '0')
      initDepartIndexno(d, d.indexno + ".")
      i += 1
    }
  }
}
