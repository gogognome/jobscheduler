package nl.gogognome;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "httpjobschedulerserver", ignoreUnknownFields = false)
public class Properties {

    private int requestTimeoutMilliseconds = 30*1000;
    private String databaseConnectionUrl = "jdbc:h2:mem:httpjobscheduler" + System.currentTimeMillis() ;
    private String databaseUsername = "sa";
    private String databasePassword = "";

    public int getRequestTimeoutMilliseconds() {
        return requestTimeoutMilliseconds;
    }

    public void setRequestTimeoutMilliseconds(int requestTimeoutMilliseconds) {
        this.requestTimeoutMilliseconds = requestTimeoutMilliseconds;
    }

    public String getDatabaseConnectionUrl() {
        return databaseConnectionUrl;
    }

    public void setDatabaseConnectionUrl(String databaseConnectionUrl) {
        this.databaseConnectionUrl = databaseConnectionUrl;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

}
