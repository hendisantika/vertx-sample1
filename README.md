# vertx-sample1

[![Java CI with Maven](https://github.com/hendisantika/vertx-sample1/actions/workflows/maven.yml/badge.svg)](https://github.com/hendisantika/vertx-sample1/actions/workflows/maven.yml)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
[![Vert.x](https://img.shields.io/badge/vert.x-5.2.2-purple.svg)](https://vertx.io)
![Maven](https://img.shields.io/badge/Maven-3.10-blue?logo=apachemaven)
[![Dependabot](https://img.shields.io/badge/Dependabot-enabled-brightgreen?logo=dependabot)](.github/dependabot.yml)

A simple **Employee CRUD REST API** built with [Eclipse Vert.x 5](https://vertx.io) on **JDK 25**, backed by MySQL
through the Vert.x JDBC client.

## Tech Stack

| Component       | Version / Notes                                                |
|-----------------|----------------------------------------------------------------|
| Java            | 25 (enforced by `maven-enforcer-plugin`)                       |
| Eclipse Vert.x  | 5.2.2 (`vertx-core`, `vertx-web`, `vertx-config`, `vertx-jdbc-client`) |
| Launcher        | `io.vertx.launcher.application.VertxApplication`              |
| Database        | MySQL (runtime, `mysql-connector-j`) · H2 in MySQL mode (tests) |
| Testing         | JUnit Jupiter 6 + `vertx-junit5` + `vertx-web-client`          |
| Build           | Maven 3.10 via Maven Wrapper (`./mvnw`)                        |

## Prerequisites

- JDK **25** (e.g. `sdk install java 25-tem`)
- MySQL running on `localhost:3306` with a database named `test` (only needed to run the app, **not** for tests)

```bash
docker run -d --name mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=test mysql:8
```

## Configuration

Configuration is loaded with `vertx-config` (deployment config `-conf`, system properties, then environment variables).

| Key             | Default                              | Description                 |
|-----------------|--------------------------------------|-----------------------------|
| `HTTP_PORT`     | `8080`                               | HTTP port (`0` = random)    |
| `url`           | `jdbc:mysql://localhost:3306/test`   | JDBC URL                    |
| `user`          | `root`                               | Database user               |
| `password`      | `root`                               | Database password           |
| `max_pool_size` | `30`                                 | Max JDBC pool size          |

A sample file lives in [`src/main/conf/my-application.conf.json`](src/main/conf/my-application.conf.json).
Since environment variables are read too, you can also override any key, e.g. `HTTP_PORT=9090 ./mvnw exec:java`.

On startup the app creates the `employees` table ([`table.sql`](src/main/resources/table.sql)) if needed and seeds
two employees when the table is empty.

## Build, Test & Run

```bash
# Run tests (uses in-memory H2, no MySQL needed)
./mvnw clean test

# Build + test + package the fat jar
./mvnw clean verify

# Run from sources (uses src/main/conf/my-application.conf.json)
./mvnw clean compile exec:java

# Run the fat jar
java -jar target/vertx-sample1-1.0.0-SNAPSHOT-fat.jar -conf=src/main/conf/my-application.conf.json
```

## REST API

Base URL: `http://localhost:8080`

| Method   | Path                   | Description           | Success |
|----------|------------------------|-----------------------|---------|
| `GET`    | `/`                    | Hello page            | `200`   |
| `GET`    | `/assets/index.html`   | Static assets         | `200`   |
| `GET`    | `/api/Employees`       | List all employees    | `200`   |
| `GET`    | `/api/Employees/{id}`  | Get one employee      | `200`   |
| `POST`   | `/api/Employees`       | Create an employee    | `201`   |
| `PUT`    | `/api/Employees/{id}`  | Update an employee    | `204`   |
| `DELETE` | `/api/Employees/{id}`  | Delete an employee    | `204`   |

Errors: `404` when the employee doesn't exist, `400` for an invalid/missing JSON body.

```bash
# Create
curl -X POST http://localhost:8080/api/Employees \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Hendi Santika","designation":"Engineer"}'

# List
curl http://localhost:8080/api/Employees

# Update
curl -X PUT http://localhost:8080/api/Employees/3 \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Hendi Santika","designation":"Architect"}'

# Delete
curl -X DELETE http://localhost:8080/api/Employees/3
```

## CI/CD

[`.github/workflows/maven.yml`](.github/workflows/maven.yml) runs on every push / PR to `main` (and manually):

- Builds and tests with **Temurin JDK 25** (`mvn -B -ntp clean verify`) with Maven caching
- Uploads the fat jar as a build artifact
- On push to `main`, submits the Maven dependency graph so Dependabot alerts are accurate

## Dependency Updates (Dependabot)

[`.github/dependabot.yml`](.github/dependabot.yml):

- **Maven** — daily at 05:00 WIB, with a cooldown (3 days, 7 days for majors) before proposing new releases
- Grouped PRs: `vertx`, `junit`, `maven-plugins`, `jdbc-drivers`
- **GitHub Actions** — weekly (Monday), all actions grouped into one PR
- Conventional commit prefixes (`build(deps)`, `build(deps-dev)`, `ci(deps)`) and labels

## Help

- [Vert.x Documentation](https://vertx.io/docs/)
- [Vert.x 5 migration guide](https://vertx.io/docs/guides/vertx-5-migration-guide/)
- [Vert.x Stack Overflow](https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15)
- [Vert.x Discord](https://discord.gg/6ry7aqPWXy)

## Author

**Hendi Santika** — [s.id/hendisantika](https://s.id/hendisantika) · Telegram [@hendisantika34](https://t.me/hendisantika34)
