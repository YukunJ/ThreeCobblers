/**
 * The web application server using VERT.X framework
 */
package edu.cmu.cc.webtier;

import io.vertx.core.*;
import io.vertx.ext.web.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

/**
 * The application main driver
 */
public class MainVerticle extends AbstractVerticle {

  /**
   * http service handler
   */
  private HttpServer server;

  /**
   * the QR Coder instance
   */
  private QRCoder qrcoder = new QRCoder();

  private DBAsync db;

  MySQLPool pool;
  
  /**
   * The Kubernetes health check response
   */
  private static final String HEALTH_RESPONSE = "Greetings! This is a healthy webtier application\n";

  String message = "ThreeCobblers,971734603674\n";

    private static final int MAX_PAYLOAD = 22;

  private static Cache cache = new Cache(Integer.parseInt(System.getProperty("CACHE_SIZE", "250000")),
          Integer.parseInt(System.getProperty("CACHE_LIMIT", "10")));


  @Override
  public void start() {
      String defaultContentEncoding = java.nio.charset.Charset.defaultCharset().name();
    db = new DBAsync(vertx);
      System.out.println("Microservice1: version 2.3.0");
    server = vertx.createHttpServer();
    Router centralRouter = Router.router(vertx);
    // the base '/' route for Kubernetes health check
    Route healthCheckRoute = centralRouter.route().path("/");
    healthCheckRoute.handler( ctx -> {
        HttpServerResponse response = ctx.response();
        response.setChunked(true);
        response.putHeader("content-type", "text/plain");
        response.write(HEALTH_RESPONSE);
        ctx.response().end();
      }
    );

    // the QR Code encode/decode service routing
    Route QRCodeRoute = centralRouter.route().path("/qrcode");
    QRCodeRoute.handler( ctx -> {
        HttpServerResponse response = ctx.response();
        response.setChunked(true);
        response.putHeader("content-type", "text/plain");
        try {          
            MultiMap params = ctx.request().params();
            String type = params.get("type");
            String data = params.get("data");
            if (type.equals("encode")) {
                response.write(qrcoder.encode(data));
            } else if (type.equals("decode")) {
                response.write(qrcoder.decode(data));
            } else {
                // wrong path prefix, ignore
            }

        } catch (Exception e) {
            // escape
        }
        ctx.response().end();
      }
    );

    // the BlockChain Validation service routing
    Route BlockChainRoute = centralRouter.route().path("/blockchain");
    BlockChainRoute.handler( ctx -> {
		BlockChainValidator blockChainValidator = new BlockChainValidator();
        HttpServerResponse response = ctx.response();
        response.setChunked(true);
        response.putHeader("content-type", "text/plain");
        try {
            MultiMap params = ctx.request().params();
            String raw_request = params.get("cc");
            response.write(blockChainValidator.validateRequest(raw_request));
        } catch (Exception e) {
            // escape

        }
        ctx.response().end();
      }
    );
	  
	// Twitter Micro3 Service
      Route DBDriverRoute = centralRouter.route().path("/twitter");
      DBDriverRoute.handler( ctx -> {
	  HttpServerResponse response = ctx.response();
	  response.setChunked(true);
          response.putHeader("content-type", "text/plain; charset=utf-8");
          try {
              // TODO: validate request.
              MultiMap params = ctx.request().params();
//              boolean cacheHit = false;
              String user = params.get("user_id");
              String tweetType = params.get("type");
              String key = user + ":" + tweetType;
              RowSet<Row> cached = cache.get(key);
              if (cached != null) {
                  ctx.response().end(message + UtilsTweet.rerankResults(cached,
                          params.get("phrase"),
                          params.get("hashtag")));
              } else {
                  db.getByUser(user, tweetType)
                          .onComplete(ar -> {
                              if (ar.succeeded()) {
                                  RowSet<Row> results = ar.result();
                                  cache.put(key, results);
                                  ctx.response().end(message + UtilsTweet.rerankResults(results,
                                          params.get("phrase"),
                                          params.get("hashtag")));
                              } else {
                                  ctx.response().end(message + "INVALID");
                              }
                          });
              }
            } catch (Exception e) {
              ctx.response().end(message + "INVALID");
            }
          }
      );
    // attach the router to listening on port 8080
    server.requestHandler(centralRouter).listen(8080);
  }

    @Override
    public void stop() {
        this.server.close();
        this.db.close();
    }

}