FROM public.ecr.aws/lambda/java:11
  
# Copy function code and runtime dependencies from Maven layout
COPY target/spring-java-lambda-log4j-0.0.1-SNAPSHOT-shaded ${LAMBDA_TASK_ROOT}
    
# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD ["com.adarsh.LambdaHandler.StreamLambdaHandler::handleRequest"]