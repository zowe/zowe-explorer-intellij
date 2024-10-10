/*
 * Copyright (c) 2020-2024 IBA Group.
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

package org.zowe.explorer.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.migration.performOldConfigStorageMigration
import org.zowe.explorer.v3.state.settings.OtherSettingsService
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService

/**
 * Activity to prepare config services before the project startup.
 * Will load [StorageService] and [ConfigCacheService],
 * together with the previously saved configs from the related XML file.
 *
 * DEPRECATED: (The next functionality will be deleted in the future)
 * Will load [ConfigService] together with the previously saved configs from the related XML file
 */
class ConfigStartupActivity : ProjectActivity {
  @OptIn(StableStorage::class)
  override suspend fun execute(project: Project) {
    ConfigService.getService().apply {
      registerAllConfigClasses()
    }
    // The next line is needed to initialize subscriptions to storage service events.
    // WARNING: it is necessary to initialize the cache service together with the storage service!
    ConfigCacheService.getService()
    // The next line is needed to initialize subscriptions to storage service events.
    // WARNING: it is necessary to initialize the other settings service together with the storage service!
    OtherSettingsService.getService()
    StorageService.getService().registerConfigTypes()

    performOldConfigStorageMigration()
  }
}
