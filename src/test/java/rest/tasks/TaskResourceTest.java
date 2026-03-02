package rest.tasks;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class TaskResourceTest {

    @Test
    void shouldReturnConsistentErrorPayloads() {
        given()
                .queryParam("sort", "not-a-sort")
                .when()
                .get("/tasks")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"))
                .body("message", equalTo("Invalid sort: not-a-sort"))
                .body("details", notNullValue());

        given()
                .when()
                .get("/tasks/{id}", "00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"))
                .body("message", equalTo("Task not found"));

        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Error precondition test"))
                .when()
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "No If-Match"))
                .when()
                .put("/tasks/{id}", id)
                .then()
                .statusCode(428)
                .body("error", equalTo("precondition_required"))
                .body("message", equalTo("Missing If-Match header"));

        given()
                .header("If-Match", "W/\"999\"")
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Stale"))
                .when()
                .put("/tasks/{id}", id)
                .then()
                .statusCode(412)
                .body("error", equalTo("precondition_failed"))
                .body("message", equalTo("If-Match version does not match current resource version"))
                .body("details", notNullValue());
    }

    @Test
    void shouldCreateGetAndEnforceIfMatchOnPut() {
        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "REST put test"))
                .when()
                .post("/tasks")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("open"))
                .extract()
                .path("id");

        String etag = given()
                .when()
                .get("/tasks/{id}", id)
                .then()
                .statusCode(200)
                .header("ETag", notNullValue())
                .extract()
                .header("ETag");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Updated without if-match"))
                .when()
                .put("/tasks/{id}", id)
                .then()
                .statusCode(428);

        given()
                .header("If-Match", "W/\"999\"")
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Updated mismatched"))
                .when()
                .put("/tasks/{id}", id)
                .then()
                .statusCode(412);

        given()
                .header("If-Match", etag)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Updated title"))
                .when()
                .put("/tasks/{id}", id)
                .then()
                .statusCode(200)
                .header("ETag", not(equalTo(etag)))
                .body("title", equalTo("Updated title"));
    }

    @Test
    void shouldListWithFiltersAndDeletedView() {
        String openId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Filter open", "tags", List.of("alpha")))
                .when()
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        String completedId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Filter completed", "status", "completed", "tags", List.of("beta")))
                .when()
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        String completedEtag = given().when().get("/tasks/{id}", completedId).then().statusCode(200).extract().header("ETag");

        given()
                .header("If-Match", completedEtag)
                .when()
                .delete("/tasks/{id}", completedId)
                .then()
                .statusCode(204);

        given()
                .queryParam("status", "open")
                .queryParam("tag", "alpha")
                .when()
                .get("/tasks")
                .then()
                .statusCode(200)
                .body("total", greaterThanOrEqualTo(1))
                .body("items.id", hasItem(openId));

        given()
                .queryParam("deleted", true)
                .when()
                .get("/tasks")
                .then()
                .statusCode(200)
                .body("items.id", hasItem(completedId));
    }

    @Test
    void shouldSupportBulkCreateAndRestore() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("tasks", List.of(
                        Map.of("title", "Bulk rest one", "tags", List.of("ops")),
                        Map.of("title", "Bulk rest two", "status", "completed"))))
                .when()
                .post("/tasks/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(2));

        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "To restore"))
                .when()
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        String etag = given().when().get("/tasks/{id}", id).then().statusCode(200).extract().header("ETag");

        given()
                .header("If-Match", etag)
                .when()
                .delete("/tasks/{id}", id)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/tasks/{id}/restore", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id));
    }
}
