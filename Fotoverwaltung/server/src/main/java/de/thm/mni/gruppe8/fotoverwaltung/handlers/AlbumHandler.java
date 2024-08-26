package de.thm.mni.gruppe8.fotoverwaltung.handlers;

import de.thm.mni.gruppe8.fotoverwaltung.repositories.AlbumRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AlbumHandler {
  private final AlbumRepository albumRepository;

  public AlbumHandler(AlbumRepository albumRepository) {
    this.albumRepository = albumRepository;
  }

  public void createAlbum(RoutingContext context) {
    String userId = context.session().get("userId");
    JsonObject body = context.body().asJsonObject();

    if (body == null || !body.containsKey("title")) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body or missing title").encode());
      return;
    }

    String title = body.getString("title");

    JsonObject albumData = new JsonObject()
      .put("user_id", userId)
      .put("title", title);

    albumRepository.create(albumData, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(201).end(new JsonObject().put("message", "Album created successfully").encode());
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void getAllAlbums(RoutingContext context) {
    String userId = context.session().get("userId");

    albumRepository.findAllByUser(userId, res -> {
      if (res.succeeded()) {
        context.response()
          .putHeader("Content-Type", "application/json")
          .end(res.result().encodePrettily());
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void getAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String userId = context.session().get("userId");

    albumRepository.findByIdAndUser(albumId, userId, res -> {
      if (res.succeeded()) {
        JsonObject album = res.result();
        if (album == null) {
          context.response().setStatusCode(404).end(new JsonObject().put("error", "Album not found or access denied").encode());
        } else {
          context.response()
            .putHeader("Content-Type", "application/json")
            .end(album.encodePrettily());
        }
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void updateAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String userId = context.session().get("userId");
    JsonObject body = context.body().asJsonObject();

    if (body == null || !body.containsKey("title")) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body or missing title").encode());
      return;
    }

    String title = body.getString("title");

    JsonObject updateData = new JsonObject().put("title", title);

    albumRepository.update(albumId, userId, updateData, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end();
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void deleteAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String userId = context.session().get("userId");

    albumRepository.delete(albumId, userId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end(); // No Content
      } else {
        String failureReason = res.cause().getMessage();
        if ("Album not found".equals(failureReason)) {
          context.response().setStatusCode(404).end(new JsonObject().put("error", "Album not found or access denied").encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }

  public void getPhotosFromAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String userId = context.session().get("userId");

    albumRepository.findByIdAndUser(albumId, userId, albumRes -> {
      if (albumRes.succeeded()) {
        JsonObject album = albumRes.result();
        if (album == null) {
          context.response().setStatusCode(404).end(new JsonObject().put("error", "Album not found or access denied").encode());
        } else {
          albumRepository.findPhotosInAlbum(albumId, userId, res -> {
            if (res.succeeded()) {
              JsonArray photos = res.result();
              context.response()
                .putHeader("Content-Type", "application/json")
                .end(photos.encodePrettily());
            } else {
              context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
            }
          });
        }
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }


  public void addPhotoToAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String userId = context.session().get("userId");
    JsonObject body = context.body().asJsonObject();

    if (body == null || !body.containsKey("photo_id")) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body or missing photo_id").encode());
      return;
    }

    String photoId = body.getString("photo_id");

    albumRepository.addPhotoToAlbum(albumId, userId, photoId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(201).end(new JsonObject().put("message", "Photo added to album successfully").encode());
      } else {
        context.response().setStatusCode(400).end(new JsonObject().put("error", res.cause().getMessage()).encode());
      }
    });
  }

  public void removePhotoFromAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String photoId = context.pathParam("photo_id");
    String userId = context.session().get("userId");

    albumRepository.removePhotoFromAlbum(albumId, userId, photoId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end();
      } else {
        context.response().setStatusCode(400).end(new JsonObject().put("error", res.cause().getMessage()).encode());
      }
    });
  }

  // Tags
  public void getTagsForAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String userId = context.session().get("userId");

    albumRepository.findByIdAndUser(albumId, userId, res -> {
      if (res.succeeded()) {
        JsonObject album = res.result();
        if (album != null) {
          albumRepository.findTagsByAlbumId(albumId, userId, tagRes -> {
            if (tagRes.succeeded()) {
              JsonArray tags = tagRes.result();
              context.response()
                .putHeader("Content-Type", "application/json")
                .end(tags.encodePrettily());
            } else {
              context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
            }
          });
        } else {
          context.response().setStatusCode(403).end(new JsonObject().put("error", "Album not found or access denied").encode());
        }
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }


  public void addTagToAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String userId = context.session().get("userId");
    JsonObject body = context.body().asJsonObject();

    if (body == null || !body.containsKey("tag_id")) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body or missing tag_id").encode());
      return;
    }

    String tagId = body.getString("tag_id");

    albumRepository.addTagToAlbum(albumId, tagId, userId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(201).end(new JsonObject().put("message", "Tag added to album successfully").encode());
      } else {
        String errorMessage = res.cause().getMessage();
        if ("Tag already associated with album".equals(errorMessage)) {
          context.response().setStatusCode(409).end(new JsonObject().put("error", errorMessage).encode());
        } else {
          context.response().setStatusCode(403).end(new JsonObject().put("error", errorMessage).encode());
        }
      }
    });
  }


  public void removeTagFromAlbum(RoutingContext context) {
    String albumId = context.pathParam("album_id");
    String tagId = context.pathParam("tag_id");
    String userId = context.session().get("userId");

    albumRepository.removeTagFromAlbum(albumId, tagId, userId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end(); // No Content
      } else {
        String errorMessage = res.cause().getMessage();
        if ("Tag not found, album not found, or access denied".equals(errorMessage)) {
          context.response().setStatusCode(403).end(new JsonObject().put("error", errorMessage).encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }

  public void getAllAlbumTags(RoutingContext context) {
    String userId = context.session().get("userId");

    albumRepository.findAllAlbumTagsByUser(userId, res -> {
      if (res.succeeded()) {
        context.response()
          .putHeader("Content-Type", "application/json")
          .end(res.result().encodePrettily());
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }
}
