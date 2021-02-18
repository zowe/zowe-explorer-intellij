package eu.ibagroup.formainframe.utils

interface QueueExecutor<V, R> {

  fun accept(input: V)

  fun shutdown()

}