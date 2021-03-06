package com.zetcode;

import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// example for asynchronous database access with Jasync and Javalin

public class AsyncDatabase {

    private static final Logger logger = LoggerFactory.getLogger(AsyncDatabase.class);

    public static void main(String[] args) {

        var config = new ConnectionPoolConfigurationBuilder();
        config.setUsername("postgres");
        config.setPassword("s$cret");
        config.setDatabase("postgres");
        config.setMaxActiveConnections(100);
        config.setMaxIdleTime(TimeUnit.MINUTES.toMillis(15));
        config.setMaxPendingQueries(10000);
        config.setConnectionValidationInterval(TimeUnit.SECONDS.toMillis(30));

        Connection connection = PostgreSQLConnectionBuilder.createConnectionPool(config);

        Javalin app = Javalin.create()
                .events(event -> {
                    event.serverStarting(() -> {
                        logger.info("--- SERVER STARTING!");
                        connection.connect().get();
                        logger.info("--- connection STARTED!");
                    });
                    event.serverStopping(() -> {
                        logger.info("--- SERVER STOPPING!");
                        connection.disconnect().get();
                        logger.info("--- connection STOPPED!");
                    });
                })
                .start(7000);

        app.get("/", (ctx) -> {

            final CompletableFuture<QueryResult> queryResultCompletableFuture =
                    connection.sendPreparedStatement("select 3+6");

            ctx.result(
                    queryResultCompletableFuture
                            .thenApply((t) -> "got result: " + t.getRows().get(0).get(0))
            );
        });
    }
}
