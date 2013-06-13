import play.api.mvc.{RequestHeader, WithFilters}
import play.extras.iteratees.GzipFilter
import play.api.mvc.Results._

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 5/29/13
 * Time: 8:15 PM
 */

object Global extends WithFilters(new GzipFilter()) {

  override def onHandlerNotFound(request: RequestHeader) = NotFound(views.html.notfound(request.path))

  override def onError(request: RequestHeader, ex: Throwable) = InternalServerError(views.html.error(request.path, ex))

}
