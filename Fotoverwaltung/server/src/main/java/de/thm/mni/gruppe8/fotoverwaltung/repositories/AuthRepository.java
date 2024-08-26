package de.thm.mni.gruppe8.fotoverwaltung.repositories;

import de.thm.mni.gruppe8.fotoverwaltung.DatabaseManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthRepository {

  public void authenticate(String username, String password, Handler<AsyncResult<JsonObject>> resultHandler) {

    String query = "SELECT user_id, username, password_hash, role FROM users WHERE username = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {
      statement.setString(1, username);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String storedPasswordHash = resultSet.getString("password_hash");
          if (BCrypt.checkpw(password, storedPasswordHash)) {
            JsonObject user = new JsonObject()
              .put("user_id", resultSet.getInt("user_id"))
              .put("username", resultSet.getString("username"))
              .put("role", resultSet.getString("role"));
            resultHandler.handle(Future.succeededFuture(user));
          } else {
            resultHandler.handle(Future.failedFuture("Unauthorized"));
          }
        } else {
          resultHandler.handle(Future.failedFuture("User not found"));
        }
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }
}
