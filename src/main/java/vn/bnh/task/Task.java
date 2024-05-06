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
    private static final DatabaseConfiguration databaseConfiguration = new AnnotationConfigApplicationContext(AppConfig.class).getBean(DatabaseConfiguration.class);
    private static final Logger LOGGER = LogManager.getLogger(Task.class);
    private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getOperatingSystemMXBean();
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final long MEGABYTE = 1024L * 1024L;

    static void execQuery(PreparedStatement stmt, long startRequestTime, boolean isPooling, String query) throws SQLException {
        long startExecTime = System.currentTimeMillis();
        stmt.execute();
        long endExecTime = System.currentTimeMillis();
        long responseTime = endExecTime - startRequestTime;
        long latency = endExecTime - startExecTime;
        long memoryUsed = (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / MEGABYTE;
        int cpuUsage = (int) (OS_BEAN.getSystemLoadAverage() / OS_BEAN.getAvailableProcessors() * 100);

        LOGGER.info("CPU: {}% - RAM: {}MB - Pooling: {} - Response: {}ms - Latency: {}ms - Query: {}", cpuUsage, memoryUsed, isPooling, responseTime, latency, query);
    }

    private static void execInactiveQuery(String query) {
        long startRequestTime = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(databaseConfiguration.getJdbcUrl(), databaseConfiguration.getJdbcUsername(), databaseConfiguration.getJdbcPassword()); PreparedStatement stmt = conn.prepareStatement(query)) {
            execQuery(stmt, startRequestTime, false, query);
        } catch (CommunicationsException e) {
            LOGGER.warn("Communication error occurred. Retrying connection...");
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void inactiveConnAndExecQuery(Object queryObject) {
        if (queryObject instanceof ReadQuery readQuery) {
            List<String> queries = readQuery.getQueries();
            while (true) {
                for (String query : queries) {
                    execInactiveQuery(query);
                }
            }
        } else if (queryObject instanceof WriteQuery writeQuery) {
            String type = writeQuery.getType();
            String queryTemplate = writeQuery.getQueryTemplate();
            List<WriteQueryDetails> queries = writeQuery.getQueries();
            switch (type) {
                case "insert" -> {
                    while (true) {
                        for (WriteQueryDetails writeQueryDetails : queries) {
                            String tableName = writeQueryDetails.getTableName();

                            // Target Columns
                            StringJoiner columnNames = new StringJoiner(", ");
                            StringJoiner values = new StringJoiner(", ");
                            for (FieldDetails fieldDetails : writeQueryDetails.getTargetColumns()) {
                                columnNames.add(fieldDetails.getName());
                                values.add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));
                            }

                            execInactiveQuery(String.format(queryTemplate, tableName, columnNames, values));
                        }
                    }
                }
                case "update" -> {
                    while (true) {
                        for (WriteQueryDetails writeQueryDetails : queries) {
                            String tableName = writeQueryDetails.getTableName();

                            StringJoiner tgPair = new StringJoiner(", ");

                            for (FieldDetails fieldDetails : writeQueryDetails.getTargetColumns()) {
                                StringJoiner updatePair = new StringJoiner(" = ");

                                updatePair.add(fieldDetails.getName()).add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));

                                tgPair.add(updatePair.toString());
                            }

                            StringJoiner wcPair = new StringJoiner(" && ");

                            for (FieldDetails fieldDetails : writeQueryDetails.getWhereClauses()) {
                                StringJoiner updatePair = new StringJoiner(" = ");

                                updatePair.add(fieldDetails.getName()).add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));

                                wcPair.add(updatePair.toString());
                            }

                            execInactiveQuery(String.format(queryTemplate, tableName, tgPair, wcPair));
                        }
                    }
                }
                case "delete" -> {
                    while (true) {
                        for (WriteQueryDetails writeQueryDetails : queries) {
                            String tableName = writeQueryDetails.getTableName();

                            StringJoiner wcPair = new StringJoiner(" && ");

                            for (FieldDetails fieldDetails : writeQueryDetails.getWhereClauses()) {
                                StringJoiner updatePair = new StringJoiner(" = ");

                                updatePair.add(fieldDetails.getName()).add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));

                                wcPair.add(updatePair.toString());
                            }

                            execInactiveQuery(String.format(queryTemplate, tableName, wcPair));
                        }
                    }
                }
            }
        }
    }

    public static void activeConnAndExecQuery(Object queryObject) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(databaseConfiguration.getJdbcUrl(), databaseConfiguration.getJdbcUsername(), databaseConfiguration.getJdbcPassword());
        } catch (CommunicationsException e) {
            LOGGER.warn("Communication error occurred. Retrying connection...");
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        if (queryObject instanceof ReadQuery readQuery) {
            List<String> queries = readQuery.getQueries();
            while (true) {
                for (String query : queries) {
                    long startRequestTime = System.currentTimeMillis();
                    try {
                        PreparedStatement stmt = conn.prepareStatement(query);
                        execQuery(stmt, startRequestTime, true, query);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else if (queryObject instanceof WriteQuery writeQuery) {
            String type = writeQuery.getType();
            String queryTemplate = writeQuery.getQueryTemplate();
            List<WriteQueryDetails> queries = writeQuery.getQueries();
            switch (type) {
                case "insert" -> {
                    while (true) {
                        for (WriteQueryDetails writeQueryDetails : queries) {
                            String tableName = writeQueryDetails.getTableName();

                            // Target Columns
                            StringJoiner columnNames = new StringJoiner(", ");
                            StringJoiner values = new StringJoiner(", ");
                            for (FieldDetails fieldDetails : writeQueryDetails.getTargetColumns()) {
                                columnNames.add(fieldDetails.getName());
                                values.add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));
                            }

                            String query = String.format(queryTemplate, tableName, columnNames, values);
                            long startRequestTime = System.currentTimeMillis();
                            try {
                                PreparedStatement stmt = conn.prepareStatement(query);
                                execQuery(stmt, startRequestTime, true, query);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                case "update" -> {
                    while (true) {
                        for (WriteQueryDetails writeQueryDetails : queries) {
                            String tableName = writeQueryDetails.getTableName();

                            StringJoiner tgPair = new StringJoiner(", ");

                            for (FieldDetails fieldDetails : writeQueryDetails.getTargetColumns()) {
                                StringJoiner updatePair = new StringJoiner(" = ");

                                updatePair.add(fieldDetails.getName()).add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));

                                tgPair.add(updatePair.toString());
                            }

                            StringJoiner wcPair = new StringJoiner(" && ");

                            for (FieldDetails fieldDetails : writeQueryDetails.getWhereClauses()) {
                                StringJoiner updatePair = new StringJoiner(" = ");

                                updatePair.add(fieldDetails.getName()).add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));

                                wcPair.add(updatePair.toString());
                            }

                            String query = String.format(String.format(queryTemplate, tableName, tgPair, wcPair));
                            long startRequestTime = System.currentTimeMillis();
                            try {
                                PreparedStatement stmt = conn.prepareStatement(query);
                                execQuery(stmt, startRequestTime, true, query);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                case "delete" -> {
                    while (true) {
                        for (WriteQueryDetails writeQueryDetails : queries) {
                            String tableName = writeQueryDetails.getTableName();

                            StringJoiner wcPair = new StringJoiner(" && ");

                            for (FieldDetails fieldDetails : writeQueryDetails.getWhereClauses()) {
                                StringJoiner updatePair = new StringJoiner(" = ");

                                updatePair.add(fieldDetails.getName()).add(ValueGenerator.getRandomData(fieldDetails.isFixed(), fieldDetails.getDataType(), fieldDetails.getSampleValue()));

                                wcPair.add(updatePair.toString());
                            }

                            String query = String.format(String.format(queryTemplate, tableName, wcPair));
                            long startRequestTime = System.currentTimeMillis();
                            try {
                                PreparedStatement stmt = conn.prepareStatement(query);
                                execQuery(stmt, startRequestTime, true, query);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }
}
