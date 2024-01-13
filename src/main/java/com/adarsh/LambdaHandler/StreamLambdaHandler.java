package com.adarsh.LambdaHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.adarsh.SpringJavaLambdaLog4jApplication;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class StreamLambdaHandler implements RequestStreamHandler {
	private static Logger logger = LogManager.getLogger(StreamLambdaHandler.class); 
    private static final SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    static {
        try {
            
            handler = SpringLambdaContainerHandler.getAwsProxyHandler(SpringJavaLambdaLog4jApplication.class);
        } catch (ContainerInitializationException e) {
            // if we fail here. We re-throw the exception to force another cold start
        	logger.error("Could not initialize Spring framework");
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring framework", e);
        }
    }
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
    
    
}
