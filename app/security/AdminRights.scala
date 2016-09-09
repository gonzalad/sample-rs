package security

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import models.{ AuthToken, User }
import play.api.mvc.Request

import scala.concurrent.Future

/**
 * @author agonzalez
 */
object AdminRights extends Authorization[AuthToken, DummyAuthenticator] {

  override def isAuthorized[B](identity: AuthToken, authenticator: DummyAuthenticator)(implicit request: Request[B]): Future[Boolean] = {
    println("isAuthorized called")
    Future.successful(identity.roles.contains("admin"))
  }
}
