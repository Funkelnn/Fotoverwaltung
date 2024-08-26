package de.thm.mni.gruppe8.fotoverwaltung.repositories;

import de.thm.mni.gruppe8.fotoverwaltung.DatabaseManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
  public void findAll(Handler<AsyncResult<JsonArray>> resultHandler) {

    String query = "SELECT user_id, username, role, created_at FROM users";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {
      ResultSet resultSet = statement.executeQuery();
      JsonArray users = new JsonArray();
      while (resultSet.next()) {
        JsonObject user = new JsonObject()
          .put("user_id", resultSet.getInt("user_id"))
          .put("username", resultSet.getString("username"))
          .put("role", resultSet.getString("role"))
          .put("created_at", resultSet.getTimestamp("created_at").toString());
        users.add(user);
      }
      resultHandler.handle(Future.succeededFuture(users));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void findById(String userId, Handler<AsyncResult<JsonObject>> resultHandler) {
    String query = "SELECT user_id, username, role, created_at FROM users WHERE user_id = ?";
    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();
      if (resultSet.next()) {
        JsonObject user = new JsonObject()
          .put("user_id", resultSet.getInt("user_id"))
          .put("username", resultSet.getString("username"))
          .put("role", resultSet.getString("role"))
          .put("created_at", resultSet.getTimestamp("created_at").toString());
        resultHandler.handle(Future.succeededFuture(user));
      } else {
        resultHandler.handle(Future.succeededFuture(null));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void create(JsonObject user, Handler<AsyncResult<Void>> resultHandler) {
    String query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setString(1, user.getString("username"));
      statement.setString(2, user.getString("password_hash"));

      statement.executeUpdate();
      resultHandler.handle(Future.succeededFuture());
    } catch (SQLException e) {
      if (e.getSQLState().equals("23000")) { // SQLState 23000 = Unique constraint violation (z.B. Duplicate username)
        resultHandler.handle(Future.failedFuture("Duplicate username"));
      } else {
        resultHandler.handle(Future.failedFuture(e));
      }
    }
  }

  public void update(String userId, JsonObject updateData, Handler<AsyncResult<Void>> resultHandler) {
    StringBuilder query = new StringBuilder("UPDATE users SET ");
    List<Object> params = new ArrayList<>();

    updateData.forEach(entry -> {
      query.append(entry.getKey()).append(" = ?, ");
      params.add(entry.getValue());
    });

    query.append("updated_at = CURRENT_TIMESTAMP");
    query.append(" WHERE user_id = ?");

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query.toString())) {
      int index = 1;
      for (Object param : params) {
        statement.setObject(index++, param);
      }
      statement.setInt(index, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("User not found"));
      }
    } catch (SQLException e) {
      if (e.getSQLState().equals("23000")) { // Unique constraint violation (z.B. Duplicate username)
        resultHandler.handle(Future.failedFuture("Duplicate username"));
      } else {
        resultHandler.handle(Future.failedFuture(e));
      }
    }
  }

  public void delete(String userId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "DELETE FROM users WHERE user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("User not found"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }
}
