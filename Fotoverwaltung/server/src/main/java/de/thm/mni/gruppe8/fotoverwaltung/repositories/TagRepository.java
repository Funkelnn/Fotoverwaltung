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

public class TagRepository {

  public void findAllByUser(String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT * FROM tags WHERE user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      JsonArray tags = new JsonArray();
      while (resultSet.next()) {
        JsonObject tag = new JsonObject()
          .put("tag_id", resultSet.getInt("tag_id"))
          .put("name", resultSet.getString("name"))
          .put("created_at", resultSet.getTimestamp("created_at").toString());
        tags.add(tag);
      }
      resultHandler.handle(Future.succeededFuture(tags));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void findByUser(String userId, String name, Handler<AsyncResult<List<String>>> resultHandler) {
    String query = "SELECT name FROM tags WHERE user_id = ? AND name = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));
      statement.setString(2, name);

      ResultSet resultSet = statement.executeQuery();
      List<String> tags = new ArrayList<>();

      while (resultSet.next()) {
        tags.add(resultSet.getString("name"));
      }

      resultHandler.handle(Future.succeededFuture(tags));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }


  public void create(JsonObject tagData, Handler<AsyncResult<Void>> resultHandler) {
    String query = "INSERT INTO tags (user_id, name) VALUES (?, ?)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(tagData.getString("user_id")));
      statement.setString(2, tagData.getString("name"));

      statement.executeUpdate();
      resultHandler.handle(Future.succeededFuture());
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void delete(String tagId, String userId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "DELETE FROM tags WHERE tag_id = ? AND user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(tagId));
      statement.setInt(2, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Tag not found or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }
}
