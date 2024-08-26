package de.thm.mni.gruppe8.fotoverwaltung;

import de.thm.mni.gruppe8.fotoverwaltung.handlers.*;
import de.thm.mni.gruppe8.fotoverwaltung.repositories.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import static io.vertx.core.http.HttpMethod.GET;

public class MainVerticle extends AbstractVerticle {
  final int port = 3000;

  public void start(Promise<Void> startPromise) {
    try {
      DatabaseManager.initialize();
    } catch (Exception e) {
      System.out.println("Failed to initialize database connection: " + e.getMessage());
      startPromise.fail(e);
      return;
    }

    AuthRepository authRepository = new AuthRepository();
    UserRepository userRepository = new UserRepository();
    PhotoRepository photoRepository = new PhotoRepository();
    AlbumRepository albumRepository = new AlbumRepository();
    TagRepository tagRepository = new TagRepository();

    AuthHandler authHandler = new AuthHandler(authRepository);
    UserHandler userHandler = new UserHandler(userRepository);
    PhotoHandler photoHandler = new PhotoHandler(vertx, photoRepository);
    AlbumHandler albumHandler = new AlbumHandler(albumRepository);
    TagHandler tagHandler = new TagHandler(tagRepository);

    Router mainRouter = Router.router(vertx);

    // Frontend Website
    mainRouter.route("/*").handler(StaticHandler.create("webroot"));

    // ApiRouter erstellen
    Router apiRouter = Router.router(vertx);
    apiRouter.route().handler(BodyHandler.create().setBodyLimit(50 * 1024 * 1024)); // 50 mb limit
    apiRouter.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    CorsHandler handler = CorsHandler.create()
      .allowedMethod(HttpMethod.GET)
      .allowedMethod(HttpMethod.POST)
      .allowedMethod(HttpMethod.PUT)
      .allowedMethod(HttpMethod.DELETE)
      .allowedMethod(HttpMethod.OPTIONS)
      .allowCredentials(true)
      .allowedHeader("Access-Control-Allow-Headers")
      .allowedHeader("Access-Control-Allow-Method")
      .allowedHeader("Access-Control-Allow-Origin")
      .allowedHeader("Access-Control-Allow-Credentials")
      .allowedHeader("Content-Type");
    apiRouter.route().handler(handler);

    // Auth-Routen
    apiRouter.post("/login").handler(authHandler::login);
    apiRouter.post("/logout").handler(authHandler::logout);

    // Alle folgenden Routen können nur als angemeldeter Nutzer verwendet werden
    apiRouter.route("/*").handler(this::checkAuthentication);

    // User-Routen
    apiRouter.get("/users").handler(userHandler::getAllUsers);
    apiRouter.get("/users/:user_id").handler(userHandler::getUser);
    apiRouter.post("/users").handler(userHandler::createUser);
    apiRouter.put("/users/:user_id").handler(userHandler::updateUser);
    apiRouter.delete("/users/:user_id").handler(userHandler::deleteUser);

    // Foto-Routen
    apiRouter.get("/photos").handler(photoHandler::getAllPhotos);
    apiRouter.get("/photos/:photo_id").handler(photoHandler::getPhoto);
    apiRouter.get("/photos/download/:photo_id").handler(photoHandler::downloadPhoto);
    apiRouter.post("/photos").handler(photoHandler::uploadPhoto);
    apiRouter.put("/photos/:photo_id").handler(photoHandler::updatePhoto);
    apiRouter.delete("/photos/:photo_id").handler(photoHandler::deletePhoto);

    // Album-Routen
    apiRouter.post("/albums").handler(albumHandler::createAlbum);
    apiRouter.get("/albums").handler(albumHandler::getAllAlbums);
    apiRouter.get("/albums/:album_id").handler(albumHandler::getAlbum);
    apiRouter.put("/albums/:album_id").handler(albumHandler::updateAlbum);
    apiRouter.delete("/albums/:album_id").handler(albumHandler::deleteAlbum);
    apiRouter.get("/albums/:album_id/photos").handler(albumHandler::getPhotosFromAlbum);
    apiRouter.post("/albums/:album_id/photos").handler(albumHandler::addPhotoToAlbum);
    apiRouter.delete("/albums/:album_id/photos/:photo_id").handler(albumHandler::removePhotoFromAlbum);

    // Tags Management
    apiRouter.get("/tags").handler(tagHandler::getAllTags);
    apiRouter.post("/tags").handler(tagHandler::createTag);
    apiRouter.delete("/tags/:tag_id").handler(tagHandler::deleteTag);

    // Photo Tags Management
    apiRouter.get("/photo-tags").handler(photoHandler::getAllPhotoTags);
    apiRouter.get("/photos/:photo_id/tags").handler(photoHandler::getTagsForPhoto);
    apiRouter.post("/photos/:photo_id/tags").handler(photoHandler::addTagToPhoto);
    apiRouter.delete("/photos/:photo_id/tags/:tag_id").handler(photoHandler::removeTagFromPhoto);

    // Album Tags Management
    apiRouter.get("/album-tags").handler(albumHandler::getAllAlbumTags);
    apiRouter.get("/albums/:album_id/tags").handler(albumHandler::getTagsForAlbum);
    apiRouter.post("/albums/:album_id/tags").handler(albumHandler::addTagToAlbum);
    apiRouter.delete("/albums/:album_id/tags/:tag_id").handler(albumHandler::removeTagFromAlbum);

    // Verbinde MainRouter mit ApiRouter
    mainRouter.route("/api/*").subRouter(apiRouter);

    vertx.createHttpServer().requestHandler(mainRouter).listen(port, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port " + port);
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void checkAuthentication(RoutingContext context) {
    if (context.session() == null || context.session().get("userId") == null) {
      context.response().setStatusCode(401).end(new JsonObject().put("error", "Unauthorized").encode());
    } else {
      context.next();
    }
  }
}
