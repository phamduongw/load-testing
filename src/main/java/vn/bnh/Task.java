package vn.bnh;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Task {
    private static final Logger log4j = LogManager.getLogger(Task.class);

    private static final String LOOP_COUNT = System.getenv("LOOP_COUNT");

    private static final String JDBC_URL = System.getenv("JDBC_URL");
    private static final String JDBC_USERNAME = System.getenv("JDBC_USERNAME");
    private static final String JDBC_PASSWORD = System.getenv("JDBC_PASSWORD");

    private static final long MEGABYTE = 1024L * 1024L;

    static void connectAndExecuteQuery(String query) {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        while (true) {
            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD); PreparedStatement statement = conn.prepareStatement(query)) {
                long loopCount = LOOP_COUNT != null ? Long.parseLong(LOOP_COUNT) : Long.MAX_VALUE;
                for (long i = 0; i < loopCount; i++) {
                    int cpuUsed = (int) (osBean.getSystemLoadAverage() / osBean.getAvailableProcessors() * 100);
                    long ramUser = (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE;
                    long startTime = System.currentTimeMillis();
                    statement.execute();
                    log4j.info("CPU: {}% - RAM: {}MB - Latency: {}ms - Query: {} - [{}]", cpuUsed, ramUser, System.currentTimeMillis() - startTime, query, (i + 1));
                }
            } catch (CommunicationsException e) {
                log4j.warn("Communication error occurred. Retrying connection...");
            } catch (SQLException e) {
                log4j.error(e.getMessage());
            }
        }
    }
}

class SelectTask implements Runnable {
    private static final String SELECT_QUERY = System.getenv("SELECT_QUERY");

    @Override
    public void run() {
        Task.connectAndExecuteQuery(SELECT_QUERY);
    }
}

class InsertTask implements Runnable {
    private static final String INSERT_QUERY = System.getenv("INSERT_QUERY");

    @Override
    public void run() {
        Task.connectAndExecuteQuery(INSERT_QUERY);
    }
}

class UpdateTask implements Runnable {
    private static final String UPDATE_QUERY = System.getenv("UPDATE_QUERY");

    @Override
    public void run() {
        Task.connectAndExecuteQuery(UPDATE_QUERY);
    }
}

class DeleteTask implements Runnable {
    private static final String DELETE_QUERY = System.getenv("DELETE_QUERY");

    @Override
    public void run() {
        Task.connectAndExecuteQuery(DELETE_QUERY);
    }
}
