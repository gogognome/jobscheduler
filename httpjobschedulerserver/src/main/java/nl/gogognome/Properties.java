package nl.gogognome;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "httpjobschedulerserver", ignoreUnknownFields = false)
public class Properties {

    private int requestTimeoutMilliseconds = 30*1000;
    private String databaseConnectionUrl = "jdbc:h2:mem:httpjobscheduler" + System.currentTimeMillis();
    private String databaseUsername = "sa";
    private String databasePassword = "";
    private String dataEncoding = "BASE64";

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

    public String getDataEncoding() {
        return dataEncoding;
    }

    /**
     * Encoding used to transform the job's data to a String representation.
     * Use "BASE64" for BASE64 or use the name of a character encoding, like "UTF-8" or "ISO-8559-1".
     * @param dataEncoding the encoding
     */
    public void setDataEncoding(String dataEncoding) {
        this.dataEncoding = dataEncoding;
    }
}
