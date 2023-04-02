package quarkus.rest;

import org.jboss.logging.Logger;
import quarkus.model.entity.Account;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("accounts")
public class AccountResource {
    @Inject
    Logger LOG;

    Set<Account> accounts = new HashSet<>();

    @PostConstruct
    public void setup() {
        accounts.add(new Account(123456789L, 987654321L, "George Baird", new BigDecimal("354.23")));
        accounts.add(new Account(121212121L, 888777666L, "Mary Taylor", new BigDecimal("560.03")));
        accounts.add(new Account(545454545L, 222444999L, "Diana Rigg", new BigDecimal("422.00")));
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Set<Account> allAccounts() {
        return accounts;
    }

    @GET
    @Path("/{accountNumber}")
    @Produces(APPLICATION_JSON)
    public Account getAccount(@PathParam("accountNumber") Long accountNumber) {
        Optional<Account> response = accounts.stream().filter(acct -> acct.getAccountNumber().equals(accountNumber)).findFirst();
        return response.orElseThrow(() -> new WebApplicationException("Account with id of " + accountNumber + " does not exists", 404));
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createAccount(Account account) {
        if (account.getAccountNumber() == null) {
            throw new WebApplicationException("No Account number specified.", 400);
        }

        accounts.add(account);
        return Response.status(201).entity(account).build();
    }

    @PUT
    @Path("{accountNumber}/withdrawal")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Account withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
        Account account = getAccount(accountNumber);
        account.withdrawFunds(new BigDecimal(amount));
        return account;
    }

    @PUT
    @Path("{accountNumber}/deposit")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) {
        Account account = getAccount(accountNumber);
        account.addFunds(new BigDecimal(amount));
        return account;
    }

    @DELETE
    @Path("{accountNumber}")
    @Produces(APPLICATION_JSON)
    public Response closeAccount(@PathParam("accountNumber") Long accountNumber) {
        Account oldAccount = getAccount(accountNumber);
        accounts.remove(oldAccount);
        return Response.noContent().build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            int statusCode = 500;
            if (exception instanceof WebApplicationException) {
                statusCode = ((WebApplicationException) exception).getResponse().getStatus();
            }

            JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
                    .add("exceptionType", exception.getClass().getName()).add("statusCode", statusCode);

            if (exception.getMessage() != null) {
                entityBuilder.add("message", exception.getMessage());
            }

            return Response.status(statusCode).entity(entityBuilder.build()).build();
        }
    }
}
