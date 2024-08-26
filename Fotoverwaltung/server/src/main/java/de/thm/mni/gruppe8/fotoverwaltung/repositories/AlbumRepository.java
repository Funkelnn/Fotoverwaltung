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

public class AlbumRepository {

  public void create(JsonObject albumData, Handler<AsyncResult<Void>> resultHandler) {
    String query = "INSERT INTO albums (user_id, title) VALUES (?, ?)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumData.getString("user_id")));
      statement.setString(2, albumData.getString("title"));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Failed to create album"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void findAllByUser(String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT * FROM albums WHERE user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      JsonArray albums = new JsonArray();
      while (resultSet.next()) {
        JsonObject album = new JsonObject()
          .put("album_id", resultSet.getInt("album_id"))
          .put("user_id", resultSet.getInt("user_id"))
          .put("title", resultSet.getString("title"))
          .put("created_at", resultSet.getTimestamp("created_at").toString())
          .put("updated_at", resultSet.getTimestamp("updated_at").toString());
        albums.add(album);
      }
      resultHandler.handle(Future.succeededFuture(albums));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void findByIdAndUser(String albumId, String userId, Handler<AsyncResult<JsonObject>> resultHandler) {
    String query = "SELECT * FROM albums WHERE album_id = ? AND user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumId));
      statement.setInt(2, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        JsonObject album = new JsonObject()
          .put("album_id", resultSet.getInt("album_id"))
          .put("user_id", resultSet.getInt("user_id"))
          .put("title", resultSet.getString("title"))
          .put("created_at", resultSet.getTimestamp("created_at").toString())
          .put("updated_at", resultSet.getTimestamp("updated_at").toString());
        resultHandler.handle(Future.succeededFuture(album));
      } else {
        resultHandler.handle(Future.succeededFuture(null));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void update(String albumId, String userId, JsonObject updateData, Handler<AsyncResult<Void>> resultHandler) {
    String query = "UPDATE albums SET title = ?, updated_at = CURRENT_TIMESTAMP WHERE album_id = ? AND user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setString(1, updateData.getString("title"));
      statement.setInt(2, Integer.parseInt(albumId));
      statement.setInt(3, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Failed to update album or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void delete(String albumId, String userId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "DELETE FROM albums WHERE album_id = ? AND user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumId));
      statement.setInt(2, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Album not found"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  // Photos in albums

  public void findPhotosInAlbum(String albumId, String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT p.* FROM photos p JOIN album_photo ap ON p.photo_id = ap.photo_id WHERE ap.album_id = ? AND p.user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumId));
      statement.setInt(2, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      JsonArray photos = new JsonArray();
      while (resultSet.next()) {
        JsonObject photo = new JsonObject()
          .put("photo_id", resultSet.getInt("photo_id"))
          .put("user_id", resultSet.getInt("user_id"))
          .put("filepath", resultSet.getString("filepath"))
          .put("title", resultSet.getString("title"))
          .put("capture_date", resultSet.getDate("capture_date").toString())
          .put("capture_time", resultSet.getTime("capture_time") != null ? resultSet.getTime("capture_time").toString() : null)
          .put("latitude", resultSet.getBigDecimal("latitude") != null ? resultSet.getBigDecimal("latitude").toString() : null)
          .put("longitude", resultSet.getBigDecimal("longitude") != null ? resultSet.getBigDecimal("longitude").toString() : null);
        photos.add(photo);
      }
      resultHandler.handle(Future.succeededFuture(photos));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void addPhotoToAlbum(String albumId, String userId, String photoId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "INSERT INTO album_photo (album_id, photo_id) " +
      "SELECT a.album_id, p.photo_id " +
      "FROM albums a, photos p " +
      "WHERE a.album_id = ? AND a.user_id = ? AND p.photo_id = ? AND p.user_id = ? " +
      "AND NOT EXISTS (SELECT 1 FROM album_photo ap WHERE ap.album_id = a.album_id AND ap.photo_id = p.photo_id)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumId));
      statement.setInt(2, Integer.parseInt(userId));
      statement.setInt(3, Integer.parseInt(photoId));
      statement.setInt(4, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Failed to add photo to album. Possible reasons: album or photo not found, or photo already in album."));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void removePhotoFromAlbum(String albumId, String userId, String photoId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "DELETE FROM album_photo " +
      "WHERE album_id = ? AND photo_id = ? " +
      "AND EXISTS (SELECT 1 FROM albums WHERE album_id = ? AND user_id = ?)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumId));
      statement.setInt(2, Integer.parseInt(photoId));
      statement.setInt(3, Integer.parseInt(albumId));
      statement.setInt(4, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Failed to remove photo from album. Possible reasons: album not found or photo not in album."));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  // Tags
  public void findTagsByAlbumId(String albumId, String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT t.tag_id, t.name FROM tags t " +
      "JOIN album_tags at ON t.tag_id = at.tag_id " +
      "JOIN albums a ON a.album_id = at.album_id " +
      "WHERE a.album_id = ? AND a.user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumId));
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


  public void addTagToAlbum(String albumId, String tagId, String userId, Handler<AsyncResult<Void>> resultHandler) {
    String checkQuery = "SELECT " +
      "(SELECT COUNT(*) FROM albums WHERE album_id = ? AND user_id = ?) AS isAlbumOwner, " +
      "(SELECT COUNT(*) FROM tags WHERE tag_id = ? AND user_id = ?) AS isTagOwner";

    String insertQuery = "INSERT INTO album_tags (album_id, tag_id) " +
      "SELECT ?, ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM album_tags WHERE album_id = ? AND tag_id = ?)";

    try (PreparedStatement checkStatement = DatabaseManager.getConnection().prepareStatement(checkQuery)) {

      checkStatement.setInt(1, Integer.parseInt(albumId));
      checkStatement.setInt(2, Integer.parseInt(userId));
      checkStatement.setInt(3, Integer.parseInt(tagId));
      checkStatement.setInt(4, Integer.parseInt(userId));

      ResultSet resultSet = checkStatement.executeQuery();
      if (resultSet.next() && resultSet.getInt("isAlbumOwner") > 0 && resultSet.getInt("isTagOwner") > 0) {
        try (PreparedStatement insertStatement = DatabaseManager.getConnection().prepareStatement(insertQuery)) {
          insertStatement.setInt(1, Integer.parseInt(albumId));
          insertStatement.setInt(2, Integer.parseInt(tagId));
          insertStatement.setInt(3, Integer.parseInt(albumId));
          insertStatement.setInt(4, Integer.parseInt(tagId));

          int rowsAffected = insertStatement.executeUpdate();
          if (rowsAffected > 0) {
            resultHandler.handle(Future.succeededFuture());
          } else {
            resultHandler.handle(Future.failedFuture("Tag already associated with album"));
          }
        }
      } else {
        resultHandler.handle(Future.failedFuture("Tag not found, album not found, or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void removeTagFromAlbum(String albumId, String tagId, String userId, Handler<AsyncResult<Void>> resultHandler) {
    String query = "DELETE FROM album_tags " +
      "WHERE album_id = ? AND tag_id = ? " +
      "AND EXISTS (SELECT 1 FROM albums WHERE album_id = ? AND user_id = ?) " +
      "AND EXISTS (SELECT 1 FROM tags WHERE tag_id = ? AND user_id = ?)";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(albumId));
      statement.setInt(2, Integer.parseInt(tagId));
      statement.setInt(3, Integer.parseInt(albumId));
      statement.setInt(4, Integer.parseInt(userId));
      statement.setInt(5, Integer.parseInt(tagId));
      statement.setInt(6, Integer.parseInt(userId));

      int rowsAffected = statement.executeUpdate();
      if (rowsAffected > 0) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture("Tag not found, album not found, or access denied"));
      }
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  public void findAllAlbumTagsByUser(String userId, Handler<AsyncResult<JsonArray>> resultHandler) {
    String query = "SELECT at.album_id, at.tag_id " +
      "FROM album_tags at " +
      "JOIN albums a ON at.album_id = a.album_id " +
      "WHERE a.user_id = ?";

    try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {

      statement.setInt(1, Integer.parseInt(userId));
      ResultSet resultSet = statement.executeQuery();

      JsonArray albumTags = new JsonArray();
      while (resultSet.next()) {
        JsonObject albumTag = new JsonObject()
          .put("album_id", resultSet.getInt("album_id"))
          .put("tag_id", resultSet.getInt("tag_id"));
        albumTags.add(albumTag);
      }
      resultHandler.handle(Future.succeededFuture(albumTags));
    } catch (SQLException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

}
