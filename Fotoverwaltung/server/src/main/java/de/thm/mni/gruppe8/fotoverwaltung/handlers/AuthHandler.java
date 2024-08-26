package de.thm.mni.gruppe8.fotoverwaltung.handlers;

import de.thm.mni.gruppe8.fotoverwaltung.repositories.AuthRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class AuthHandler {
  private final AuthRepository authRepository;

  public AuthHandler(AuthRepository authRepository) {
    this.authRepository = authRepository;
  }

  public void login(RoutingContext context) {
    JsonObject body = context.body().asJsonObject();

    if (body == null) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body").encode());
      return;
    }

    String username = body.getString("username");
    String password = body.getString("password");

    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Username and password must be provided").encode());
      return;
    }

    authRepository.authenticate(username, password, res -> {
      if (res.succeeded()) {
        JsonObject user = res.result();
        Session session = context.session();
        session.put("userId", user.getString("user_id"));
        session.put("username", user.getString("username"));
        session.put("role", user.getString("role"));

        context.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject()
            .put("message", "Login successful")
            .put("user_id", user.getInteger("user_id"))
            .put("username", user.getString("username"))
            .put("role", user.getString("role"))
            .encode());
      } else {
        String cause = res.cause().getMessage();
        if ("User not found".equals(cause) || "Unauthorized".equals(cause)) {
          context.response().setStatusCode(401).end(new JsonObject().put("error", "Invalid username or password").encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }

  public void logout(RoutingContext context) {
    Session session = context.session();
    session.destroy();
    context.response().setStatusCode(204).end();
  }
}
