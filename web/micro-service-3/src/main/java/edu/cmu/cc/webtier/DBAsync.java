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

//    public CompositeFuture doGet(String username, String tweetType, String phrase, String tag) throws InterruptedException, SQLException {
//        Future<RowSet<Row>> future1 = getStaticScore(username, tweetType);
//
//        Future<HashMap<Long, Integer>> future2 = getDynamicScore(username, phrase, tag, tweetType);
//
//        return CompositeFuture.join(future1, future2);

//    }

    public void close() {
        client.close();
    }


    public Future<RowSet<Row>> getStaticScore(String username, String tweetType) {
        Promise p = Promise.promise();
        String query;
        String tweetColumn;
        if (tweetType.equals("reply")) {
            tweetColumn = "latest_tweet_reply";
        } else if (tweetType.equals("retweet")) {
            tweetColumn = "latest_tweet_retweet";
        } else {
            // TODO: Invalid Request Type
            tweetColumn = "latest_tweet_both";
        }
        query = String.format("SELECT user_b, latest_screen, latest_description, product_score, %s " +
                "FROM static_table WHERE %s != \"\" AND user_a = ? ", tweetColumn, tweetColumn);
        client
        .preparedQuery(query)
        .execute(Tuple.of(username)).onSuccess(users -> {
            if (users.size() > 0) {
                p.complete(users);
            } else {
                // TODO: deal with null.
                p.fail("INVALID");
            }
        }).onFailure(p::fail);

        return p.future();
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


    public Future<HashMap<Long, Integer>> getDynamicScore(String username, String phrase, String tag, String tweetType) {

        Promise p = Promise.promise();
        String query;
        if (tweetType.equals("reply")) {
            query = "SELECT receiver, content, tags FROM dynamic_table WHERE is_reply=\'True\' AND sender = ? ";
        } else if (tweetType.equals("retweet")) {
            query = "SELECT receiver, content, tags FROM dynamic_table WHERE is_reply=\'False\' AND sender = ? ";
        } else {
            // TODO: Invalid request
            query = "SELECT receiver, content, tags FROM dynamic_table WHERE sender = ? ";
        }
        client
        .preparedQuery(query)
        .execute(Tuple.of(username)).onSuccess(users -> {
            if (users.iterator().hasNext()) {
                p.complete(UtilsTweet.calculateDynamicCount(users, phrase, tag));
            } else {
                // TODO: deal with null.
                p.fail("INVALID");
            }
        }).onFailure(p::fail);

        return p.future();
    }

    public static void main(String[] args) throws Exception {

    }
}
