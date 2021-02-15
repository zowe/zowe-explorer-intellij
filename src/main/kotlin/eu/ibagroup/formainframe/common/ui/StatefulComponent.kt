package eu.ibagroup.formainframe.common.ui

interface StatefulComponent<T : DialogState> {

  var state: T

}