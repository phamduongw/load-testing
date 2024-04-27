package vn.bnh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger log4j = LogManager.getLogger(Main.class);

    private static final String JDBC_DRIVER = System.getenv("JDBC_DRIVER");
    private static final String JDBC_URL = System.getenv("JDBC_URL");
    private static final String JDBC_USERNAME = System.getenv("JDBC_USERNAME");
    private static final String JDBC_PASSWORD = System.getenv("JDBC_PASSWORD");
    private static final String JDBC_QUERY = System.getenv("JDBC_QUERY");
    private static final String NUMBER_OF_CONNECTIONS = System.getenv("NUMBER_OF_CONNECTIONS");
    private static final String LOOP_COUNT_PER_CONNECTION = System.getenv("LOOP_COUNT_PER_CONNECTION");

    private static final long MEGABYTE = 1024L * 1024L;

    public static void validateNotNull(String value, String name) {
        if (value == null) {
            String errorMessage = name + " is null";
            log4j.error(errorMessage);
            throw new NullPointerException(errorMessage);
        }
    }

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

    public static void main(String[] args) throws ClassNotFoundException {
        validateNotNull(JDBC_DRIVER, "JDBC_DRIVER");
        validateNotNull(JDBC_URL, "JDBC_URL");
        validateNotNull(JDBC_USERNAME, "JDBC_USERNAME");
        validateNotNull(JDBC_PASSWORD, "JDBC_PASSWORD");
        validateNotNull(NUMBER_OF_CONNECTIONS, "NUMBER_OF_CONNECTIONS");

        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            log4j.error(e.getMessage());
            throw e;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(Integer.parseInt(NUMBER_OF_CONNECTIONS));
        for (int i = 0; i < Integer.parseInt(NUMBER_OF_CONNECTIONS); i++) {
            executorService.submit(new QueryTask());
        }
        executorService.shutdown();
    }

    static class QueryTask implements Runnable {
        @Override
        public void run() {
            // CPU used
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            // RAM used
            Runtime runtime = Runtime.getRuntime();

            try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD); PreparedStatement statement = connection.prepareStatement(JDBC_QUERY)) {

                long loopCount = LOOP_COUNT_PER_CONNECTION != null ? Long.parseLong(LOOP_COUNT_PER_CONNECTION) : Long.MAX_VALUE;
                for (long i = 0; i < loopCount; i++) {
                    log4j.info("[{}] - Executing query: {}", (i + 1), JDBC_QUERY);
                    statement.execute();
                    log4j.info("CPU used: {}%", osBean.getSystemLoadAverage() / osBean.getAvailableProcessors() * 100);
                    log4j.info("RAM used: {}MB", bytesToMegabytes(runtime.totalMemory() - runtime.freeMemory()));
                }
            } catch (SQLException e) {
                log4j.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
