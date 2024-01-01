package com.restvert.auth;

import java.nio.charset.Charset;
import java.nio.file.Paths;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.CredentialValidationException;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

public class HtpasswdAuthImpl implements AuthenticationProvider {
	
	Vertx _vertx;
  public HtpasswdAuthImpl(Vertx vertx) {
	  _vertx = vertx;
  }

  @Override
  public Future<User> authenticate(Credentials credential) {

    final UsernamePasswordCredentials authInfo;
    try {
      try {
    	  System.out.println("inside authenticate in Htpass");
        authInfo = (UsernamePasswordCredentials) credential;
      } catch (ClassCastException e) {
    	  System.out.println("ClassCastException in Htpass");
        throw new CredentialValidationException("Invalid credentials type", e);
      }
      authInfo.checkValid(null);
    } catch (RuntimeException e) {
    	System.out.println("RuntimeException in Htpass");
      return Future.failedFuture(e);
    }
    
    String username = authInfo.getUsername();
    String password = authInfo.getPassword();
    
    System.out.println("username: " + username + " password: " + password);
    
	User user = User.fromName(username);
    System.out.println("after User.fromName");
    
    Promise<Boolean> canUserLogin = canUserLogin(username, password);
    System.out.println("can user login?");
    Promise<User> promiseUser = Promise.promise();
    canUserLogin.future().onComplete(event -> {
    	if (event.result())
    	{
    		System.out.println("Username credetials match");
    		promiseUser.complete(user);
    		return;
    	}
    	System.out.println("Username credetials do not match");
		promiseUser.fail("Username credetials do not match");
		return;
    });
   
    return promiseUser.future();
  }

@Override
public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
	// TODO Auto-generated method stub
	
}

private Promise<Boolean> canUserLogin(String userName, String password) {
	Promise<Boolean> promise = Promise.promise();
	Future<Buffer> futureBuffer = readFile("UsersLogin.json", Charset.defaultCharset());
	futureBuffer.onComplete(readEvent -> {
		if (readEvent.succeeded())
		{
			Buffer buffer = readEvent.result();
			if (buffer.length() == 0)
			{
				promise.complete(false);
				return;
			}
				
			JsonObject obj = new JsonObject(buffer);
			System.out.println("jsonObject: " + obj.toString());

			JsonObject jsonUsersObject = obj.getJsonObject("Users");
			System.out.println("jsonUsersObject: " + jsonUsersObject.toString());
			
			JsonArray arr = jsonUsersObject.getJsonArray("User");
			System.out.println("JSONArray: " + arr.toString());
			
			for (int i = 0; i < arr.size(); i++)
			{
			    String persistedUserName = arr.getJsonObject(i).getString("Username");
			    System.out.println("persistedUserName: " + persistedUserName);
			    String persistedPassword = arr.getJsonObject(i).getString("Password");
			    System.out.println("persistedPassword: " + persistedPassword);
			    
			    if (persistedUserName.equals(userName) && persistedPassword.equals(password))
			    {
			    	System.out.println("success");
			    	promise.complete(true);
			    	return;
			    }  	
			}
			
			promise.complete(false);
			return;
		}	
		else
		{
			promise.complete(false);
			return;
		}
	});	
	
	return promise;
}

	//TODO: this must be replaced with vertx.fileSystem() for reading a file
	private Future<Buffer> readFile(String fileName, Charset encoding)
	{
		System.out.println("current directory: " + System.getProperty("user.dir"));
		//TODO: file shall be read using ClassLoader.getResource() and not in this way
		return _vertx.fileSystem().readFile(Paths.get("resources", fileName).toString());
	}
}

