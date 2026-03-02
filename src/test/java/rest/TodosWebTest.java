package rest;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class TodosWebTest {

    private static final Pattern CSRF_FORM_PATTERN = Pattern.compile("name=\"csrf-token\" value=\"([^\"]+)\"");

    @Test
    void shouldRenderNeobrutalistBoardAndCreateTaskThroughForm() {
        String title = "web-form-task-" + UUID.randomUUID();

        Csrf csrf = fetchCsrf();

        given()
                .redirects().follow(false)
                .contentType("application/x-www-form-urlencoded")
                .cookie("csrf-token", csrf.token())
                .formParam("csrf-token", csrf.token())
                .formParam("title", title)
                .when()
                .post("/Todos/add")
                .then()
                .statusCode(303);

        given()
                .when()
                .get("/Todos/todos")
                .then()
                .statusCode(200)
                .body(containsString("todo-board"))
                .body(containsString("neo-shell"))
                .body(containsString(title.substring(1)));
    }

    @Test
    void shouldCompleteAndDeleteTaskFromBoard() {
        String title = "web-complete-task-" + UUID.randomUUID();

        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", title))
                .when()
                .post("/tasks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Csrf csrf = fetchCsrf();

        given()
                .redirects().follow(false)
                .contentType("application/x-www-form-urlencoded")
                .cookie("csrf-token", csrf.token())
                .formParam("csrf-token", csrf.token())
                .formParam("id", id)
                .when()
                .post("/Todos/complete")
                .then()
                .statusCode(303);

        given()
                .when()
                .get("/Todos/todos")
                .then()
                .statusCode(200)
                .body(containsString("data-task-id=\"" + id + "\""))
                .body(containsString("todo-card is-completed"));

        csrf = fetchCsrf();

        given()
                .redirects().follow(false)
                .contentType("application/x-www-form-urlencoded")
                .cookie("csrf-token", csrf.token())
                .formParam("csrf-token", csrf.token())
                .formParam("id", id)
                .when()
                .post("/Todos/delete")
                .then()
                .statusCode(303);

        given()
                .when()
                .get("/Todos/todos")
                .then()
                .statusCode(200)
                .body(not(containsString("data-task-id=\"" + id + "\"")));
    }

    private Csrf fetchCsrf() {
        Response response = given()
                .when()
                .get("/Todos/todos")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String token = response.cookie("csrf-token");
        if (token != null && !token.isBlank()) {
            return new Csrf(token);
        }

        Matcher matcher = CSRF_FORM_PATTERN.matcher(response.asString());
        if (matcher.find()) {
            return new Csrf(matcher.group(1));
        }

        throw new IllegalStateException("Could not extract csrf-token from /Todos/todos");
    }

    private record Csrf(String token) {
    }
}
