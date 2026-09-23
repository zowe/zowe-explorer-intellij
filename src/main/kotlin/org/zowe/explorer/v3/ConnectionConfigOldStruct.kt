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

package org.zowe.explorer.v3

import org.zowe.explorer.utils.crudable.annotations.Column
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.kotlinsdk.annotations.ZVersion

// TODO: tags
// TODO: clarify abstraction
/**
 * Connection config based class. Could be used as a basic connection config representation or as a base for other
 * connection config type
 * @param name the name of the connection config
 * @param url the URL for the connection config
 * @param isAllowSelfSigned to indicate whether it is allowed to use self-signed certificates during a connection
 * @param zVersion the version of the z/OS being used for the connection
 * @param owner the actual USS user related to the USS user provided in the connection to work with in the USS part
 * @param isAllowCleartext to indicate whether the connection is explicitly allowed to use unencrypted HTTP transport
 */
open class ConnectionConfigOldStruct(
  uuid: String = EMPTY_ID,
  @Column var name: String = "",
  @Column var url: String = "",
  @Column var isAllowSelfSigned: Boolean = true,
  @Column var zVersion: ZVersion = ZVersion.ZOS_2_3,
  @Column var owner: String = "",
  @Column var isAllowCleartext: Boolean = false
) : Config(uuid, configType = ConfigType.HTTP_CONNECTION_CONFIG_V1)
