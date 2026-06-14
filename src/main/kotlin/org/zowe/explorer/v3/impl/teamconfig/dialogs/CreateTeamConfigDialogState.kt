/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig.dialogs

import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.profiles.BaseProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.CicsProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.Db2Profile
import org.zowe.explorer.v3.impl.teamconfig.profiles.EbgProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.EndevorLocationProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.EndevorProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.JclCheckProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.MqProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.SshProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.SysviewFormatProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.SysviewProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.TsoProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.ZftpProfile
import org.zowe.explorer.v3.impl.teamconfig.profiles.ZosmfProfile

/**
 * Mutable state holder for [CreateTeamConfigDialog].
 *
 * Aggregates the [configType] selection and one instance of each supported
 * Zowe profile type. The dialog binds its UI controls directly to these objects;
 * when the user confirms, [CreateTeamConfigDialog] reads this state to build and
 * write the JSON config file.
 *
 * The **base** profile is mandatory and always serialized. All other profiles are
 * opt-in via their `shouldCreate` flag.
 */
class CreateTeamConfigDialogState {
  var configType: ConfigType = ConfigType.LOCAL_TEAM
  val baseProfile = BaseProfile()
  val zosmfProfile = ZosmfProfile()
  val tsoProfile = TsoProfile()
  val sshProfile = SshProfile()
  val sysviewProfile = SysviewProfile()
  val sysviewFormatProfile = SysviewFormatProfile()
  val endevorProfile = EndevorProfile()
  val endevorLocationProfile = EndevorLocationProfile()
  val jclCheckProfile = JclCheckProfile()
  val ebgProfile = EbgProfile()
  val zftpProfile = ZftpProfile()
  val cicsProfile = CicsProfile()
  val db2Profile = Db2Profile()
  val mqProfile = MqProfile()
}