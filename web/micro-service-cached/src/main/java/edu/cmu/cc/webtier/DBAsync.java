/**
 * The Helper class for QR Code encode and decode
 */
package edu.cmu.cc.webtier;

import io.vertx.core.*;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.sql.*;
import java.util.*;

/**
 * Microservice1 helper: QRCoder
 */
public class DBAsync {
    /**
     * Database name.
     */
    private static final String DB_NAME = "twitter";

    private static final String DB_CHARSET = "utf8mb4";

    private static final String DB_COLLAIION = "utf8mb4_bin";

    private static final String mysqlHost = System.getenv("MYSQL_HOST");

    /**
     * MySQL username.
     */
    private static final String mysqlName = System.getenv("MYSQL_NAME");
    /**
     * MySQL Password.
     */
    private static final String mysqlPwd = System.getenv("MYSQL_PWD");

    MySQLConnectOptions connectOptions;

    PoolOptions poolOptions;

    MySQLPool client;

    public DBAsync(Vertx vertx) {
        connectOptions = new MySQLConnectOptions()
                .setPort(3306)
                .setHost(mysqlHost)
                .setDatabase(DB_NAME)
                .setUser(mysqlName)
                .setCharset(DB_CHARSET)
                .setCollation(DB_COLLAIION)
                .setPassword(mysqlPwd);
        poolOptions = new PoolOptions().setMaxSize(Integer.parseInt(System.getProperty("POOL_SIZE", "2")));
        client = MySQLPool.pool(vertx, connectOptions, poolOptions);
    }


    public void close() {
        client.close();
    }

    public Future<RowSet<Row>> getByUser(String username, String tweetType) {
        Promise<RowSet<Row>> p = Promise.promise();

        String query;
        if (tweetType.equals("reply")) {
            query = "SELECT receiver, product_score, content, tags, latest_retweet_reply, latest_screen, latest_description FROM unified_table WHERE is_reply=\'True\' AND sender = %s ";
        } else if (tweetType.equals("retweet")) {
            query = "SELECT receiver, product_score, content, tags, latest_retweet_reply, latest_screen, latest_description FROM unified_table WHERE is_reply=\'False\' AND sender = %s ";
        } else {
            // TODO: Invalid request
            query = "SELECT receiver, product_score, content, tags, both_latest, latest_screen, latest_description FROM unified_table WHERE sender = %s ";
        }
        query = String.format(query, username);
        client
        .query(query)
        .execute().onSuccess(users -> {
            if (users.size() > 0) {
                p.complete(users);
            } else {
                // TODO: deal with null.
                p.fail("INVALID");
            }
        }).onFailure(p::fail);
        return p.future();
    }

}
