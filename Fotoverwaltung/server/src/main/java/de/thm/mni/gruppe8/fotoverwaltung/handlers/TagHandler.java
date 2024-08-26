package de.thm.mni.gruppe8.fotoverwaltung.handlers;

import de.thm.mni.gruppe8.fotoverwaltung.repositories.TagRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public class TagHandler {
  private final TagRepository tagRepository;

  public TagHandler(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  public void getAllTags(RoutingContext context) {
    String userId = context.session().get("userId");

    tagRepository.findAllByUser(userId, res -> {
      if (res.succeeded()) {
        JsonArray tags = res.result();
        context.response()
          .putHeader("Content-Type", "application/json")
          .end(tags.encodePrettily());
      } else {
        context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
      }
    });
  }

  public void createTag(RoutingContext context) {
    String userId = context.session().get("userId");
    JsonObject body = context.body().asJsonObject();

    if (body == null || !body.containsKey("name")) {
      context.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid JSON body or missing name").encode());
      return;
    }

    String newTagName = body.getString("name");

    tagRepository.findByUser(userId, newTagName, res -> {
      if (res.succeeded()) {
        List<String> existingTags = res.result();
        boolean tagExists = existingTags.stream().anyMatch(existingTag -> existingTag.equals(newTagName));

        if (tagExists) {
          context.response().setStatusCode(409).end(new JsonObject().put("error", "Tag already exists").encode());
        } else {
          JsonObject tagData = new JsonObject()
            .put("user_id", userId)
            .put("name", newTagName);

          tagRepository.create(tagData, createRes -> {
            if (createRes.succeeded()) {
              context.response().setStatusCode(201).end(new JsonObject().put("message", "Tag created successfully").encode());
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

  public void deleteTag(RoutingContext context) {
    String tagId = context.pathParam("tag_id");
    String userId = context.session().get("userId");

    tagRepository.delete(tagId, userId, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end(); // No Content
      } else {
        String errorMessage = res.cause().getMessage();
        if ("Tag not found or access denied".equals(errorMessage)) {
          context.response().setStatusCode(404).end(new JsonObject().put("error", "Tag not found or access denied").encode());
        } else {
          context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
      }
    });
  }
}
