package net.openurp.shcmusic.idc.model

class StaffDegreeData {
  var code: String = _
  var eduDegreeCode: String = _
  var eduDegreeName: String = _
  var degreeLevelCode: Option[String] = None
  var degreeLevelName: Option[String] = None
  var lastEduDegree: Boolean = _
  var lastDegree: Boolean = _
}
