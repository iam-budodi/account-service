package quarkus.rest;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import quarkus.model.entity.Account;
import quarkus.model.valueobject.AccountStatus;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestHTTPEndpoint(AccountResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountResourceTest {

    @Test
    @Order(1)
    void allAccounts() {
        Response response =
                given()
                        .accept(APPLICATION_JSON)
                        .when().get()
                        .then()
                        .statusCode(200)
                        .body(
                                containsString("George Baird"),
                                containsString("Mary Taylor"),
                                containsString("Diana Rigg")
                        )
                        .extract()
                        .response();

        List<Account> accounts = response.jsonPath().getList("$");
        assertThat(accounts, not(empty()));
        assertThat(accounts, hasSize(3));
    }

    @Test
    @Order(2)
    void getAccount() {
        Account account =
                given()
                        .accept(APPLICATION_JSON)
                        .pathParams("accountNumber", 123456789)
                        .when().get("/{accountNumber}")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Account.class);

        assertThat(account.getAccountNumber(), equalTo(123456789L));
        assertThat(account.getCustomerName(), equalTo("George Baird"));
        assertThat(account.getBalance(), equalTo(new BigDecimal("354.23")));
        assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));

    }

    @Test
    @Order(3)
    void createAccount() {
        Account newAccount = new Account(324324L, 112244L, "Sandy Holmes", new BigDecimal("154.55"));
        Account returnedAccount =
                given()
                        .contentType(APPLICATION_JSON)
                        .body(newAccount)
                        .when().post()
                        .then()
                            .statusCode(201)
                            .extract()
                            .as(Account.class);

        assertThat(returnedAccount, notNullValue());
        assertThat(returnedAccount, equalTo(newAccount));

        Response response =
                given()
                        .when().get()
                        .then()
                            .statusCode(200)
                            .body(
                                    containsString("George Baird"),
                                    containsString("Mary Taylor"),
                                    containsString("Diana Rigg"),
                                    containsString("Sandy Holmes")
                            )
                        .extract()
                        .response();

        List<Account> accounts = response.jsonPath().getList("$");
        assertThat(accounts, not(empty()));
        assertThat(accounts, hasSize(4));
    }

    @Test
    @Order(4)
    void withdrawal() {
        Account account =
                given()
                        .pathParams("accountNumber", 545454545)
                        .when().get("/{accountNumber}")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Account.class);

        assertThat(account.getAccountNumber(), is(equalTo(545454545L)));
        assertThat(account.getCustomerName(), is(equalTo("Diana Rigg")));
        assertThat(account.getBalance(), is(equalTo(new BigDecimal("422.00"))));
        assertThat(account.getAccountStatus(), is(equalTo(AccountStatus.OPEN)));

        Account response =
                given()
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .pathParam("accountNumber", 545454545)
                        .body("56.21")
                        .when().put("/{accountNumber}/withdrawal")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Account.class);

        assertThat(response.getAccountNumber(), is(equalTo(545454545L)));
        assertThat(response.getCustomerName(), is(equalTo("Diana Rigg")));
        assertThat(response.getBalance(), is(equalTo(account.getBalance().subtract(new BigDecimal("56.21")))));
        assertThat(response.getAccountStatus(), is(equalTo(AccountStatus.OPEN)));
    }

    @Test
    @Order(5)
    void deposit() {
        Account account =
                given()
                        .pathParam("accountNumber", 123456789)
                        .when().get("/{accountNumber}")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Account.class);

        assertThat(account.getAccountNumber(), is(equalTo(123456789L)));
        assertThat(account.getCustomerName(), is(equalTo("George Baird")));
        assertThat(account.getBalance(), is(equalTo(new BigDecimal("354.23"))));
        assertThat(account.getAccountStatus(), is(equalTo(AccountStatus.OPEN)));

        Account response =
                given()
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .pathParams("accountNumber", 123456789)
                        .body("28.42")
                        .when().put("/{accountNumber}/deposit")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Account.class);

        assertThat(response.getAccountNumber(), is(equalTo(123456789L)));
        assertThat(response.getCustomerName(), is(equalTo("George Baird")));
        assertThat(response.getBalance(), is(equalTo(account.getBalance().add(new BigDecimal("28.42")))));
        assertThat(response.getAccountStatus(), is(equalTo(AccountStatus.OPEN)));
    }

    @Test
    void closeAccount() {
        given()
                .accept(APPLICATION_JSON)
                .pathParams("accountNumber", 121212121)
                .when().delete("/{accountNumber}")
                .then()
                .statusCode(204);

        given()
                .accept(APPLICATION_JSON)
                .pathParams("accountNumber", 121212121)
                .when().get("/{accountNumber}")
                .then()
                .statusCode(404);
    }
}