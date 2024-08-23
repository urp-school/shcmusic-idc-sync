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

import net.openurp.shcmusic.idc.reader.*
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.ems.app.EmsApp.token
import org.openurp.base.hr.model.{Staff, StaffTitle}
import org.openurp.base.model.{Department, School}
import org.openurp.code.edu.model.{Degree, DegreeLevel, EducationDegree}
import org.openurp.code.hr.model.{StaffType, WorkStatus}
import org.openurp.code.job.model.ProfessionalTitle
import org.openurp.code.person.model.{Gender, IdType, PoliticalStatus}
import org.openurp.std.info.model.Contact

import java.time.{Instant, LocalDate}

/** 同步教职工
 *
 */
class StaffSyncService(entityDao: EntityDao) extends Logging {

  def sync(token: String, endPoint: String): Unit = {
    val school = entityDao.getAll(classOf[School]).head
    val staffs = new StaffReader(token, endPoint).read()
    val genders = entityDao.getAll(classOf[Gender]).map(x => (x.name, x)).toMap
    val idTypes = entityDao.getAll(classOf[IdType]).map(x => (x.name, x)).toMap
    val politicalStatuses = entityDao.getAll(classOf[PoliticalStatus]).map(x => (x.code, x)).toMap
    val workstatuses = entityDao.getAll(classOf[WorkStatus]).map(x => (x.name, x)).toMap
    val staffTypes = entityDao.getAll(classOf[StaffType]).map(x => (x.code, x)).toMap
    var degrees = entityDao.getAll(classOf[Degree]).map(x => (x.code, x)).toMap
    val degreeLevels = entityDao.getAll(classOf[DegreeLevel]).map(x => (x.name, x)).toMap
    val defaultStaffType = entityDao.getAll(classOf[StaffType]).find(_.code == "1").get //管理职务人员

    val departMap = entityDao.findBy(classOf[Department], "school", school).map(x => (x.code, x)).toMap
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
      s.degreeCode foreach { degreeCode =>
        if (degrees.contains(degreeCode)) {
          staff.degree = degrees.get(degreeCode)
        } else {
          //create a new degree code
          val degree = new Degree()
          degree.code = degreeCode
          degree.name = s.degreeName.get
          degree.beginOn = LocalDate.now()
          degree.updatedAt = Instant.now
          degree.level = degreeLevels(s.getDegreeLevelName.get)
          entityDao.saveOrUpdate(degree)
          staff.degree = Some(degree)
          degrees = entityDao.getAll(classOf[Degree]).map(x => (x.code, x)).toMap
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
    logger.info(s"同步了${staffs.size}个教职工")
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
    logger.info(s"同步了${titles.size}个教职工职称")
    //同步学历学位信息
    val eduDegrees = new StaffDegreeReader(token, endPoint).read()
    val educationDegrees = entityDao.getAll(classOf[EducationDegree]).map(x => (x.code, x)).toMap

    eduDegrees foreach { t =>
      staffMap.get(t.code) foreach { staff =>
        if (t.lastEduDegree) {
          if (educationDegrees.contains(t.eduDegreeCode)) {
            staff.educationDegree = educationDegrees.get(t.eduDegreeCode)
          } else {
            logger.error(s"不能识别的学历代码:${t.eduDegreeCode} 工号:${staff.code}")
          }
        }
        t.degreeLevelName foreach { degreeLevelName =>
          if (t.lastDegree) {
            if (degreeLevels.contains(degreeLevelName)) {
              staff.degreeLevel = degreeLevels.get(degreeLevelName)
            } else {
              logger.error(s"不能识别的学位层次名称:${degreeLevelName} 工号:${staff.code}")
            }
          }
        }
        entityDao.saveOrUpdate(staff)
      }
    }
    logger.info(s"同步了${eduDegrees.size}个教职工学历学位信息")
  }

}
