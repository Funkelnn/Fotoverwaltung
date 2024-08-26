package de.thm.mni.gruppe8.fotoverwaltung.repositories;

import de.thm.mni.gruppe8.fotoverwaltung.DatabaseManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PhotoRepository {
  public void findAllByUser(String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT * FROM photos WHERE user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();
      JsonArray photos = new JsonArray();

      while (resultSet.next()) {
        JsonObject photo = new JsonObject()
          .put("photo_id", resultSet.getInt("photo_id"))
          .put("title", resultSet.getString("title"))
          .put("filepath", resultSet.getString("filepath"))
          .put("capture_date", resultSet.getDate("capture_date").toString())
          .put("capture_time", resultSet.getTime("capture_time") != null ? resultSet.getTime("capture_time").toString() : null)
          .put("latitude", resultSet.getBigDecimal("latitude") != null ? resultSet.getBigDecimal("latitude").toString() : null)
          .put("longitude", resultSet.getBigDecimal("longitude") != null ? resultSet.getBigDecimal("longitude").toString() : null)
          .put("created_at", resultSet.getTimestamp("created_at").toString())
          .put("updated_at", resultSet.getTimestamp("updated_at").toString());
        photos.add(photo);
      }
      resultHandler.handle(Future.succeededFuture(photos));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void findByIdAndUser(String photoId, String userId, Handler<AsyncResult<JsonObject>> resultHandler) {
    String query = "SELECT * FROM photos WHERE photo_id = ? AND user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(photoId));
      statement.setInt(2, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        JsonObject photo = new JsonObject()
          .put("photo_id", resultSet.getInt("photo_id"))
          .put("user_id", resultSet.getInt("user_id"))
          .put("filepath", resultSet.getString("filepath"))
          .put("title", resultSet.getString("title"))
          .put("capture_date", resultSet.getDate("capture_date").toString())
          .put("capture_time", resultSet.getTime("capture_time") != null ? resultSet.getTime("capture_time").toString() : "")
          .put("latitude", resultSet.getBigDecimal("latitude") != null ? resultSet.getBigDecimal("latitude").toString() : "")
          .put("longitude", resultSet.getBigDecimal("longitude") != null ? resultSet.getBigDecimal("longitude").toString() : "");

        resultHandler.handle(Future.succeededFuture(photo));
      } else {
        resultHandler.handle(Future.succeededFuture(null));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void create(JsonObject photoData, Handler<AsyncResult<Void>> resultHandler) {
    String query = "INSERT INTO photos (user_id, filepath, title, capture_date, capture_time, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {
      statement.setInt(1, photoData.getInteger("user_id"));
      statement.setString(2, photoData.getString("filepath"));
      statement.setString(3, photoData.getString("title"));
      statement.setDate(4, Date.valueOf(photoData.getString("capture_date")));
      statement.setTime(5, photoData.getString("capture_time") != null ? Time.valueOf(photoData.getString("capture_time")) : null);

      BigDecimal latitude = photoData.getString("latitude") != null ? new BigDecimal(photoData.getString("latitude")) : null;
      BigDecimal longitude = photoData.getString("longitude") != null ? new BigDecimal(photoData.getString("longitude")) : null;

      statement.setBigDecimal(6, latitude);
      statement.setBigDecimal(7, longitude);

      statement.executeUpdate();
      resultHandler.handle(Future.succeededFuture());
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void update(String photoId, String userId, JsonObject updateData, Handler<AsyncResult<Void>> resultHandler) {
    StringBuilder query = new StringBuilder("UPDATE photos SET ");
    List<Object> params = new ArrayList<>();

    updateData.forEach(entry -> {
      query.append(entry.getKey()).append(" = ?, ");
      params.add(entry.getValue());
    });

    query.append("updated_at = CURRENT_TIMESTAMP WHERE photo_id = ? AND user_id = ?");
    params.add(Integer.parseInt(photoId));
    params.add(Integer.parseInt(userId));

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query.toString())) {

      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Photo not found or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void delete(String photoId, String userId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "DELETE FROM photos WHERE photo_id = ? AND user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(photoId));
      statement.setInt(2, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Photo not found or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }


  // Tags
  public void findTagsByPhotoId(String photoId, String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT t.tag_id, t.name FROM tags t " +
      "JOIN photo_tags pt ON t.tag_id = pt.tag_id " +
      "JOIN photos p ON p.photo_id = pt.photo_id " +
      "WHERE p.photo_id = ? AND p.user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(photoId));
      statement.setInt(2, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      JsonArray tags = new JsonArray();
      while (resultSet.next()) {
        JsonObject tag = new JsonObject()
          .put("tag_id", resultSet.getInt("tag_id"))
          .put("name", resultSet.getString("name"));
        tags.add(tag);
      }
      resultHandler.handle(Future.succeededFuture(tags));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void addTagToPhoto(String photoId, String tagId, String userId, Handler<AsyncResult<Void>> resultHandler) {
    String checkQuery = "SELECT " +
      "(SELECT COUNT(*) FROM photos WHERE photo_id = ? AND user_id = ?) AS isPhotoOwner, " +
      "(SELECT COUNT(*) FROM tags WHERE tag_id = ? AND user_id = ?) AS isTagOwner";

    String insertQuery = "INSERT INTO photo_tags (photo_id, tag_id) " +
      "SELECT ?, ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM photo_tags WHERE photo_id = ? AND tag_id = ?)";

    try (PreparedStatement checkStatement = DatabaseManager.getConnection().prepareStatement(checkQuery)) {

      checkStatement.setInt(1, Integer.parseInt(photoId));
      checkStatement.setInt(2, Integer.parseInt(userId));
      checkStatement.setInt(3, Integer.parseInt(tagId));
      checkStatement.setInt(4, Integer.parseInt(userId));

      ResultSet resultSet = checkStatement.executeQuery();
      if (resultSet.next() && resultSet.getInt("isPhotoOwner") > 0 && resultSet.getInt("isTagOwner") > 0) {
        try (PreparedStatement insertStatement = DatabaseManager.getConnection().prepareStatement(insertQuery)) {
          insertStatement.setInt(1, Integer.parseInt(photoId));
          insertStatement.setInt(2, Integer.parseInt(tagId));
          insertStatement.setInt(3, Integer.parseInt(photoId));
          insertStatement.setInt(4, Integer.parseInt(tagId));

          int rowsAffected = insertStatement.executeUpdate();
          if (rowsAffected > 0) {
            resultHandler.handle(Future.succeededFuture());
          } else {
            resultHandler.handle(Future.failedFuture("Tag already associated with photo"));
          }
        }
      } else {
        resultHandler.handle(Future.failedFuture("Tag not found, photo not found, or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }


  public void removeTagFromPhoto(String photoId, String tagId, String userId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "DELETE FROM photo_tags " +
      "WHERE photo_id = ? AND tag_id = ? " +
      "AND EXISTS (SELECT 1 FROM photos WHERE photo_id = ? AND user_id = ?) " +
      "AND EXISTS (SELECT 1 FROM tags WHERE tag_id = ? AND user_id = ?)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(photoId));
      statement.setInt(2, Integer.parseInt(tagId));
      statement.setInt(3, Integer.parseInt(photoId));
      statement.setInt(4, Integer.parseInt(userId));
      statement.setInt(5, Integer.parseInt(tagId));
      statement.setInt(6, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Tag not found, photo not found, or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void findAllPhotoTagsByUser(String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT pt.photo_id, pt.tag_id " +
      "FROM photo_tags pt " +
      "JOIN photos p ON pt.photo_id = p.photo_id " +
      "WHERE p.user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      JsonArray photoTags = new JsonArray();
      while (resultSet.next()) {
        JsonObject photoTag = new JsonObject()
          .put("photo_id", resultSet.getInt("photo_id"))
          .put("tag_id", resultSet.getInt("tag_id"));
        photoTags.add(photoTag);
      }
      resultHandler.handle(Future.succeededFuture(photoTags));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

}
