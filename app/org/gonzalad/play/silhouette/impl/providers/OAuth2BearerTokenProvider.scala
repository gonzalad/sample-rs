package org.gonzalad.play.silhouette.impl.providers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.crypto.Base64
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.{ Credentials, HTTPLayer }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticatorService.{ ID => _ }
import com.mohiva.play.silhouette.impl.exceptions.{ IdentityNotFoundException, ProfileRetrievalException }
import com.mohiva.play.silhouette.impl.providers.BasicAuthProvider._
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.impl.providers.oauth2.Auth0Provider.{ ID => _, _ }
import models.{ AuthToken, User }
import play.api.http.HeaderNames
import play.api.libs.json.{ JsObject, JsValue }
import play.api.mvc.{ Request, RequestHeader }

import scala.concurrent.{ ExecutionContext, Future }

case class OAuth2BearerToken(
  id: String,
  //loginInfo: LoginInfo,
  username: Option[String] = None,
  scope: Option[String] = None,
  clientId: Option[String] = None,
  tokenType: Option[String] = None,
  exp: Option[Int] = None,
  iat: Option[Int] = None,
  nbf: Option[Int] = None,
  sub: Option[String] = None,
  aud: Option[String] = None,
  iss: Option[String] = None,
  jti: Option[String] = None
) {
}

/**
 * Implements bearer oauth2 token with remote validation.
 *
 * @author agonzalez
 */
class OAuth2BearerTokenProvider @Inject() (
  protected val identityService: OAuth2BearerIntrospectIdentityService
)(implicit val executionContext: ExecutionContext)
  extends RequestProvider with Logger {

  /**
   * Gets the provider ID.
   *
   * @return The provider ID.
   */
  override def id = ID

  /**
   * Authenticates an identity based on credentials sent in a request.
   *
   * @param request The request.
   * @tparam B The type of the body.
   * @return Some login info on successful authentication or None if the authentication was unsuccessful.
   */
  override def authenticate[B](request: Request[B]): Future[Option[LoginInfo]] = {
    getBearerToken(request) match {
      case Some(bearerToken) =>
        def loginInfo: LoginInfo = LoginInfo(ID, bearerToken)
        identityService.retrieve(loginInfo).flatMap {
          case Some(authToken) =>
            Future.successful(loginInfo)
          case _ =>
            //invalid accessToken in request Header
            Future.successful(None)
        }
        Future.successful(Some(loginInfo))
      case None => Future.successful(None) //Future.failed(new IdentityNotFoundException("No oauth bearer token found in current request"))
    }
  }

  /**
   * Retrieves bearer token from Authorization HTTP Header.
   *
   * @param request Contains the colon-separated name-value pairs in clear-text string format
   * @return the oauth2 bearer token
   */
  def getBearerToken(request: RequestHeader): Option[String] = {
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(header) if header.startsWith("Bearer ") =>
        header.replace("Bearer ", "") match {
          case bearerToken if bearerToken.length > 0 => Some(bearerToken)
          case _ => None
        }
      case _ => None
    }
  }
}

/**
 * The companion object.
 */
object OAuth2BearerTokenProvider {

  /**
   * The provider constants.
   */
  val ID = "oauth2-bearer-token"
}
