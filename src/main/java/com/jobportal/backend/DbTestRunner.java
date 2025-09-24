package com.jobportal.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DbTestRunner implements CommandLineRunner {

    private final DataSource dataSource;

    public DbTestRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ Successfully connected to the database!");
            System.out.println("DB URL: " + conn.getMetaData().getURL());
            System.out.println("DB User: " + conn.getMetaData().getUserName());
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to the database:");
            e.printStackTrace();
        }
    }
}
