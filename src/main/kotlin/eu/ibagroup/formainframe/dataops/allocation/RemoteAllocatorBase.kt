package eu.ibagroup.formainframe.dataops.allocation

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.FetchCallback
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.r2z.DataAPI

abstract class RemoteAllocatorBase<R : Any> : Allocator<R, RemoteQuery<R>> {

  override fun allocate(query: RemoteQuery<R>, callback: FetchCallback<AllocationStatus>, project: Project?) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, title) {

      override fun run(indicator: ProgressIndicator) {
        try {
          callback.onStart()
          indicator.checkCanceled()
          val response = performAllocationRequest(query)
          callback.onSuccess(response)
        } catch (t : Throwable) {
          callback.onThrowable(t)
        } finally {
          callback.onFinish()
        }

      }

    })
  }

  abstract val title: String

  abstract fun performAllocationRequest(query: RemoteQuery<R>) : AllocationStatus

  override val queryClass: Class<out Query<*>> = RemoteQuery::class.java
}