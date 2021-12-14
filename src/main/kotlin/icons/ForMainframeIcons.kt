package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ForMainframeIcons {

  private fun getIcon(path: String): Icon {
    return IconLoader.getIcon(path, this::class.java)
  }

  @JvmField
  val ExplorerToolbarIcon = getIcon("icons/toolWindowLogo.svg")

  @JvmField
  val JclDirectory = getIcon("icons/jclDir.svg")
}
