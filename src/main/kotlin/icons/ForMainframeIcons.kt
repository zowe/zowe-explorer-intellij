package icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import javax.swing.Icon

object ForMainframeIcons {

  private fun getIcon(path: String): Icon {
    return IconLoader.getIcon(path, this::class.java)
  }

  @JvmField
  val ExplorerToolbarIcon = getIcon("icons/toolWindowLogo.svg")

  @JvmField
  val JclDirectory = getIcon("icons/jclDir.svg")

  @JvmField
  val DatasetMask = getIcon("icons/datasetMask.svg")

  @JvmField
  val MemberIcon = IconUtil.addText(AllIcons.FileTypes.Any_type, "MEM")
}
