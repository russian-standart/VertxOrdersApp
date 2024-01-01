package com.restvert.handlers;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.impl.BasicAuthHandlerImpl;

public class UserAuthorizationHandler extends BasicAuthHandlerImpl {

	public UserAuthorizationHandler(AuthenticationProvider authenticaionProvider)
	{
		super(authenticaionProvider, null);
	}
	
	@Override
	public void handle(RoutingContext event) {
		Logger log = LoggerFactory.getLogger(this.getClass());
		
		System.out.println("inside UserAutho handle");
		log.info("inside UserAutho handle");
		
	        HttpServerResponse response = event.response();
	        HttpServerRequest request = event.request();
	        
	        Session session = event.session();
	        String requestedPath = request.path();

	        if(requestedPath.equalsIgnoreCase("/Login")) {
	            if(request.method() != HttpMethod.POST)
	                event.fail(500);
	            else {
	            	System.out.println("body to string: " + event.body().toString());
	               	System.out.println("body as string: " + event.body().asString());
	               	
	            	JsonObject reqJson = event.body().asJsonObject();
                	String username = reqJson.getString("username");
                	String password = reqJson.getString("password");
                	
                	log.info(username + ":" + password + " login attempt");
                	
                	response.putHeader("content-type", "application/json");
                	
                    authProvider.authenticate(new UsernamePasswordCredentials(username, password), res -> {
                        if(res.succeeded()) {
                            User userToSet = res.result();
                            session.put("user", userToSet);
                            log.info("Login successful for " + username);   
                            //response.putHeader("Location", "/").setStatusCode(302);
                            JsonObject resJson = new JsonObject().put("Login", true);
                            response.setStatusCode(200);
                            response.end(resJson.toBuffer());
                        } else {
                            //event.fail(500);
                            log.error("Auth error for " + request.authority().host());
                            JsonObject resJson = new JsonObject().put("Login", false);
                            response.setStatusCode(401);
                            response.end(resJson.toBuffer());
                        }
                    });
	            }
	        }
	}

}
