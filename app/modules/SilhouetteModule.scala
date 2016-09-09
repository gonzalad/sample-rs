package modules

import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus, Silhouette, SilhouetteProvider }
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.util._
import models.daos._
import models.services.{ UserService, UserServiceImpl }
import net.codingwell.scalaguice.ScalaModule
import org.gonzalad.play.silhouette.impl.providers.OAuth2BearerIntrospectIdentityService
import org.gonzalad.play.silhouette.impl.providers.OAuth2BearerTokenProvider
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import security.DefaultEnv

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[UserService].to[UserServiceImpl]
    bind[UserDAO].to[UserDAOImpl]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  /**
   * Provides the Silhouette environment.
   *
   * @param identityService          The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus             The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    identityService: OAuth2BearerIntrospectIdentityService,
    authenticatorService: AuthenticatorService[DummyAuthenticator],
    oAuth2BearerTokenProvider: OAuth2BearerTokenProvider,
    eventBus: EventBus
  ): Environment[DefaultEnv] = {

    Environment[DefaultEnv](
      identityService,
      authenticatorService,
      Seq(oAuth2BearerTokenProvider),
      eventBus
    )
  }

  @Provides
  def providesDummyAuthenticator(): AuthenticatorService[DummyAuthenticator] = {
    new DummyAuthenticatorService
  }

  @Provides
  def providesOAuth2BearerTokenProvider(oAuth2BearerIntrospectIdentityService: OAuth2BearerIntrospectIdentityService): OAuth2BearerTokenProvider = {
    new OAuth2BearerTokenProvider(oAuth2BearerIntrospectIdentityService)
  }

  @Provides
  def providesOAuth2BearerIntrospectIdentityService(configuration: Configuration, httpLayer: HTTPLayer): OAuth2BearerIntrospectIdentityService = {
    new OAuth2BearerIntrospectIdentityService(
      configuration.getString("silhouette.oidc.clientId").get,
      configuration.getString("silhouette.oidc.clientSecret").get,
      configuration.getString("silhouette.oidc.tokenIntrospectUri").get, httpLayer
    )
  }

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)
}
