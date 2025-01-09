package id.my.hendisantika.vertx_sample1;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

  private JDBCClient jdbc;

  @Override
  public void start(Future<Void> fut) {
    // Create a router object.
    Router router = Router.router(vertx);

    // Bind "/" to our hello message - so we are still compatible.
    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
        .putHeader("content-type", "text/html")
        .end("<h1>Hello from my first Vert.x 3 application</h1>");
    });

    // Serve static resources from the /assets directory
    router.route("/assets/*").handler(StaticHandler.create("assets"));

    //My REST End points for my CRUD APP
    router.get("/api/Employees").handler(this::getAll);
    router.get("/api/Employees/:id").handler(this::getOne);
    router.route("/api/Employees*").handler(BodyHandler.create());
    router.post("/api/Employees").handler(this::addOne);
    router.delete("/api/Employees/:id").handler(this::deleteOne);
    router.put("/api/Employees/:id").handler(this::updateOne);

    ConfigRetriever retriever = ConfigRetriever.create(vertx);


    ConfigRetriever.getConfigAsFuture(retriever)
      .compose(config -> {
        jdbc = JDBCClient.createShared(vertx, new JsonObject()
          .put("url", "jdbc:mysql://localhost:3306/test")
          .put("user", "root")
          .put("password", "root")
          .put("driver_class", "com.mysql.jdbc.Driver")
          .put("max_pool_size", 30));

        return connect()
          .compose(connection -> {
            Future<Void> future = Future.future();
            createTableIfNeeded(connection)
              .compose(this::createSomeDataIfNone)
              .setHandler(x -> {
                connection.close();
                future.handle(x.mapEmpty());
              });
            return future;
          })
          .compose(v -> createHttpServer(config, router));

      })
      .setHandler(fut);
  }

  private Future<Void> createHttpServer(JsonObject config, Router router) {
    Future<Void> future = Future.future();
    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .listen(
        config.getInteger("HTTP_PORT", 8080),
        res -> future.handle(res.mapEmpty())
      );
    return future;
  }

}
