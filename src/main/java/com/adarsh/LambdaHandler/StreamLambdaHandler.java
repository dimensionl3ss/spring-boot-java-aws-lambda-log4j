package com.adarsh.LambdaHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.adarsh.SpringJavaLambdaLog4jApplication;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class StreamLambdaHandler implements RequestStreamHandler {
    private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public StreamLambdaHandler() throws ContainerInitializationException {
        handler = ((SpringBootProxyHandlerBuilder) new SpringBootProxyHandlerBuilder()
                .defaultProxy()
                .asyncInit())
                .springBootApplication(SpringJavaLambdaLog4jApplication.class)
                .buildAndInitialize();
    }
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
