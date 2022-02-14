/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.utils

import com.intellij.openapi.components.service
import org.zowe.explorer.config.ConfigService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

private val configService = service<ConfigService>()

class ChannelExecutor<V>(
  private val channel: Channel<V>,
  delayDuration: Duration
) : QueueExecutor<V> {

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private val executionChannel = Channel<Unit>(Channel.RENDEZVOUS)
  private val pauseChannel = Channel<Unit>(Channel.RENDEZVOUS)

  private val executionMutex = Mutex()

  private val isOnPause = AtomicBoolean(false)
  private val isCancelled = AtomicBoolean(false)
  private val isFirstReceive = AtomicBoolean(true)

  private val delayDurationInMilliseconds = delayDuration.toMillis()

  private lateinit var execution: (V) -> Unit

  @Synchronized
  override fun launch(execution: (V) -> Unit) {
    val needToNotifyInitialized = !this::execution.isInitialized
    this.execution = execution
    if (needToNotifyInitialized) {
      scope.launch { executionChannel.send(Unit) }
    }
  }

  private suspend fun processInput(input: V) = coroutineScope {
    withContext(NonCancellable) {
      executionMutex.withLock {
        execution(input)
      }
    }
  }

  private suspend fun processChannel(afterCancelled: Boolean) {
    val input = if (afterCancelled) {
      channel.tryReceive().getOrNull()
    } else {
      channel.receive()
    }
    if (configService.isAutoSyncEnabled.get() || isFirstReceive.compareAndSet(true, false)) {
      input?.let { processInput(it) }
    }
  }

  private val job = scope.launch {
    while (true) {
      try {
        ensureActive()
        if (!this@ChannelExecutor::execution.isInitialized) {
          executionChannel.receive()
        }
        if (isOnPause.get()) {
          pauseChannel.receive()
        }
        processChannel(false)
        delay(delayDurationInMilliseconds)
      } catch (ignored: CancellationException) {
        isCancelled.compareAndSet(false, true)
        break
      } finally {
        if (isCancelled.compareAndSet(true, true)) {
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
