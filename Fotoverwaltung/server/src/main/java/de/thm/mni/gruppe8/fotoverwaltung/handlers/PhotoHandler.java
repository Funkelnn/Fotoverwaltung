package de.thm.mni.gruppe8.fotoverwaltung.handlers;

import de.thm.mni.gruppe8.fotoverwaltung.repositories.PhotoRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.AbstractVerticle;

import java.util.Arrays;
import java.util.UUID;

public class PhotoHandler extends AbstractVerticle {
  private final PhotoRepository photoRepository;
  private final Vertx vertx;

  public PhotoHandler(Vertx vertx, PhotoRepository photoRepository) {
    this.vertx = vertx;
    this.photoRepository = photoRepository;
  }

  public void getAllPhotos(RoutingContext context) {
    String userId = context.session().get("userId");

    photoRepository.findAllByUser(userId, res -> {
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

  public void getPhoto(RoutingContext context) {
    String photoId = context.pathParam("photo_id");
    String userId = context.session().get("userId");

    photoRepository.findByIdAndUser(photoId, userId, res -> {
      if (res.succeeded()) {
        JsonObject photo = res.result();
        if (photo == null) {
          // Foto nicht gefunden oder Benutzer ist nicht der Besitzer
          context.response().setStatusCode(404).end(new JsonObject().put("error", "Photo not found").encode());
        } else {
          // Foto erfolgreich gefunden und Benutzer ist der Besitzer
          context.response()
            .putHeader("Content-Type", "application/json")
            .end(photo.encodePrettily());
        }
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void downloadPhoto(RoutingContext context) {
    String photoId = context.pathParam("photo_id");
    String userId = context.session().get("userId");

    photoRepository.findByIdAndUser(photoId, userId, res -> {
      if (res.succeeded()) {
        JsonObject photo = res.result();
        if (photo != null && photo.getString("user_id").equals(userId)) {
          String filePath = photo.getString("filepath");
          context.response().sendFile(filePath);
        } else {
          context.response().setStatusCode(404).end(new JsonObject().put("error", "Photo not found or access denied").encode());
        }
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void uploadPhoto(RoutingContext context) {
    String userId = context.session().get("userId");

    // Überprüfen, ob Datei-Uploads vorhanden sind
    if (context.fileUploads().isEmpty()) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "No file uploaded").encode());
      return;
    }

    // Metadaten aus dem Multipart-Form-Datenfeld 'metadata' extrahieren
    String metadataString = context.request().getFormAttribute("metadata");
    if (metadataString == null || metadataString.isEmpty()) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "No metadata provided").encode());
      return;
    }

    JsonObject metadata = new JsonObject(metadataString);
    String title = metadata.getString("title");
    String captureDate = metadata.getString("capture_date");
    String captureTime = metadata.getString("capture_time", null); // Optional

    if (title == null || title.isEmpty() || captureDate == null || captureDate.isEmpty()) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Title and capture date must be provided").encode());
      return;
    }

    FileUpload fileUpload = context.fileUploads().iterator().next();
    String uploadedFileName = fileUpload.uploadedFileName();
    String fileName = fileUpload.fileName();
    String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

    // Überprüfen des Dateityps
    if (!Arrays.asList("jpg", "jpeg", "png", "heic", "webp").contains(extension)) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid file type").encode());
      return;
    }

    String newFileName = UUID.randomUUID().toString() + "." + extension;
    String filePath = "photos/" + userId + "/" + newFileName;

    // Erstelle das Verzeichnis, falls es nicht existiert
    vertx.fileSystem().mkdirs("photos/" + userId, mkdirRes -> {
      if (mkdirRes.succeeded()) {
        // Datei verschieben
        vertx.fileSystem().move(uploadedFileName, filePath, moveRes -> {
          if (moveRes.succeeded()) {
            // Metadaten speichern
            JsonObject photoData = new JsonObject()
              .put("user_id", Integer.parseInt(userId))
              .put("filepath", filePath)
              .put("title", title)
              .put("capture_date", captureDate)
              .put("capture_time", captureTime); // Optional

            photoRepository.create(photoData, res -> {
              if (res.succeeded()) {
                context.response().setStatusCode(201).end(new JsonObject().put("message", "Photo uploaded successfully").encode());
              } else {
                context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
              }
            });
          } else {
            context.response().setStatusCode(500).end(new JsonObject().put("error", "Failed to save file").encode());
          }
        });
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Failed to create directory").encode());
      }
    });
  }

  public void updatePhoto(RoutingContext context) {
    String photoId = context.pathParam("photo_id");
    String userId = context.session().get("userId");
    JsonObject body = context.body().asJsonObject();

    if (body == null) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body").encode());
      return;
    }

    JsonObject updateData = new JsonObject();
    if (body.containsKey("title") && !body.getString("title").isEmpty()) {
      updateData.put("title", body.getString("title"));
    }
    if (body.containsKey("capture_date") && !body.getString("capture_date").isEmpty()) {
      updateData.put("capture_date", body.getString("capture_date"));
    }
    if (body.containsKey("capture_time") && !body.getString("capture_time").isEmpty()) {
      updateData.put("capture_time", body.getString("capture_time"));
    }

    if (updateData.isEmpty()) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "No valid fields to update").encode());
      return;
    }

    photoRepository.update(photoId, userId, updateData, updateRes -> {
      if (updateRes.succeeded()) {
        context.response().setStatusCode(204).end();
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void deletePhoto(RoutingContext context) {
    String photoId = context.pathParam("photo_id");
    String userId = context.session().get("userId");

    photoRepository.findByIdAndUser(photoId, userId, res -> {
      if (res.succeeded()) {
        JsonObject photo = res.result();
        if (photo == null) {
          // Foto nicht gefunden oder Benutzer ist nicht der Besitzer
          context.response().setStatusCode(404).end(new JsonObject().put("error", "Photo not found").encode());
        } else {
          // Datei löschen
          String filePath = photo.getString("filepath");
          vertx.fileSystem().delete(filePath, deleteFileRes -> {
            if (deleteFileRes.succeeded()) {
              // Foto aus der Datenbank löschen
              photoRepository.delete(photoId, userId, deleteRes -> {
                if (deleteRes.succeeded()) {
                  context.response().setStatusCode(204).end();
                } else {
                  context.response().setStatusCode(500).end(new JsonObject().put("error", "Failed to delete photo from database").encode());
                }
              });
            } else {
              context.response().setStatusCode(500).end(new JsonObject().put("error", "Failed to delete file").encode());
            }
          });
        }
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  // Tags

  public void getTagsForPhoto(RoutingContext context) {
    String photoId = context.pathParam("photo_id");
    String userId = context.session().get("userId");

    photoRepository.findByIdAndUser(photoId, userId, res -> {
      if (res.succeeded()) {
        JsonObject photo = res.result();
        if (photo != null) {
          photoRepository.findTagsByPhotoId(photoId, userId, tagRes -> {
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
          context.response().setStatusCode(403).end(new JsonObject().put("error", "Photo not found or access denied").encode());
        }
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }


  public void addTagToPhoto(RoutingContext context) {
    String photoId = context.pathParam("photo_id");
    String userId = context.session().get("userId");
    JsonObject body = context.body().asJsonObject();

    if (body == null || !body.containsKey("tag_id")) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body or missing tag_id").encode());
      return;
    }

    String tagId = body.getString("tag_id");

    photoRepository.addTagToPhoto(photoId, tagId, userId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(201).end(new JsonObject().put("message", "Tag added to photo successfully").encode());
      } else {
        String errorMessage = res.cause().getMessage();
        if ("Tag already associated with photo".equals(errorMessage)) {
          context.response().setStatusCode(409).end(new JsonObject().put("error", errorMessage).encode());
        } else {
          context.response().setStatusCode(403).end(new JsonObject().put("error", errorMessage).encode());
        }
      }
    });
  }

  public void removeTagFromPhoto(RoutingContext context) {
    String photoId = context.pathParam("photo_id");
    String tagId = context.pathParam("tag_id");
    String userId = context.session().get("userId");

    photoRepository.removeTagFromPhoto(photoId, tagId, userId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end(); // No Content
      } else {
        String errorMessage = res.cause().getMessage();
        if ("Tag not found, photo not found, or access denied".equals(errorMessage)) {
          context.response().setStatusCode(403).end(new JsonObject().put("error", errorMessage).encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }

  public void getAllPhotoTags(RoutingContext context) {
    String userId = context.session().get("userId");

    photoRepository.findAllPhotoTagsByUser(userId, res -> {
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
