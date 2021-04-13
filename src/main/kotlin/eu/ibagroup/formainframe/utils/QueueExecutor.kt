package eu.ibagroup.formainframe.utils

interface QueueExecutor<V> {

  fun launch(execution: (V) -> Unit)

  fun accept(input: V)

  fun shutdown()

  fun pause()

  fun resume()

}