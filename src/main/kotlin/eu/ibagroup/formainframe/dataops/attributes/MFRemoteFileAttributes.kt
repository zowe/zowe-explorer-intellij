package eu.ibagroup.formainframe.dataops.attributes

interface MFRemoteFileAttributes<R> : VFileInfoAttributes {

  val url: String

  val requesters: MutableList<R>

}

interface Requester {
  val user: String
}