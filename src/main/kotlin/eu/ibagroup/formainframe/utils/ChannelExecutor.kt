package eu.ibagroup.formainframe.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Duration


interface Execution<V, R> {

  @Throws(Throwable::class)
  fun execute(input: V): R

  fun receive(result: R)

  fun onThrowable(input: V, throwable: Throwable)

}

class ChannelExecutor<V, R>(
  private val channel: Channel<V>,
  delayDuration: Duration,
  private val execution: Execution<V, R>
) : QueueExecutor<V, R> {

  private val delayDurationInMilliseconds = delayDuration.toMillis()

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private suspend fun processInput(input: V): R = coroutineScope {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
      execution.onThrowable(input, throwable)
    }
    withContext(NonCancellable + exceptionHandler) {
      execution.execute(input)
    }
  }

  private suspend fun processChannel(scope: CoroutineScope) {
    withContext(NonCancellable) {
      val input = if (scope.isActive) {
        channel.receive()
      } else {
        channel.poll()
      }
      if (input != null) {
        val result = processInput(input)
        execution.receive(result)
      }
    }
  }

  private val job = scope.launch {
    while (true) {
      try {
        processChannel(scope)
        delay(delayDurationInMilliseconds)
      } catch (ignored: CancellationException) {
        break
      } finally {
        processChannel(scope)
      }
    }
  }

  override fun accept(input: V) {
    scope.launch {
      channel.send(input)
    }
  }

  override fun shutdown() {
    runBlocking {
      job.cancelAndJoin()
    }
  }

}