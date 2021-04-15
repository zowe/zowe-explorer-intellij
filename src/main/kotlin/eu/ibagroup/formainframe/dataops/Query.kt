package eu.ibagroup.formainframe.dataops

interface Query<Request, Result> : Operation<Result> {

  val request: Request

}