package eu.ibagroup.formainframe.utils

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PropertyWithLock<V>(
  private var value: V,
  private val lock: Lock
) : ReadWriteProperty<Any?, V> {

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
    lock.withLock { this.value = value }
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>) = lock.withLock {
    this.value
  }
}

class PropertyWithRWLock<V>(
  private var value: V,
  private val lock: ReadWriteLock
) : ReadWriteProperty<Any?, V> {

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
    lock.write { this.value = value }
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>) = lock.read {
    this.value
  }
}

fun <V> locked(value: V, lock: Lock = ReentrantLock()) = PropertyWithLock(value, lock)
fun <V> rwLocked(value: V, lock: ReadWriteLock = ReentrantReadWriteLock()) = PropertyWithRWLock(value, lock)