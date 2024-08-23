import org.openurp.parent.Settings.*

ThisBuild / organization := "net.openurp.shcmusic"
ThisBuild / version := "0.0.2-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/urp-school/shcmusic-idc"),
    "scm:git@github.com:urp-school/shcmusic-idc.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "Shcmusic IDC Sync"
ThisBuild / homepage := Some(url("http://openurp.github.io/urp-school/index.html"))

val openurp_start_task = "org.openurp.starter" % "openurp-starter-task" % "0.3.39"
val openurp_std_api = "org.openurp.std" % "openurp-std-api" % "0.41.2"
lazy val web = (project in file("."))
  .settings(
    name := "shcmusic-idc-sync",
    common,
    Compile / mainClass := Some("net.openurp.shcmusic.idc.SyncMain"),
    libraryDependencies ++= Seq(openurp_start_task, openurp_std_api)
  )
