package eu.ibagroup.formainframe.dataops.jeslog

interface LogProvider {
  fun provideLog(): String
  fun isLogFinished(): Boolean
  fun logPostfix(): String = ""
}
