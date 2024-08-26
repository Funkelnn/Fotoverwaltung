package de.thm.mni.gruppe8.fotoverwaltung.handlers;

import de.thm.mni.gruppe8.fotoverwaltung.repositories.UserRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.mindrot.jbcrypt.BCrypt;

public class UserHandler {
  private final UserRepository userRepository;

  public UserHandler(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public void getAllUsers(RoutingContext context) {
    if (!isAdmin(context)) {
      context.response().setStatusCode(403).end(new JsonObject().put("error", "Forbidden").encode());
      return;
    }

    userRepository.findAll(res -> {
      if (res.succeeded()) {
        JsonArray users = res.result();
        context.response()
          .putHeader("Content-Type", "application/json")
          .setStatusCode(200)
          .end(users.encodePrettily());
      } else {
        context.response().setStatusCode(500).end("Server error");
      }
    });
  }

  public void getUser(RoutingContext context) {
    String userId = context.pathParam("user_id");
    String sessionUserId = context.session().get("userId");

    // Überprüfe, ob der Benutzer Admin ist oder sich selbst abrufen will
    if (!sessionUserId.equals(userId) && !isAdmin(context)) {
      context.response().setStatusCode(403).end(new JsonObject().put("error", "Forbidden").encode());
      return;
    }

    userRepository.findById(userId, res -> {
      if (res.succeeded()) {
        JsonObject user = res.result();
        if (user != null) {
          context.response()
            .putHeader("Content-Type", "application/json")
            .end(user.encodePrettily());
        } else {
          context.response().setStatusCode(404)
            .end(new JsonObject().put("error", "User not found").encode());
        }
      } else {
        context.response().setStatusCode(500)
          .end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void createUser(RoutingContext context) {
    JsonObject body = context.body().asJsonObject();

    if (body == null) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body").encode());
      return;
    }

    String username = body.getString("username");
    String password = body.getString("password");

    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Username, password must be provided").encode());
      return;
    }

    // Passwort hashen
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

    // Benutzer erstellen
    JsonObject user = new JsonObject()
      .put("username", username)
      .put("password_hash", hashedPassword);

    userRepository.create(user, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(201).end(new JsonObject().put("message", "User created successfully").encode());
      } else {
        String cause = res.cause().getMessage();
        if ("Duplicate username".equals(cause)) {
          context.response().setStatusCode(409).end(new JsonObject().put("error", "Username already exists").encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }

  public void updateUser(RoutingContext context) {
    String userId = context.pathParam("user_id");
    JsonObject body = context.body().asJsonObject();

    if (body == null) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body").encode());
      return;
    }

    String sessionUserId = context.session().get("userId");
    String sessionUserRole = context.session().get("role");

    // Überprüfe, ob der Benutzer Admin ist oder sich selbst aktualisiert
    if (!sessionUserId.equals(userId) && !"admin".equals(sessionUserRole)) {
      context.response().setStatusCode(403).end(new JsonObject().put("error", "Forbidden").encode());
      return;
    }

    String username = body.getString("username");
    String password = body.getString("password");

    if ((username == null || username.isEmpty()) && (password == null || password.isEmpty())) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "No fields to update").encode());
      return;
    }

    JsonObject updateData = new JsonObject();
    if (username != null && !username.isEmpty()) updateData.put("username", username);
    if (password != null && !password.isEmpty()) {
      String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
      updateData.put("password_hash", hashedPassword);
    }

    userRepository.update(userId, updateData, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end();
      } else {
        String cause = res.cause().getMessage();
        if ("Duplicate username".equals(cause)) {
          context.response().setStatusCode(409).end(new JsonObject().put("error", "Username already exists").encode());
        } else if ("User not found".equals(cause)) {
          context.response().setStatusCode(404).end(new JsonObject().put("error", "User not found").encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }

  public void deleteUser(RoutingContext context) {
    String userId = context.pathParam("user_id");
    String sessionUserId = context.session().get("userId");
    String sessionUserRole = context.session().get("role");

    // Überprüfe, ob der Benutzer Admin ist oder sich selbst löscht
    if (!sessionUserId.equals(userId) && !"admin".equals(sessionUserRole)) {
      context.response().setStatusCode(403)
        .end(new JsonObject().put("error", "Forbidden").encode());
      return;
    }

    userRepository.delete(userId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end();
      } else {
        String cause = res.cause().getMessage();
        if ("User not found".equals(cause)) {
          context.response().setStatusCode(404).end(new JsonObject().put("error", "User not found").encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }

  private boolean isAdmin(RoutingContext context) {
    String role = context.session().get("role");
    return "admin".equals(role);
  }
}
