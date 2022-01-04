package eu.ibagroup.formainframe.analytics

import eu.ibagroup.formainframe.common.message

class PolicyProviderImpl : PolicyProvider {

  private val licenseText by lazy {
    this::class.java.classLoader.getResource("policy.txt")?.readText()
  }

  private val licenseVersion by lazy {
    analyticsProperties.getProperty("policy.version").toInt()
  }

  override val text: String
    get() = licenseText ?: "N/A"

  override fun buildAgreementText(action: String): String {
    return message("analytics.agreement.text", action)
  }

  override val version
    get() = if (licenseText != null) {
      licenseVersion
    } else {
      Int.MAX_VALUE
    }

}