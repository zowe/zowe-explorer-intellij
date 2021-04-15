package eu.ibagroup.formainframe.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class ChannelExecutor<V>(
  private val channel: Channel<V>,
  delayDuration: Duration
) : QueueExecutor<V> {

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private val initChannel = Channel<Unit>(Channel.RENDEZVOUS)

  private lateinit var execution: (V) -> Unit

  @Synchronized
  override fun launch(execution: (V) -> Unit) {
    val needToNotifyInitialized = !this::execution.isInitialized
    this.execution = execution
    if (needToNotifyInitialized) {
      scope.launch { initChannel.send(Unit) }
    }
  }

  private val delayDurationInMilliseconds = delayDuration.toMillis()

  private val executionMutex = Mutex()

  private suspend fun processInput(input: V) = coroutineScope {
    withContext(NonCancellable) {
      executionMutex.withLock {
        execution(input)
      }
    }
  }

  private suspend fun processChannel(afterCancelled: Boolean) {
    val input = if (afterCancelled) {
      channel.poll()
    } else {
      channel.receive()
    }
    input?.let { processInput(it) }
  }

  private val cancelled = AtomicBoolean(false)

  private val job = scope.launch {
    while (true) {
      try {
        ensureActive()
        if (!this@ChannelExecutor::execution.isInitialized) {
          initChannel.receive()
        }
        if (isOnPause.get()) {
          pauseChannel.receive()
        }
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

  private val isOnPause = AtomicBoolean(false)
  private val pauseChannel = Channel<Unit>(Channel.RENDEZVOUS)

  override fun pause() {
    isOnPause.set(true)
  }

  override fun resume() {
    if (isOnPause.compareAndSet(true, false)) {
      scope.launch {
        pauseChannel.send(Unit)
      }
    }
  }

}