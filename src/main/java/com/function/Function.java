package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import com.azure.identity.*;
import com.azure.core.credential.*;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import java.sql.*;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request1,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        //========================
        //========================

        // Uncomment one of the two lines depending on the identity type
        DefaultAzureCredential creds = new DefaultAzureCredentialBuilder().build(); // system-assigned identity
        //DefaultAzureCredential creds = new DefaultAzureCredentialBuilder().managedIdentityClientId('<client-id-of-user-assigned-identity>")'build(); // user-assigned identity

        // Get the token  
        TokenRequestContext request = new TokenRequestContext();
        request.addScopes("https://database.windows.net//.default");
        AccessToken token=creds.getToken(request).block();

        // Set token in your SQL connection
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName("aaasqlmanid56server.database.windows.net");
        ds.setDatabaseName("aaasqlmanid56db");
        ds.setAccessToken(token.getToken());

        // Connect
        try {
            Connection connection = ds.getConnection(); 
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT SUSER_SNAME()");
            if (rs.next()) {
                context.getLogger().info("Signed into database as: " + rs.getString(1));
                //System.out.println("Signed into database as: " + rs.getString(1));
            }	
        }
        catch (Exception e) {
            context.getLogger().info(e.getMessage());
            //System.out.println(e.getMessage());
        }

        //========================
        //========================

        // Parse query parameter
        final String query = request1.getQueryParameters().get("name");
        final String name = request1.getBody().orElse(query);

        if (name == null) {
            return request1.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            return request1.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        }
    }
}
