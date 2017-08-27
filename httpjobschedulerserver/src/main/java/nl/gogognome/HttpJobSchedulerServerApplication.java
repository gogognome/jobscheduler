package nl.gogognome;

import com.zaxxer.hikari.HikariDataSource;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HttpJobSchedulerServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HttpJobSchedulerServerApplication.class, args);
	}

}
