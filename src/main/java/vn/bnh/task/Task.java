package vn.bnh.task;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import vn.bnh.config.*;
import vn.bnh.generator.ValueGenerator;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

public class Task {
    private static final DatabaseConfiguration DATABASE_CONFIG = new AnnotationConfigApplicationContext(AppConfig.class)
            .getBean(DatabaseConfiguration.class);
    private static final Logger LOGGER = LogManager.getLogger(Task.class);
    private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getOperatingSystemMXBean();
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final long MEGABYTE = 1024L * 1024L;

    private static void logQueryExecutionStats(PreparedStatement stmt, long startRequestTime, boolean isPooling, String query) throws SQLException {
        long startExecTime = System.currentTimeMillis();
        stmt.execute();
        long endExecTime = System.currentTimeMillis();
        long responseTime = endExecTime - startRequestTime;
        long latency = endExecTime - startExecTime;
        long memoryUsed = (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / MEGABYTE;
        int cpuUsage = (int) (OS_BEAN.getSystemLoadAverage() / OS_BEAN.getAvailableProcessors() * 100);

        LOGGER.info("CPU: {}% - RAM: {}MB - Pooling: {} - Response Time: {}ms - Latency: {}ms - Query: {}",
                cpuUsage, memoryUsed, isPooling, responseTime, latency, query);
    }

    private static void executeQuery(Connection conn, String query, long startRequestTime, boolean isPooling) {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            logQueryExecutionStats(stmt, startRequestTime, isPooling, query);
        } catch (SQLException e) {
            LOGGER.error("Query execution error: {}", e.getMessage());
        }
    }

    private static void execInactiveQuery(String query) {
        long startRequestTime = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(DATABASE_CONFIG.getJdbcUrl(), DATABASE_CONFIG.getJdbcUsername(), DATABASE_CONFIG.getJdbcPassword())) {
            executeQuery(conn, query, startRequestTime, false);
        } catch (CommunicationsException e) {
            LOGGER.warn("Communication error occurred. Retrying connection...");
        } catch (SQLException e) {
            LOGGER.error("Database connection error: {}", e.getMessage());
        }
    }

    public static void inactiveConnAndExecQuery(Object queryObject, LoopCount loopCount) {
        long startTime = System.currentTimeMillis();
        long durationMillis = loopCount.getType().equals("time") ? loopCount.getCount() * 1000 : 0;
        int maxLoops = loopCount.getType().equals("number") ? (int) loopCount.getCount() : Integer.MAX_VALUE;

        if (queryObject instanceof ReadQuery readQuery) {
            List<String> queries = readQuery.getQueries();
            for (int i = 0; i < maxLoops && (durationMillis == 0 || System.currentTimeMillis() - startTime < durationMillis); i++) {
                queries.forEach(Task::execInactiveQuery);
            }
        } else if (queryObject instanceof WriteQuery writeQuery) {
            String queryTemplate = writeQuery.getQueryTemplate();
            List<WriteQueryDetails> queries = writeQuery.getQueries();
            for (int i = 0; i < maxLoops && (durationMillis == 0 || System.currentTimeMillis() - startTime < durationMillis); i++) {
                queries.forEach(queryDetails -> execInactiveQuery(generateQuery(queryTemplate, queryDetails, writeQuery.getType())));
            }
        }
    }

    public static void activeConnAndExecQuery(Object queryObject, LoopCount loopCount) {
        long startTime = System.currentTimeMillis();
        long durationMillis = loopCount.getType().equals("time") ? loopCount.getCount() * 1000 : 0;
        int maxLoops = loopCount.getType().equals("number") ? (int) loopCount.getCount() : Integer.MAX_VALUE;

        try (Connection conn = DriverManager.getConnection(DATABASE_CONFIG.getJdbcUrl(), DATABASE_CONFIG.getJdbcUsername(), DATABASE_CONFIG.getJdbcPassword())) {
            if (queryObject instanceof ReadQuery readQuery) {
                List<String> queries = readQuery.getQueries();
                for (int i = 0; i < maxLoops && (durationMillis == 0 || System.currentTimeMillis() - startTime < durationMillis); i++) {
                    queries.forEach(query -> executeQuery(conn, query, System.currentTimeMillis(), true));
                }
            } else if (queryObject instanceof WriteQuery writeQuery) {
                String queryTemplate = writeQuery.getQueryTemplate();
                List<WriteQueryDetails> queries = writeQuery.getQueries();
                for (int i = 0; i < maxLoops && (durationMillis == 0 || System.currentTimeMillis() - startTime < durationMillis); i++) {
                    queries.forEach(queryDetails -> executeQuery(conn, generateQuery(queryTemplate, queryDetails, writeQuery.getType()), System.currentTimeMillis(), true));
                }
            }
        } catch (CommunicationsException e) {
            LOGGER.warn("Communication error occurred. Retrying connection...");
        } catch (SQLException e) {
            LOGGER.error("Database connection error: {}", e.getMessage());
        }
    }

    private static String generateQuery(String queryTemplate, WriteQueryDetails queryDetails, String type) {
        String tableName = queryDetails.getTableName();
        return switch (type) {
            case "insert" ->
                    String.format(queryTemplate, tableName, generateColumns(queryDetails), generateValues(queryDetails));
            case "update" ->
                    String.format(queryTemplate, tableName, generateUpdatePairs(queryDetails), generateWhereClauses(queryDetails));
            case "delete" -> String.format(queryTemplate, tableName, generateWhereClauses(queryDetails));
            default -> throw new IllegalArgumentException("Invalid query type: " + type);
        };
    }

    private static StringJoiner generateColumns(WriteQueryDetails queryDetails) {
        StringJoiner columnNames = new StringJoiner(", ");
        queryDetails.getTargetColumns().forEach(fieldDetails -> columnNames.add(fieldDetails.getName()));
        return columnNames;
    }

    private static StringJoiner generateValues(WriteQueryDetails queryDetails) {
        StringJoiner values = new StringJoiner(", ");
        queryDetails.getTargetColumns().forEach(fieldDetails -> values.add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue())));
        return values;
    }

    private static StringJoiner generateUpdatePairs(WriteQueryDetails queryDetails) {
        StringJoiner updatePairs = new StringJoiner(", ");
        queryDetails.getTargetColumns().forEach(fieldDetails -> updatePairs.add(fieldDetails.getName() + " = " + ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue())));
        return updatePairs;
    }

    private static StringJoiner generateWhereClauses(WriteQueryDetails queryDetails) {
        StringJoiner whereClauses = new StringJoiner(" && ");
        queryDetails.getWhereClauses().forEach(fieldDetails -> whereClauses.add(fieldDetails.getName() + " = " + ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue())));
        return whereClauses;
    }
}
