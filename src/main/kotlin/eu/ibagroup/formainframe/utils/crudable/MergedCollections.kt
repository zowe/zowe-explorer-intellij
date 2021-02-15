package eu.ibagroup.formainframe.utils.crudable

data class MergedCollections<E>(
  val toAdd: Collection<E>,
  val toUpdate: Collection<E>,
  val toDelete: Collection<E>
)