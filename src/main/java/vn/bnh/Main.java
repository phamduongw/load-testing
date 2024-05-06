package vn.bnh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import vn.bnh.config.AppConfig;
import vn.bnh.config.DatabaseConfiguration;
import vn.bnh.config.ReadQuery;
import vn.bnh.config.WriteQuery;
import vn.bnh.task.Task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        DatabaseConfiguration databaseConfiguration = new AnnotationConfigApplicationContext(AppConfig.class).getBean(DatabaseConfiguration.class);

        String jdbcDriver = databaseConfiguration.getJdbcDriver();
        String jdbcUrl = databaseConfiguration.getJdbcUrl();
        String jdbcUsername = databaseConfiguration.getJdbcUsername();
        String jdbcPassword = databaseConfiguration.getJdbcPassword();

        Connection connection = null;

        try {
            Class.forName(jdbcDriver);
            LOGGER.info("Start connecting to the database!");
            connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
            LOGGER.info("Connected to the database successfully!");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.error("Failed to close the connection!", e);
                }
            }
        }

        int connCount = databaseConfiguration.getConnCount();
        double connPoolingWeight = databaseConfiguration.getConnPoolingWeight();

        ReadQuery readQuery = databaseConfiguration.getReadQuery();
        WriteQuery insertQuery = null;
        WriteQuery updateQuery = null;
        WriteQuery deleteQuery = null;

        double selectWeight = readQuery.getConnWeight();
        double insertWeight = 0;
        double updateWeight = 0;
        double deleteWeight = 0;

        List<WriteQuery> writeQueries = databaseConfiguration.getWriteQueries();

        for (WriteQuery writeQuery : writeQueries) {
            switch (writeQuery.getType()) {
                case "insert" -> {
                    insertWeight = writeQuery.getConnWeight();
                    insertQuery = writeQuery;
                }
                case "update" -> {
                    updateWeight = writeQuery.getConnWeight();
                    updateQuery = writeQuery;
                }
                case "delete" -> {
                    deleteWeight = writeQuery.getConnWeight();
                    deleteQuery = writeQuery;
                }
            }
        }

        int selectQueryCount = (int) (connCount * selectWeight);
        int insertQueryCount = (int) (connCount * insertWeight);
        int updateQueryCount = (int) (connCount * updateWeight);
        int deleteQueryCount = (int) (connCount * deleteWeight);

        int totalQueryCount = selectQueryCount + insertQueryCount + updateQueryCount + deleteQueryCount;
        int maxCount = Math.max(selectQueryCount, Math.max(insertQueryCount, Math.max(updateQueryCount, deleteQueryCount)));
        int connPoolingCount = (int) (totalQueryCount * connPoolingWeight);

        ExecutorService executorService = Executors.newFixedThreadPool(totalQueryCount);

        for (int i = 0; i < maxCount; i++) {
            if (i < connPoolingCount) {
                if (i < selectQueryCount) {
                    executorService.submit(new ActiveTask(readQuery));
                }
                if (i < insertQueryCount) {
                    executorService.submit(new ActiveTask(insertQuery));
                }
                if (i < updateQueryCount) {
                    executorService.submit(new ActiveTask(updateQuery));
                }
                if (i < deleteQueryCount) {
                    executorService.submit(new ActiveTask(deleteQuery));
                }
            } else {
                if (i < selectQueryCount) {
                    executorService.submit(new InactiveTask(readQuery));
                }
                if (i < insertQueryCount) {
                    executorService.submit(new InactiveTask(insertQuery));
                }
                if (i < updateQueryCount) {
                    executorService.submit(new InactiveTask(updateQuery));
                }
                if (i < deleteQueryCount) {
                    executorService.submit(new InactiveTask(deleteQuery));
                }
            }
        }
    }
}

class ActiveTask implements Runnable {
    Object queryObject;

    public ActiveTask(Object queryObject) {
        this.queryObject = queryObject;
    }

    @Override
    public void run() {
        Task.activeConnAndExecQuery(queryObject);
    }
}

class InactiveTask implements Runnable {
    Object queryObject;

    public InactiveTask(Object queryObject) {
        this.queryObject = queryObject;
    }

    @Override
    public void run() {
        Task.inactiveConnAndExecQuery(queryObject);
    }
}
