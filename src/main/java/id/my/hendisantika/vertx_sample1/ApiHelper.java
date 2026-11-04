package id.my.hendisantika.vertx_sample1;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.NoSuchElementException;

/**
 * Created by IntelliJ IDEA.
 * Project : vertx-sample1
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 10/01/25
 * Time: 06.05
 * To change this template use File | Settings | File Templates.
 */
public class ApiHelper {
  private static <T> Handler<AsyncResult<T>> writeJsonResponse(RoutingContext context, int status) {
    return ar -> {
      if (ar.failed()) {
        handleFailure(context, ar.cause());
      } else {
        context.response().setStatusCode(status)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(ar.result()));
      }
    };
  }

  static <T> Handler<AsyncResult<T>> ok(RoutingContext rc) {
    return writeJsonResponse(rc, 200);
  }

  static <T> Handler<AsyncResult<T>> created(RoutingContext rc) {
    return writeJsonResponse(rc, 201);
  }

  static Handler<AsyncResult<Void>> noContent(RoutingContext rc) {
    return ar -> {
      if (ar.failed()) {
        handleFailure(rc, ar.cause());
      } else {
        rc.response().setStatusCode(204).end();
      }
    };
  }

  private static void handleFailure(RoutingContext rc, Throwable cause) {
    if (cause instanceof NoSuchElementException) {
      rc.response().setStatusCode(404).end(cause.getMessage());
    } else if (cause instanceof IllegalArgumentException) {
      rc.response().setStatusCode(400).end(cause.getMessage());
    } else {
      rc.fail(cause);
    }
  }
}
