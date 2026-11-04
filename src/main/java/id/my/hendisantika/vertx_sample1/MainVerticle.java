package id.my.hendisantika.vertx_sample1;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static id.my.hendisantika.vertx_sample1.ApiHelper.created;
import static id.my.hendisantika.vertx_sample1.ApiHelper.noContent;
import static id.my.hendisantika.vertx_sample1.ApiHelper.ok;

/**
 * Created by IntelliJ IDEA.
 * Project : vertx-sample1
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 05/11/26
 * Time: 05.59
 */
public class MainVerticle extends VerticleBase {

  private static final String SELECT_ALL = "SELECT id, fullName, designation FROM employees";
  private static final String SELECT_ONE = SELECT_ALL + " WHERE id = ?";
  private static final String INSERT = "INSERT INTO employees (fullName, designation) VALUES (?, ?)";
  private static final String UPDATE = "UPDATE employees SET fullName = ?, designation = ? WHERE id = ?";
  private static final String DELETE = "DELETE FROM employees WHERE id = ?";

  private Pool pool;
  private HttpServer server;

  @Override
  public Future<?> start() {
    Router router = Router.router(vertx);

    router.route("/").handler(rc -> rc.response()
      .putHeader("content-type", "text/html")
      .end("<h1>Hello from my first Vert.x 5 application</h1>"));

    // Serve static resources from the /assets directory
    router.route("/assets/*").handler(StaticHandler.create("assets"));

    // REST endpoints for the CRUD app
    router.route("/api/Employees*").handler(BodyHandler.create());
    router.get("/api/Employees").handler(this::getAll);
    router.get("/api/Employees/:id").handler(this::getOne);
    router.post("/api/Employees").handler(this::addOne);
    router.put("/api/Employees/:id").handler(this::updateOne);
    router.delete("/api/Employees/:id").handler(this::deleteOne);

    ConfigRetriever retriever = ConfigRetriever.create(vertx);

    return retriever.getConfig()
      .compose(config -> {
        pool = JDBCPool.pool(vertx,
          new JDBCConnectOptions()
            .setJdbcUrl(config.getString("url", "jdbc:mysql://localhost:3306/test"))
            .setUser(config.getString("user", "root"))
            .setPassword(config.getString("password", "root")),
          new PoolOptions().setMaxSize(config.getInteger("max_pool_size", 30)));

        return createTableIfNeeded()
          .compose(v -> createSomeDataIfNone())
          .compose(v -> vertx.createHttpServer()
            .requestHandler(router)
            .listen(config.getInteger("HTTP_PORT", 8080)));
      })
      .onSuccess(s -> server = s);
  }

  @Override
  public Future<?> stop() {
    return pool == null ? Future.succeededFuture() : pool.close();
  }

  /**
   * Actual port the HTTP server is bound to (useful when configured with port 0).
   */
  public int actualPort() {
    return server == null ? -1 : server.actualPort();
  }

  // ---- Persistence ----

  private Future<Void> createTableIfNeeded() {
    return vertx.fileSystem().readFile("table.sql")
      .compose(sql -> pool.query(sql.toString()).execute())
      .mapEmpty();
  }

  private Future<Void> createSomeDataIfNone() {
    return pool.query(SELECT_ALL).execute()
      .compose(rows -> {
        if (rows.size() > 0) {
          return Future.succeededFuture();
        }
        return pool.withTransaction(conn -> Future.all(
          insert(conn, new Employee("Abhishek", "Software Engineer")),
          insert(conn, new Employee("Sunil", "Release Manager"))
        )).mapEmpty();
      });
  }

  private Future<Employee> insert(SqlClient client, Employee employee) {
    return client.preparedQuery(INSERT)
      .execute(Tuple.of(employee.getFullName(), employee.getDesignation()))
      .map(rows -> {
        Row keys = rows.property(JDBCPool.GENERATED_KEYS);
        return new Employee(keys.getLong(0), employee.getFullName(), employee.getDesignation());
      });
  }

  private Future<List<Employee>> query() {
    return pool.query(SELECT_ALL).execute().map(MainVerticle::toEmployees);
  }

  private Future<Employee> queryOne(long id) {
    return pool.preparedQuery(SELECT_ONE).execute(Tuple.of(id))
      .map(rows -> {
        List<Employee> employees = toEmployees(rows);
        if (employees.isEmpty()) {
          throw new NoSuchElementException("No Employee with id " + id);
        }
        return employees.getFirst();
      });
  }

  private Future<Void> update(long id, Employee employee) {
    return pool.preparedQuery(UPDATE)
      .execute(Tuple.of(employee.getFullName(), employee.getDesignation(), id))
      .compose(rows -> rows.rowCount() == 0
        ? Future.failedFuture(new NoSuchElementException("No Employee with id " + id))
        : Future.succeededFuture());
  }

  private Future<Void> delete(long id) {
    return pool.preparedQuery(DELETE).execute(Tuple.of(id))
      .compose(rows -> rows.rowCount() == 0
        ? Future.failedFuture(new NoSuchElementException("Unknown Employee " + id))
        : Future.succeededFuture());
  }

  private static List<Employee> toEmployees(RowSet<Row> rows) {
    List<Employee> employees = new ArrayList<>(rows.size());
    for (Row row : rows) {
      employees.add(new Employee(row.getLong(0), row.getString(1), row.getString(2)));
    }
    return employees;
  }

  // ---- HTTP Actions ----

  private void getAll(RoutingContext rc) {
    query()
      .map(employees -> new JsonArray(employees.stream().map(Employee::toJson).toList()))
      .onComplete(ok(rc));
  }

  private void getOne(RoutingContext rc) {
    parseId(rc).compose(this::queryOne).map(Employee::toJson).onComplete(ok(rc));
  }

  private void addOne(RoutingContext rc) {
    readEmployee(rc)
      .compose(employee -> insert(pool, employee))
      .map(Employee::toJson)
      .onComplete(created(rc));
  }

  private void updateOne(RoutingContext rc) {
    parseId(rc)
      .compose(id -> readEmployee(rc).compose(employee -> update(id, employee)))
      .onComplete(noContent(rc));
  }

  private void deleteOne(RoutingContext rc) {
    parseId(rc).compose(this::delete).onComplete(noContent(rc));
  }

  private static Future<Long> parseId(RoutingContext rc) {
    try {
      return Future.succeededFuture(Long.parseLong(rc.pathParam("id")));
    } catch (NumberFormatException e) {
      return Future.failedFuture(new NoSuchElementException("Invalid Employee id " + rc.pathParam("id")));
    }
  }

  private static Future<Employee> readEmployee(RoutingContext rc) {
    try {
      JsonObject body = rc.body().asJsonObject();
      if (body == null) {
        return Future.failedFuture(new IllegalArgumentException("Request body is required"));
      }
      Employee employee = new Employee(body);
      if (employee.getFullName() == null || employee.getDesignation() == null) {
        return Future.failedFuture(new IllegalArgumentException("fullName and designation are required"));
      }
      return Future.succeededFuture(employee);
    } catch (RuntimeException e) {
      return Future.failedFuture(new IllegalArgumentException("Invalid Employee payload", e));
    }
  }
}
