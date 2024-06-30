package vn.bnh.transaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import vn.bnh.config.AppConfig;
import vn.bnh.config.DatabaseConfiguration;
import vn.bnh.config.Transaction;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class TestTransaction {
    private static final DatabaseConfiguration DATABASE_CONFIG = new AnnotationConfigApplicationContext(AppConfig.class).getBean(DatabaseConfiguration.class);
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final int MEGABYTE = 1024 * 1024;
    private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getOperatingSystemMXBean();
    private static final Logger LOGGER = LogManager.getLogger(TestTransaction.class);

    public static void main(String[] args) throws SQLException {
        List<Transaction> transactions = DATABASE_CONFIG.getTransactions();
        while (true) {
            Connection conn = null;
            try {
                for (Transaction transaction : transactions) {
                    long startRequestTime = System.currentTimeMillis();
                    conn = DriverManager.getConnection(DATABASE_CONFIG.getJdbcUrl(), DATABASE_CONFIG.getJdbcUsername(), DATABASE_CONFIG.getJdbcPassword());
                    conn.setAutoCommit(false);
                    for (String query : transaction.getQueries()) {
                        try (PreparedStatement stmt = conn.prepareStatement(query)) {
                            logQueryExecutionStats(stmt, startRequestTime, query);
                        } catch (SQLException ex) {
                            conn.rollback();
                            LOGGER.error("Error executing query: " + query, ex);
                            throw ex;
                        }
                    }
                    conn.commit();
                    LOGGER.info("Committed transaction");
                }
            } catch (SQLException ex) {
                LOGGER.error("Error establishing connection or setting autocommit: ", ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        LOGGER.error("Error closing connection: ", ex);
                    }
                }
            }
        }
    }

    private static void logQueryExecutionStats(PreparedStatement stmt, long startRequestTime, String query) throws SQLException {
        long startExecTime = System.currentTimeMillis();
        stmt.execute();
        long endExecTime = System.currentTimeMillis();
        long responseTime = endExecTime - startRequestTime;
        long latency = endExecTime - startExecTime;
        long memoryUsed = (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / MEGABYTE;
        int cpuUsage = (int) (OS_BEAN.getSystemLoadAverage() / OS_BEAN.getAvailableProcessors() * 100);

        LOGGER.info("CPU: {}% - RAM: {}MB - Response Time: {}ms - Latency: {}ms - Query: {}", cpuUsage, memoryUsed, responseTime, latency, query);
    }
}
