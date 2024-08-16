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

import net.openurp.shcmusic.idc.reader.{DepartReader, StaffReader, StaffTitleReader}
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.EntityDao
import org.openurp.base.hr.model.{Staff, StaffTitle}
import org.openurp.base.model.{Department, School}
import org.openurp.code.hr.model.{StaffType, WorkStatus}
import org.openurp.code.job.model.ProfessionalTitle
import org.openurp.code.person.model.{Gender, IdType, PoliticalStatus}

import java.time.{Instant, LocalDate}

class SyncService(entityDao: EntityDao, config: Oauth2Config, endPoint: String) extends Logging {

  def sync(): Unit = {
    Oauth2Helper.fetchAccessToken(config) foreach { token =>
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
        if d.enabled then depart.endOn = null
        else {
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
      //同步教职工
      val staffs = new StaffReader(token, endPoint).read()
      val genders = entityDao.getAll(classOf[Gender]).map(x => (x.name, x)).toMap
      val idTypes = entityDao.getAll(classOf[IdType]).map(x => (x.name, x)).toMap
      val politicalStatuses = entityDao.getAll(classOf[PoliticalStatus]).map(x => (x.code, x)).toMap
      val workstatuses = entityDao.getAll(classOf[WorkStatus]).map(x => (x.name, x)).toMap
      val staffTypes = entityDao.getAll(classOf[StaffType]).map(x => (x.code, x)).toMap
      val defaultStaffType = entityDao.getAll(classOf[StaffType]).find(_.code == "1").get //管理职务人员
      val staffMap = Collections.newMap[String, Staff]
      staffs foreach { s =>
        val errors = Collections.newBuffer[String]
        val warnings = Collections.newBuffer[String]
        val staff = entityDao.findBy(classOf[Staff], "school" -> school, "code" -> s.code)
          .headOption.getOrElse(new Staff)
        if (!staff.persisted) {
          staff.code = s.code
          staff.school = school
          staff.beginOn = LocalDate.now
          staff.updatedAt = Instant.now
        }
        if (s.beginOn.nonEmpty) staff.beginOn = s.beginOn.get
        if (s.endOn.nonEmpty) staff.endOn = s.endOn
        //FIXME
        //update begin_on and endOn
        staff.name = s.name
        if (staffTypes.contains(s.staffTypeCode)) {
          staff.staffType = staffTypes(s.staffTypeCode)
        } else {
          staff.staffType = defaultStaffType
          //warnings.addOne(s"不能识别的教职工类别:${s.staffTypeCode},工号：${s.code}")
        }
        if (genders.contains(s.genderName)) {
          staff.gender = genders(s.genderName)
        } else {
          errors.addOne(s"不能识别的性别:${s.genderName},工号：${s.code}")
        }
        if (idTypes.contains(s.idTypeName)) {
          staff.idType = idTypes.get(s.idTypeName)
        } else {
          warnings.addOne(s"不能识别的证件类型:${s.idTypeName},工号：${s.code}")
        }
        staff.idNumber = s.idNumber
        if (s.birthday.nonEmpty) staff.birthday = s.birthday
        if (departMap.contains(s.departCode)) {
          staff.department = departMap(s.departCode)
        } else {
          errors.addOne(s"不能识别的部门:${s.departCode} 工号:${s.code}")
        }
        if (s.politicalStatusCode.nonEmpty) {
          if (politicalStatuses.contains(s.politicalStatusCode.get)) {
            staff.politicalStatus = politicalStatuses.get(s.politicalStatusCode.get)
          } else {
            warnings.addOne(s"不能识别的政治面貌:${s.politicalStatusCode.get},工号：${s.code}")
          }
        }
        if (null != s.statusName) {
          if (workstatuses.contains(s.statusName)) {
            staff.status = workstatuses(s.statusName)
          } else {
            errors.addOne(s"不能识别的在职状态:${s.statusName} 代码:${s.statusCode}")
          }
        }
        s.mobile foreach { m => staff.mobile = Some(m) }
        s.email foreach { e => staff.email = Some(e) }
        if warnings.nonEmpty then logger.warn(warnings.mkString(","))
        if (errors.isEmpty) {
          entityDao.saveOrUpdate(staff)
          staffMap.put(staff.code, staff)
        } else {
          logger.error(errors.mkString(","))
        }
      }
      //同步职称
      val titles = new StaffTitleReader(token, endPoint).read().groupBy(_.code)
      val professionalTitles = entityDao.getAll(classOf[ProfessionalTitle]).map(x => (x.code, x)).toMap
      titles foreach { case (code, ts) =>
        staffMap.get(code) foreach { staff =>
          val exists = entityDao.findBy(classOf[StaffTitle], "staff", staff).map(x => (x.title, x)).toMap
          val activeTitles = Collections.newBuffer[ProfessionalTitle]
          ts foreach { t =>
            if (professionalTitles.contains(t.titleCode)) {
              val title = professionalTitles(t.titleCode)
              val staffTitle = exists.getOrElse(title, new StaffTitle)
              if (!staffTitle.persisted) {
                staffTitle.staff = staff
                staffTitle.title = title
              }
              staffTitle.beginOn = t.beginOn.getOrElse(staff.beginOn)
              staffTitle.endOn = t.endOn
              if (t.active) activeTitles.addOne(title)
              entityDao.saveOrUpdate(staffTitle)
            } else {
              logger.error(s"不能识别的职称:${t.titleCode} 工号:${staff.code}")
            }
          }
          if (activeTitles.size == 1) {
            if (!staff.title.contains(activeTitles.head)) {
              staff.title = Some(activeTitles.head)
            }
          } else {
            staff.title = None
          }
        }
      }
    }
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
