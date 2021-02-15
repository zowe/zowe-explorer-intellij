package eu.ibagroup.formainframe.common

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

const val BUNDLE = "messages.CommonBundle"

class MainframeCommonBundle private constructor() : DynamicBundle(BUNDLE) {
  companion object {
    @JvmStatic
    val INSTANCE = MainframeCommonBundle()
  }
}

fun message(@PropertyKey(resourceBundle = BUNDLE) key: String,
            vararg params: Any): String {
  return MainframeCommonBundle.INSTANCE.getMessage(key, params)
}

fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String,
                vararg params: Any): Supplier<String> {
  return MainframeCommonBundle.INSTANCE.getLazyMessage(key, params)
}
