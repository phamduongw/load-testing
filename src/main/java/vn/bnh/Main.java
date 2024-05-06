package vn.bnh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import vn.bnh.config.*;
import vn.bnh.task.Task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            DatabaseConfiguration databaseConfiguration = context.getBean(DatabaseConfiguration.class);

            if (initializeDatabaseConnection(databaseConfiguration)) {
                ExecutorService executorService = initializeExecutorService(databaseConfiguration);
                shutdownExecutorService(executorService);
            }
        }
    }

    private static boolean initializeDatabaseConnection(DatabaseConfiguration databaseConfiguration) {
        String jdbcDriver = databaseConfiguration.getJdbcDriver();
        String jdbcUrl = databaseConfiguration.getJdbcUrl();
        String jdbcUsername = databaseConfiguration.getJdbcUsername();
        String jdbcPassword = databaseConfiguration.getJdbcPassword();

        LOGGER.info("Start connecting to the database!");
        try {
            Class.forName(jdbcDriver);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword)) {
                LOGGER.info("Connected to the database successfully!");
                return true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.error("Database connection failed!", e);
            return false;
        }
    }

    private static ExecutorService initializeExecutorService(DatabaseConfiguration databaseConfiguration) {
        LoopCount loopCount = databaseConfiguration.getLoopCount();

        int connCount = databaseConfiguration.getConnCount();
        double connPoolingWeight = databaseConfiguration.getConnPoolingWeight();

        ReadQuery readQuery = databaseConfiguration.getReadQuery();
        List<WriteQuery> writeQueries = databaseConfiguration.getWriteQueries();

        WriteQuery insertQuery = findQueryByType(writeQueries, "insert");
        WriteQuery updateQuery = findQueryByType(writeQueries, "update");
        WriteQuery deleteQuery = findQueryByType(writeQueries, "delete");

        double selectWeight = readQuery.getConnWeight();
        double insertWeight = insertQuery != null ? insertQuery.getConnWeight() : 0;
        double updateWeight = updateQuery != null ? updateQuery.getConnWeight() : 0;
        double deleteWeight = deleteQuery != null ? deleteQuery.getConnWeight() : 0;

        int selectQueryCount = (int) (connCount * selectWeight);
        int insertQueryCount = (int) (connCount * insertWeight);
        int updateQueryCount = (int) (connCount * updateWeight);
        int deleteQueryCount = (int) (connCount * deleteWeight);

        int totalQueryCount = selectQueryCount + insertQueryCount + updateQueryCount + deleteQueryCount;
        int maxCount = Math.max(selectQueryCount, Math.max(insertQueryCount, Math.max(updateQueryCount, deleteQueryCount)));
        int connPoolingCount = (int) (totalQueryCount * connPoolingWeight);

        ExecutorService executorService = Executors.newFixedThreadPool(totalQueryCount);

        for (int i = 0; i < maxCount; i++) {
            submitTaskIfNotNull(executorService, readQuery, i < selectQueryCount, i < connPoolingCount, loopCount);
            submitTaskIfNotNull(executorService, insertQuery, i < insertQueryCount, i < connPoolingCount, loopCount);
            submitTaskIfNotNull(executorService, updateQuery, i < updateQueryCount, i < connPoolingCount, loopCount);
            submitTaskIfNotNull(executorService, deleteQuery, i < deleteQueryCount, i < connPoolingCount, loopCount);
        }

        return executorService;
    }

    private static WriteQuery findQueryByType(List<WriteQuery> writeQueries, String type) {
        return writeQueries.stream().filter(wq -> type.equals(wq.getType())).findFirst().orElse(null);
    }

    private static void submitTaskIfNotNull(ExecutorService executorService, Object queryObject, boolean condition, boolean isActiveConn, LoopCount loopCount) {
        if (condition && queryObject != null) {
            executorService.submit(new QueryTask(isActiveConn, queryObject, loopCount));
        }
    }

    private static void shutdownExecutorService(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static class QueryTask implements Runnable {
        private final boolean isActiveConn;
        private final Object queryObject;
        private final LoopCount loopCount;

        public QueryTask(boolean isActiveConn, Object queryObject, LoopCount loopCount) {
            this.isActiveConn = isActiveConn;
            this.queryObject = queryObject;
            this.loopCount = loopCount;
        }

        @Override
        public void run() {
            if (isActiveConn) {
                Task.activeConnAndExecQuery(queryObject, loopCount);
            } else {
                Task.inactiveConnAndExecQuery(queryObject, loopCount);
            }
        }
    }
}
