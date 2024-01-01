package com.restvert;


import com.hazelcast.config.Config;
import com.restvert.auth.HtpasswdAuthImpl;
import com.restvert.common.Constants;
import com.restvert.handlers.UserAuthorizationHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class RestVerticle extends AbstractVerticle {

	//private long _currentGetOrdersResponseId;
	//For mapping beetween response ID to response for GetOrders
	//private Map<Long, HttpServerResponse> getOrdersIdToResponse = new HashMap<Long, HttpServerResponse>();
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	public RestVerticle()
	{
	}
  @Override
  public void start(Promise<Void> startPromise) throws Exception {	
	  HttpServer server = vertx.createHttpServer();
	  Router router = Router.router(vertx);

//	  //For consuming messages from 'OrderVerticle'
//	  EventBus eventBus = vertx.eventBus();
//	  eventBus.consumer(Constants.ORDER_VERTICLE, (message) -> {
//            Object messageBody = message.body();
//			System.out.println(messageBody.toString().length());
//			JsonObject messageJson = (JsonObject) messageBody;
//            JsonArray orders = messageJson.getJsonArray(Constants.ORDERS);
//            Long responseId = messageJson.getLong(Constants.RESPONSE_ID);
//            HttpServerResponse response = getOrdersIdToResponse.get(responseId);
//            getOrdersIdToResponse.remove(responseId);
//			response.end(orders.toBuffer());
//	  });
	  
	  //Create local session handler for all routings
	  router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

	  router.route(HttpMethod.POST, "/Login").handler(BodyHandler.create()).handler(new UserAuthorizationHandler(new HtpasswdAuthImpl(vertx)));
	  
	  router.route(HttpMethod.POST, "/Logout").handler(ctx -> {
		  ctx.session().destroy();

    	HttpServerResponse response = ctx.response();
	    response.putHeader("content-type", "text/plain");
	    response.setStatusCode(200).end("Session was destroyed succesfully");
	  });

	  router.route(HttpMethod.POST, "/AddOrder").handler(BodyHandler.create())
	  .handler(ctx -> {
		  Session session = ctx.session();
		  User user = session.get("user");
		  JsonObject body = ctx.body().asJsonObject();
		  HttpServerResponse response = ctx.response();
		  response.putHeader("content-type", "text/plain");
		    
		  if(user != null) {
		        String userName = user.principal().getString("username");
				System.out.println(userName + " user detected");
		        
				 String orderName = body.getString("orderName");
				  
		        JsonObject jsonObject = new JsonObject();
		        jsonObject.put(Constants.ACTION, Constants.ADD_ORDER);
		        jsonObject.put(Constants.USERNAME, userName);
		        jsonObject.put(Constants.ORDER_NAME, orderName);
		        
		        //TODO need to use publish if there are multiple listeners
		        vertx.eventBus().request(Constants.MESSAGE_BUS_ADDRESS, jsonObject, reply ->
		        {
		        	if (reply.succeeded())
		        		response.setStatusCode(200).end("Order was added successfully");
		        	else
		        		response.setStatusCode(500).end("Error during adding of order");
		        });
		        
		        log.info("Current Thread Id {" + Thread.currentThread().getId() + "} Is Clustered {+ " + vertx.isClustered() + "} ");
		    } else {
		    	response.setStatusCode(401).end("You need to login first!");
		    }
	  });
	  
	  router.route(HttpMethod.GET, "/GetOrders").handler(BodyHandler.create()).handler(ctx -> {
		  Session session = ctx.session();
		  HttpServerResponse response = ctx.response();
		  response.putHeader("content-type", "application/json");
		  User user = session.get("user");
		  
		    if(user != null) {		    	
		        String username = user.principal().getString("username");
				System.out.println(username + " user detected");
		        
		        JsonObject jsonObject = new JsonObject();
		        jsonObject.put(Constants.ACTION, Constants.GET_ORDERS);
		        jsonObject.put(Constants.USERNAME, username);

		        vertx.eventBus().request(Constants.MESSAGE_BUS_ADDRESS, jsonObject, reply ->
		        {
		        	JsonObject relpyJson = (JsonObject)(reply.result().body());
		        	if (reply.succeeded())
		        		response.setStatusCode(200).end(relpyJson.toString());
		        	else
		        	{
		        		response.setStatusCode(500).end(relpyJson.toString());
		        	}
		        		
		        });
		        
		    } else {
		    	System.out.println("Null user request detected");
	
			    response.putHeader("content-type", "text/plain");
			    response.setStatusCode(401).end("You need to login first!");
		    }
		  });
	  
	  server.requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
    	  startPromise.complete();
    	  System.out.println("HTTP server started on port 8888");
      } else {
    	  startPromise.fail(http.cause());
      }
    });
  }
    
  public static void main(String[] args) {
	 
	  System.out.println("In main, start");
	  Config hazelcastConfig = new Config();
	    hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1").setEnabled(true);
	    hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
	    
	    ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);
	    Vertx
	    .builder()
	    .withClusterManager(mgr)
	    .buildClustered()
	    .onComplete(res -> {
	    if (res.succeeded()) {
	      Vertx vertx = res.result();
	      vertx.deployVerticle(new RestVerticle());
	    } else {
	      // failed!
	    }
	  });
	    
	   System.out.println("In main, end");
  }
}

