package eu.ibagroup.formainframe.dataops

import eu.ibagroup.formainframe.utils.UNIT_CLASS

interface UnitOperation : Operation<Unit> {

  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS

}