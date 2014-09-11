import play.api.mvc.{RequestHeader, WithFilters}
import play.filters.gzip.GzipFilter
import play.api.mvc.Results._

import scala.concurrent.Future

/**
 * web
 * User: markbe
 * Date: 9/11/14
 * Time: 2:04 PM
 */

object Global extends WithFilters(new GzipFilter(shouldGzip = (request, response) =>
	response.headers.get("Content-Type").exists(c => c.startsWith("text/html") || c.startsWith("application/json")))) {

	override def onHandlerNotFound(request: RequestHeader) = Future.successful(NotFound(views.html.notfound(request.path)))

	override def onError(request: RequestHeader, ex: Throwable) = Future.successful(InternalServerError(views.html.error(request.path, ex)))

}
