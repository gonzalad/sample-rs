package org.gonzalad.play.silhouette.impl.providers

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.{ IdentityNotFoundException, ProfileRetrievalException }
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.impl.providers.oauth2.Auth0Provider._
import models.AuthToken
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, JsValue }
import play.api.libs.ws.WSAuthScheme

import scala.concurrent.{ ExecutionContext, Future }

/**
 * @author agonzalez
 */
class OAuth2BearerIntrospectIdentityService(
  protected val clientId: String,
  protected val clientSecret: String,
  protected val tokenIntrospectUri: String,
  protected val httpLayer: HTTPLayer
)(implicit val executionContext: ExecutionContext)
  extends IdentityService[AuthToken] {

  override def retrieve(loginInfo: LoginInfo): Future[Option[AuthToken]] = {
    val authInfo: OAuth2Info = new OAuth2Info(accessToken = loginInfo.providerKey, tokenType = Some("Bearer"))
    buildAuthenticator(authInfo).flatMap {
      case bearerToken =>
        Future.successful(Some(buildIdentity(bearerToken)))
    }
  }

  protected def buildIdentity(token: OAuth2BearerToken): AuthToken = {
    def scopes = if (token.scope.isDefined) token.scope.get.split(" ").toList else List()
    def expSeconds: Int = token.exp.getOrElse(DateTime.now().plusSeconds(20).getMillis.toInt / 1000)
    AuthToken(id = token.id, roles = scopes, userID = token.username, expiry = new DateTime((expSeconds.toLong) * 1000L))
  }

  /**
   * Builds the login info from Json.
   *
   * @param authInfo accessToken
   * @return The login info on success, otherwise a failure.
   */
  private def buildAuthenticator(authInfo: OAuth2Info): Future[OAuth2BearerToken] = {
    httpLayer.url(tokenIntrospectUri)
      .withAuth(clientId, clientSecret, WSAuthScheme.BASIC)
      .withHeaders("Authorization" -> ("Bearer " + authInfo.accessToken), ("Accept" -> "application/json, application/*+json"), ("Content-Type", "application/x-www-form-urlencoded"))
      .post(Map("token" -> Seq(authInfo.accessToken)))
      .flatMap { response =>
        val json = response.json
        (json \ "error").asOpt[JsObject] match {
          case Some(error) =>
            val errorCode = (error \ "code").as[Int]
            val errorMsg = (error \ "message").as[String]
            //Future.failed(new ProfileRetrievalException(SpecifiedProfileError.format(OAuth2BearerTokenProvider.ID, errorCode, errorMsg)))
            throw new ProfileRetrievalException(SpecifiedProfileError.format(OAuth2BearerTokenProvider.ID, errorCode, errorMsg))
          case _ => parseResponse(json, authInfo)
        }
      }
  }

  /**
   * Parse OAuth2 Intropection Response
   *
   * see https://tools.ietf.org/html/rfc7662#page-6
   *
   * @param json
   * @param authInfo
   * @return
   */
  private def parseResponse(json: JsValue, authInfo: OAuth2Info): Future[OAuth2BearerToken] = {
    val active = (json \ "active").as[Boolean]
    val scope = (json \ "scope").asOpt[String]
    val username = (json \ "username").asOpt[String]
    val clientId = (json \ "client_id").asOpt[String]
    val tokenType = (json \ "token_type").asOpt[String]
    val exp = (json \ "exp").asOpt[Int]
    val iat = (json \ "iat").asOpt[Int]
    val nbf = (json \ "nbf").asOpt[Int]
    val sub = (json \ "sub").asOpt[String]
    val aud = (json \ "aud").asOpt[String]
    val iss = (json \ "iss").asOpt[String]
    val jti = (json \ "jti").asOpt[String]

    active match {
      case true =>
        Future.successful(OAuth2BearerToken(
          id = authInfo.accessToken,
          scope = scope,
          username = username,
          clientId = clientId,
          tokenType = tokenType,
          exp = exp,
          iat = iat,
          nbf = nbf,
          sub = sub,
          aud = aud,
          iss = iss,
          jti = jti
        ))
      case _ =>
        throw new IdentityNotFoundException("invalid AT (active status false)")
    }
  }
}
