package id.my.hendisantika.vertx_sample1;

import io.vertx.core.json.JsonObject;

/**
 * Created by IntelliJ IDEA.
 * Project : vertx-sample1
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 10/01/25
 * Time: 06.06
 * To change this template use File | Settings | File Templates.
 */
public class Employee {
  private long id;
  private String fullName;
  private String designation;

  public Employee() {
    // TODO Auto-generated constructor stub
  }


  public Employee(long id, String fullName, String designation) {
    super();
    this.id = id;
    this.fullName = fullName;
    this.designation = designation;
  }


  public Employee(String fullName, String designation) {
    this.fullName = fullName;
    this.designation = designation;
  }

  public Employee(JsonObject json) {
    this(
      json.getInteger("id", -1),
      json.getString("fullName"),
      json.getString("designation")
    );
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }
}
