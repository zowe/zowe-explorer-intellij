package eu.ibagroup.formainframe.utils.crudable

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

class SimpleReadWriteAdapter : ReadWriteLocksAdapter() {

  private val lock = ReentrantReadWriteLock()

  override fun <E : Any> getReadLock(rowClass: Class<out E>): Lock? {
    return lock.readLock()
  }

  override fun <E : Any> getWriteLock(rowClass: Class<out E>): Lock? {
    return lock.writeLock()
  }

}