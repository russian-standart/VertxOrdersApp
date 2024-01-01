package com.ordersvert;

import com.hazelcast.config.Config;
import com.ordersvert.common.Constants;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.ConfigUtil;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;


public class OrderVerticle  extends AbstractVerticle {

	//TODO: this map should be used in order to keep the orders in-memory, 
	//For avoiding reading them from file every-time
	//private Map<String, List<Order>> orders = new HashMap<String, List<Order>>();
	
	  @Override
	  public void start(Promise<Void> startPromise) throws Exception {
		  System.out.println("OrderVerticle start");
		  EventBus eventBus = vertx.eventBus();
		  eventBus.consumer(Constants.MESSAGE_BUS_ADDRESS, (message) -> {
	            Object messageBody = message.body();
				System.out.println(messageBody.toString().length());
	            JsonObject messageJson = (JsonObject) messageBody;
	            
	            String action = messageJson.getString(Constants.ACTION);
	            switch(action)
	            {
		            case Constants.GET_ORDERS:
		            {
		            	String username = messageJson.getString(Constants.USERNAME);
		            	if (username != null)
		            	{
			            	GetUserOrders(username).onComplete(event -> {
			            		JsonObject jsonRet = new JsonObject();
			            		if (event.succeeded())
			            		{
				            		jsonRet.put(Constants.ORDERS, event.result());
			            		}
			            		else
			            		{
			            			jsonRet.put("Success", false);
			            			jsonRet.put("ErrorDetails", event.cause().getMessage());
			            		}
			            		
			            		message.reply(jsonRet);
			            		//eventBus.publish(Constants.ORDER_VERTICLE, jsonRet);
			            	});
		            	}
		            	else
		            	{
		            		  System.out.println("user name key does not exist");
		            	}
		            	break;
		            }
		            case Constants.ADD_ORDER:
		            {
		            	String username = messageJson.getString(Constants.USERNAME);
		            	if (username != null)
		            	{
		            		String orderName = messageJson.getString(Constants.ORDER_NAME);
			            	AddOrder(username, orderName, message);
		            	}
		            	else
		            	{
		            		System.out.println("user name key does not exist");
		            	}
		            	break;
		            }
		            default:
		            	System.out.println("Action " + action + " is not supported");
	            }   
	        });
		  
		  startPromise.complete();
	  }


	//TODO: read the orders once during startup from file
	//Then when adding an order, add it both to file and in-memory
	private void AddOrder(String username, String orderName, Message<Object> message) {
		
		String usernameOrdersFilename = "Orders_" + username + ".json";
		vertx.fileSystem().exists(usernameOrdersFilename, (event) -> {
		if (!event.result().booleanValue())
			vertx.fileSystem().createFile(usernameOrdersFilename);	
		
		vertx.fileSystem().readFile(usernameOrdersFilename, (fileReadEvent) -> {
			if (fileReadEvent.succeeded())
			{
				Buffer buffer = fileReadEvent.result();
				
				JsonArray jsonArray = new JsonArray();
				if (buffer.length() > 0)
				{
					jsonArray = new JsonArray(buffer);
				}
				
				JsonObject newOrder = new JsonObject();
				newOrder.put("Id", jsonArray.size() + 1); //assuming IDs can be equal for different users
				newOrder.put("Name", orderName);
				newOrder.put("Date", java.time.LocalDateTime.now().toString());
				
				jsonArray.add(newOrder);
				Buffer newJsonBuffer = jsonArray.toBuffer();
				vertx.fileSystem().writeFile(usernameOrdersFilename, newJsonBuffer, (writeEvent) ->
				{
					if (writeEvent.succeeded())
					{
						message.reply("Order was added successfully");
					}
					else
					{
						message.reply("Error during writing orders file");
					}
				});
			}
			else
			{
				message.reply("Failed reading orders file " + fileReadEvent.cause().getMessage());
			}
			
		});
	});
	}

	//TODO: read the orders once during startup from file into memory
	private Future<JsonArray> GetUserOrders(String username) {
		Promise<JsonArray> promise = Promise.promise();
		String usernameOrdersFilename = "Orders_" + username + ".json";
		vertx.fileSystem().exists(usernameOrdersFilename, (event) -> {
			if (event.result().booleanValue())
			{
				vertx.fileSystem().readFile(usernameOrdersFilename, (fileReadEvent) -> {
					if (fileReadEvent.succeeded())
					{
						Buffer buffer = fileReadEvent.result();
						JsonArray userOrders = new JsonArray(buffer);
						promise.complete(userOrders);
					}
					else
					{
						promise.fail("Error while reading orders file");
					}

				});
			}
			else
			{
				promise.complete(new JsonArray()); //no orders yet
			}
		});
		return promise.future();
	}
	
	 public static void main(String[] args) {
		 
		  System.out.println("In main, start");
		  Config hazelcastConfig = ConfigUtil.loadConfig();
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
		      vertx.deployVerticle(new OrderVerticle());
		    } else {
		      // failed!
		    }
		  });
		    
		   System.out.println("In main, end");
	  }
}


