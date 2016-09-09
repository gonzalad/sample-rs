package security

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import models.{ AuthToken, User }

/**
 * @author agonzalez
 */
trait DefaultEnv extends Env {
  type I = AuthToken
  type A = DummyAuthenticator
}