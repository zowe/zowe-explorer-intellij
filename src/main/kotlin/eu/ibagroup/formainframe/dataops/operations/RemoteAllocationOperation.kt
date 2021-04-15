package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.utils.UNIT_CLASS

interface RemoteAllocationOperation<Request> : RemoteQuery<Request, Unit> {
  override val resultClass
    get() = UNIT_CLASS
}