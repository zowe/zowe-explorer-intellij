package eu.ibagroup.formainframe.dataops

interface DataOpsComponentFactory<Component> {

  fun buildComponent(dataOpsManager: DataOpsManager): Component

}