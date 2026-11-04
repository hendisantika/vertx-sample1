package id.my.hendisantika.vertx_sample1;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  private WebClient client;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    // Each test gets its own in-memory H2 database (MySQL mode) and a random HTTP port
    JsonObject config = new JsonObject()
      .put("HTTP_PORT", 0)
      .put("url", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1")
      .put("user", "sa")
      .put("password", "");
    MainVerticle verticle = new MainVerticle();
    vertx.deployVerticle(verticle, new DeploymentOptions().setConfig(config))
      .onComplete(testContext.succeeding(id -> {
        client = WebClient.create(vertx, new WebClientOptions()
          .setDefaultHost("localhost")
          .setDefaultPort(verticle.actualPort()));
        testContext.completeNow();
      }));
  }

  @Test
  void root_returns_hello(VertxTestContext testContext) {
    client.get("/").send().onComplete(testContext.succeeding(res -> testContext.verify(() -> {
      assertEquals(200, res.statusCode());
      assertTrue(res.bodyAsString().contains("Vert.x 5"));
      testContext.completeNow();
    })));
  }

  @Test
  void get_all_returns_seed_data(VertxTestContext testContext) {
    client.get("/api/Employees").send().onComplete(testContext.succeeding(res -> testContext.verify(() -> {
      assertEquals(200, res.statusCode());
      JsonArray employees = res.bodyAsJsonArray();
      assertEquals(2, employees.size());
      assertEquals("Abhishek", employees.getJsonObject(0).getString("fullName"));
      testContext.completeNow();
    })));
  }

  @Test
  void crud_flow(VertxTestContext testContext) {
    JsonObject payload = new JsonObject().put("fullName", "Hendi Santika").put("designation", "Engineer");
    client.post("/api/Employees").sendJsonObject(payload)
      .compose(res -> {
        testContext.verify(() -> assertEquals(201, res.statusCode()));
        long id = res.bodyAsJsonObject().getLong("id");
        return client.put("/api/Employees/" + id)
          .sendJsonObject(payload.copy().put("designation", "Architect"))
          .compose(put -> {
            testContext.verify(() -> assertEquals(204, put.statusCode()));
            return client.get("/api/Employees/" + id).send();
          })
          .compose(get -> {
            testContext.verify(() -> {
              assertEquals(200, get.statusCode());
              assertEquals("Architect", get.bodyAsJsonObject().getString("designation"));
            });
            return client.delete("/api/Employees/" + id).send();
          })
          .compose(del -> {
            testContext.verify(() -> assertEquals(204, del.statusCode()));
            return client.get("/api/Employees/" + id).send();
          });
      })
      .onComplete(testContext.succeeding(res -> testContext.verify(() -> {
        assertEquals(404, res.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  void invalid_payload_returns_bad_request(VertxTestContext testContext) {
    client.post("/api/Employees").sendBuffer(io.vertx.core.buffer.Buffer.buffer("not-json"))
      .onComplete(testContext.succeeding(res -> testContext.verify(() -> {
        assertEquals(400, res.statusCode());
        testContext.completeNow();
      })));
  }
}
