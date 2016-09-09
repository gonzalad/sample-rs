package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import play.api.i18n.MessagesApi
import play.api.mvc._
import security.{ AdminRights, DefaultEnv }

/**
 * @author agonzalez
 */
class SecuredController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[DefaultEnv]
)
  extends Controller {

  def unsecured() = Action {
    Ok("Hello")
  }

  def secured = silhouette.SecuredAction(AdminRights) { implicit request =>
    Ok("secured microservice called")
  }
}
