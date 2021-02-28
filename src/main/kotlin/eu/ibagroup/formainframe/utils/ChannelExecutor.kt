package eu.ibagroup.formainframe.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean


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

  private suspend fun processChannel(afterCancelled: Boolean) {
    val input = if (afterCancelled) {
      channel.poll()
    } else {
      channel.receive()
    }
    if (input != null) {
      val result = processInput(input)
      execution.receive(result)
    }
  }

  private val cancelled = AtomicBoolean(false)

  private val job = scope.launch {
    while (true) {
      try {
        ensureActive()
        processChannel(false)
        delay(delayDurationInMilliseconds)
      } catch (ignored: CancellationException) {
        cancelled.compareAndSet(false, true)
        break
      } finally {
        if (cancelled.compareAndSet(true, true)) {
          processChannel(true)
        }
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