package eu.ibagroup.formainframe.utils

class SparseMatrix<T>(
  private val defaultValue: T? = null,
) : ObjectMatrix<T> {

  private val map = mutableMapOf<Pair<Int, Int>, T?>()

  override fun get(i: Int, j: Int): T? {
    return map[Pair(i, j)] ?: defaultValue
  }

  override fun set(i: Int, j: Int, value: T?) {
    map[Pair(i, j)] = value
  }

  @Suppress("UNCHECKED_CAST")
  val asMap: Map<Pair<Int, Int>, T>
    get() = map.filter { it.value != null } as Map<Pair<Int, Int>, T>

  fun clean() {
    asMap.forEach {
      map[it.key] = defaultValue
    }
  }

}