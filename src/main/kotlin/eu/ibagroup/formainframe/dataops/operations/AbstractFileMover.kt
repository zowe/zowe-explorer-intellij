package eu.ibagroup.formainframe.dataops.operations

abstract class AbstractFileMover : OperationRunner<MoveCopyOperation, Unit> {

  override val operationClass = MoveCopyOperation::class.java

  override val resultClass = Unit::class.java

}