package eu.ibagroup.formainframe.dataops

interface Operation<Result> {

  val resultClass: Class<out Result>

}