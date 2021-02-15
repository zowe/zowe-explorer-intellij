package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath

interface WorkingSet : ExplorerUnit {

  val dsMasks: Collection<DSMask>

  val ussPaths: Collection<UssPath>

}