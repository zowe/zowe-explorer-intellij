package eu.ibagroup.formainframe.utils

interface ObjectMatrix<T> {

  operator fun get(i: Int, j: Int): T?

  operator fun set(i: Int, j: Int, value: T?)

}