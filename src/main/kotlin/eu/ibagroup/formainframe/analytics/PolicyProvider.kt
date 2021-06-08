package eu.ibagroup.formainframe.analytics

interface PolicyProvider {

  val text: String

  fun buildAgreementText(action: String): String

  val version: Int

}