package com.adarsh.LambdaHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

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
    private static final Properties properties = new Properties();
    static {
        try {
        	
        	/*When you create your Lambda deployment package (usually a JAR or a ZIP file), 
        	 *include the application.properties file at the root of your deployment package.
        	 *(When you create the shaded jar using maven-shade-plugin the jar will include application.properties file.)
        	 *
        	 *In your Lambda function code, you can use the Properties class to read and access the properties.
        	 *If application.properties file includes database configuration for MySQL, 
        	 *the MySQL Connector/J itself will not automatically configure these properties for you. 
        	 *You need to explicitly load and use these properties in your Lambda function code.
        	 */
        	
            // Load the application.properties file
            InputStream input = StreamLambdaHandler.class.getClassLoader().getResourceAsStream("application.properties");
            properties.load(input);
            
            handler = SpringLambdaContainerHandler.getAwsProxyHandler(SpringJavaLambdaLog4jApplication.class);
        } catch (ContainerInitializationException e) {
            // if we fail here. We re-throw the exception to force another cold start
        	logger.error("Could not initialize Spring framework");
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring framework", e);
        } catch (IOException e) {
        	logger.error("Failed to load application.properties");
            e.printStackTrace();
            throw new RuntimeException("Failed to load application.properties");
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
    	logger.info("Handling Request for" + properties.getProperty("spring.application.name"));
        handler.proxyStream(inputStream, outputStream, context);
    }
    
    
}
