package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath

interface WorkingSet : ExplorerUnit {

  val dsMasks: Collection<DSMask>

  fun addMask(dsMask: DSMask)

  fun removeMask(dsMask: DSMask)

  val ussPaths: Collection<UssPath>

  fun addUssPath(ussPath: UssPath)

  fun removeUssPath(ussPath: UssPath)

}