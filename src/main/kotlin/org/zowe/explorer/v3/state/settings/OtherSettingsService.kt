/*
 * Copyright (c) 2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.v3.state.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService

/**
 * THIS IS THE PREFERRED WAY TO WORK WITH OTHER SETTINGS VALUES
 * Other settings service to work with intermediate state of other settings values.
 * When changes are made and are ready to be saved, will synchronize the state to the storage
 */
@Service
@OptIn(StableStorage::class)
class OtherSettingsService {
  companion object {
    val TOPIC = Topic.create("OtherSettingsEventListener", OtherSettingsEventListener::class.java)

    fun getService(): OtherSettingsService = service()
  }

  /** Modifiable state of the other settings that reflects the changes of the storage */
  private var state: OtherSettingsHolder = OtherSettingsState()
    get() = synchronized(stateLock) { field }
    private set(value) = synchronized(stateLock) { field = value }

  private val eventProducer = OtherSettingsEventProducerImpl()
  private val stateLock = Any()

  init {
    /**
     * The subscription model expects that modifiable state already has the consistent changes against the stable
     * storage. So whenever the basic event, related to the [StorageService], happens, it tries to update
     * the modifiable state without throwing the error notification if the changes are already there.
     * Also, some of the handlers are needed to reflect the initial load of the config state from storage.
     * To spread the updates across components, the respective event is produced with the updates to apply
     */
    subscribe(
      StorageService.OTHER_SETTINGS_TOPIC,
      object : OtherSettingsEventListener {
        /**
         * Reload other settings elements with the changes from the storage
         * @param newSettings the new state to apply
         */
        override fun otherSettingsReloaded(newSettings: OtherSettingsHolder) {
          synchronized(stateLock) {
            state = newSettings
            eventProducer.onOtherSettingsReload(newSettings)
          }
        }

        /**
         * Reflect the changes of other settings element from the storage
         * @param newSettings the new state to apply
         */
        override fun otherSettingsChanged(newSettings: OtherSettingsHolder) {
          synchronized(stateLock) {
            if (state != newSettings) {
              // TODO: log that settings are inconsistent with storage
              updateOtherSettings(newSettings)
            }
          }
        }
      }
    )
  }

  /** Fetch the other settings state */
  fun getOtherSettings(): OtherSettingsHolder {
    synchronized(stateLock) {
      return state
    }
  }

  /**
   * Update the other settings state
   * @param newSettings the new settings to apply
   */
  fun updateOtherSettings(newSettings: OtherSettingsHolder) {
    synchronized(stateLock) {
      state = newSettings
      eventProducer.onOtherSettingsChange(state)
    }
  }

  /**
   * Save other settings state to the storage.
   * The changes are final and irreversible
   */
  fun saveOtherSettings() {
    synchronized(stateLock) {
      StorageService.getService().updateSettingsInStorage(state)
    }
  }

  /** Reload other settings state from the stable storage. Is helpful to refresh the other settings completely */
  fun reloadOtherSettingsFromStorage() {
    synchronized(stateLock) {
      StorageService.getService().reloadOtherSettingsFromStorage()
    }
  }
}
