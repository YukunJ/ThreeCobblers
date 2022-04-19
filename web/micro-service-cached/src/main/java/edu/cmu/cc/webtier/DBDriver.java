/**
 * The Helper class for QR Code encode and decode
 */
package edu.cmu.cc.webtier;

import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

/**
 * Microservice1 helper: QRCoder
 */
public class DBDriver {

    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    /**
     * Database name.
     */
    private static final String DB_NAME = "temp";

    private static final String mysqlHost = System.getenv("MYSQL_HOST");
    /**
     * MySQL username.
     */
    private static final String mysqlName = System.getenv("MYSQL_NAME");
    /**
     * MySQL Password.
     */
    private static final String mysqlPwd = System.getenv("MYSQL_PWD");

    /**
     * The connection (session) with the database.
     * HINT: pay attention to how this is used internally
     */
    private static Connection conn;

    /**
     * MySQL URL.
     */
    private static final String URL = "jdbc:mysql://" + mysqlHost + ":3306/"
            + DB_NAME + "?useSSL=false&serverTimezone=UTC";

    MySQLConnectOptions connectOptions;

    PoolOptions poolOptions;

    MySQLPool client;

    private Connection getDBConnection() throws SQLException {
        Objects.requireNonNull(mysqlHost);
        Objects.requireNonNull(mysqlName);
        Objects.requireNonNull(mysqlPwd);
        return DriverManager.getConnection(URL, mysqlName, mysqlPwd);
    }

    public DBDriver() throws SQLException {
        conn = getDBConnection();

    }

    public void doGet() throws InterruptedException, SQLException {
        // ASYNC version:
//        StringBuilder ret = new StringBuilder();
//        client
//        .query("SELECT * FROM Persons;")
//        .execute(ar -> {
//            if (ar.succeeded()) {
//                RowSet<Row> result = ar.result();
//                for(Row r: result) {
////                    System.out.println(r.getString(1));
//                    System.out.println(r.getString("LastName"));
//                }
//            } else {
//                System.out.println("Failure: " + ar.cause().getMessage());
//            }
//            client.close();
//        });
//        System.out.println("Earlier");

        // Traditional Version:

        String query = "SELECT * FROM Persons WHERE PersonID = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, 1);
        ResultSet rs = pstmt.executeQuery();
        if (!rs.next()) {
            System.out.println("Failure");
        } else {
            System.out.println(rs.getString("LastName"));
        }
    }

    public static void main(String[] args) throws Exception {
        DBDriver driver = new DBDriver();
        driver.doGet();
    }
}
