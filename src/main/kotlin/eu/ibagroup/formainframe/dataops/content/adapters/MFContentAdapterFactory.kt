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

package eu.ibagroup.formainframe.dataops.content.adapters

import eu.ibagroup.formainframe.dataops.DataOpsComponentFactory

/**
 * Base factory for registering content adapters in Intellij IoC container.
 */
interface MFContentAdapterFactory : DataOpsComponentFactory<MFContentAdapter>
