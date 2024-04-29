package vn.bnh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final Logger log4j = LogManager.getLogger(Main.class);

    private static final int CONN_COUNT = Integer.parseInt(System.getenv("CONN_COUNT"));

    private static final double SELECT_WEIGHT = Double.parseDouble(System.getenv("SELECT_WEIGHT"));
    private static final double INSERT_WEIGHT = Double.parseDouble(System.getenv("INSERT_WEIGHT"));
    private static final double UPDATE_WEIGHT = Double.parseDouble(System.getenv("UPDATE_WEIGHT"));
    private static final double DELETE_WEIGHT = Double.parseDouble(System.getenv("DELETE_WEIGHT"));

    public static void main(String[] args) throws ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log4j.error(e.getMessage());
            throw e;
        }

        int selectQueryCount = (int) (CONN_COUNT * SELECT_WEIGHT);
        int insertQueryCount = (int) (CONN_COUNT * INSERT_WEIGHT);
        int updateQueryCount = (int) (CONN_COUNT * UPDATE_WEIGHT);
        int deleteQueryCount = (int) (CONN_COUNT * DELETE_WEIGHT);

        int maxCount = Math.max(selectQueryCount, Math.max(insertQueryCount, Math.max(updateQueryCount, deleteQueryCount)));

        ExecutorService executorService = Executors.newFixedThreadPool(selectQueryCount + insertQueryCount + updateQueryCount + deleteQueryCount);
        for (int i = 0; i < maxCount; i++) {
            if (i < selectQueryCount) {
                executorService.submit(new SelectTask());
            }
            if (i < insertQueryCount) {
                executorService.submit(new InsertTask());
            }
            if (i < updateQueryCount) {
                executorService.submit(new UpdateTask());
            }
            if (i < deleteQueryCount) {
                executorService.submit(new DeleteTask());
            }
        }

        executorService.shutdown();
    }
}
