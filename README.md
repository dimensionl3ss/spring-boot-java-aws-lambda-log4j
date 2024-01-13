# Spring Boot Application [with Log4j] Deployment on AWS Lambda with Container Image OR Deployment Package.
We can deploy any spring application on AWS Lambda in two ways. 
1. Container Image
2. Deployment Package

## Prerequisites
- Java 17
- Maven 
- Spring Boot Version 3
- Docker
- Postman

## Configure Serverless Lambda and Log4j Library in the existing Spring boot application
### 1. Add Dependencies
Add a maven dependency to configure your application to support JSON-based logging.

	<dependency>
		<groupId>org.apache.logging.log4j</groupId>
		<artifactId>log4j-layout-template-json</artifactId>
	</dependency>

Add An appender library for Apache Log4j 2 that you can use to add the request ID for the current invocation to your function logs.
		
	<dependency>
		<groupId>com.amazonaws</groupId>
		<artifactId>aws-lambda-java-log4j2</artifactId>
		<version>1.5.1</version>
	</dependency>

Add a maven dependency to import the spring library for serverless invocations.
	
	<dependency>
	    <groupId>com.amazonaws.serverless</groupId>
	    <artifactId>aws-serverless-java-container-springboot3</artifactId>
	    <version>2.0.0-M2</version>
	</dependency>

### 2. Configure Log4j2
Create a log4j2.xml configuration for logging with lambda appender.

	<?xml version="1.0" encoding="UTF-8"?>
	<Configuration packages="com.amazonaws.services.lambda.runtime.log4j2">
	    <Appenders>
	        <Lambda name="lambda">
				<JsonTemplateLayout eventTemplateUri="classpath:EcsLayout.json"/>
	        </Lambda>
	    </Appenders>
	    <Loggers>
	        <!-- LOG everything at INFO level -->
	        <Root level="info">
	            <AppenderRef ref="lambda" />
	        </Root>
	    </Loggers>
	</Configuration>




 	


### 2. Create the Lambda handler
Create a lambda handler class that will handle your incoming API Gateway or ALB requests. The below snippet supports asynchronous initialization.</br>
Spring Boot 3 applications can be slow to start, particularly if they discover and initialize a lot of components. In the example above, we recommend using a static block or the constructor of your RequestStreamHandler class to initialize the framework to take advantage of the higher CPU available in AWS Lambda during the initialization phase. However, AWS Lambda limits the initialization phase to 10 seconds. If the application takes longer than 10 seconds to start, AWS Lambda will assume the sandbox is dead and attempt to start a new one. To make the most of the 10 seconds available in the initialization,
and still return control back to the Lambda runtime in a timely fashion, we support asynchronous initialization:


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
### 3. Package the application
We need two maven plugins for packaging the application.
- `maven-dependency-plugin`: This will copy the dependencies of the application into the `target/dependency` folder. we will need to copy the dependencies into the image container.

		<plugin>
		    <artifactId>maven-dependency-plugin</artifactId>
		    <executions>
		      <execution>
			<phase>install</phase>
			<goals>
			  <goal>copy-dependencies</goal>
			</goals>
			<configuration>
			  <outputDirectory>${project.build.directory}/dependency</outputDirectory>
			</configuration>
		      </execution>
		    </executions>
		</plugin>
   
- `maven-shade-plugin`: By default, Spring Boot projects include the `spring-boot-maven-plugin` and an embedded Tomcat application server. To package the Spring Boot application for AWS Lambda, we do not need the Spring Boot maven plugin and we can configure the shade plugin to exclude the embedded Tomcat - the serverless-java-container library takes its place.
If you use the appender library (`aws-lambda-java-log4j2`), you must also configure a transformer for the Maven Shade plugin. The transformer library combines versions of a cache file that appear in both the appender library and in Log4j. Also, maven-shade-plugin may cause to ignore MANIFEST.MF. You will need to add a manifest resource transformer identifying your main class.

		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-shade-plugin</artifactId>
			<executions>
				<execution>
					<id>shade-jar-with-dependencies</id>
					<phase>package</phase>
					<goals>
						<goal>shade</goal>
					</goals>
					<configuration>
						<createDependencyReducedPom>false</createDependencyReducedPom>
						<transformers>
							<transformer
								implementation="io.github.edwgiz.log4j.maven.plugins.shade.transformer.Log4j2PluginCacheFileTransformer">
							</transformer>
							<transformer
								implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
								<manifestEntries>
									<Main-Class>
										com.adarsh.SpringJavaLambdaLog4jApplication</Main-Class>
								</manifestEntries>
							</transformer>
						</transformers>
						<artifactSet>
							<excludes>
								<exclude>org.apache.tomcat.embed:*</exclude>
							</excludes>
						</artifactSet>
					</configuration>
				</execution>
			</executions>
			<dependencies>
				<dependency>
					<groupId>io.github.edwgiz</groupId>
					<artifactId>log4j-maven-shade-plugin-extensions</artifactId>
					<version>2.20.0</version>
				</dependency>
			</dependencies>
		</plugin>

### 4. Deploy on AWS Lambda

#### 4.1. Deploy using the deployment package.
Build your deployment package using the `mvn clean package` command. It will create a shaded jar (with shade suffix) of the application. You can now upload this jar in your AWS Lambda function directly or put this jar into the S3 bucket and upload the S3 Key URL into the Lambda function.

#### 4.2. Create an Image using Dockerfile.
- Create a Dockerfile to create a Docker container image and push this image into AWS ECR.

		FROM public.ecr.aws/lambda/java:17
		# Copy function code and runtime dependencies from Maven layout
		COPY target/classes ${LAMBDA_TASK_ROOT}
		COPY target/dependency/* ${LAMBDA_TASK_ROOT}/lib/
		    
		# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
		CMD ["com.adarsh.LambdaHandler.StreamLambdaHandler::handleRequest"]

	Now use the image from AWS ECR to create a Lambda function. </br>
	Note: The Amazon ECR repository must be in the same AWS Region as the Lambda function.

- Create and Test the Docker image locally
	1. Create the Docker image using the command `docker build -t spring-lambda:test .`
 	2. Run the image as a container using the command `docker run -p 9000:8080 spring-lambda:test`
  	3. Send a `GET` request to `http://localhost:9000/2015-03-31/functions/function/invocations` using Postman. Use below JSON object as request body for the request.
  	   
			{
			    "requestContext": {
			        "elb": {
			            "targetGroupArn": "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/lambda-279XGJDqGZ5rsrHC2Fjr/49e9d65c45c6791a"
			        }
			    },
			    "httpMethod": "GET",
			    "path": "/api/hello",
			    "queryStringParameters": {
			        "query": "1234ABCD"
			    },
			    "headers": {
			        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
			        "accept-encoding": "gzip",
			        "accept-language": "en-US,en;q=0.9",
			        "connection": "keep-alive",
			        "host": "lambda-alb-123578498.us-east-1.elb.amazonaws.com",
			        "upgrade-insecure-requests": "1",
			        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36",
			        "x-amzn-trace-id": "Root=1-5c536348-3d683b8b04734faae651f476",
			        "x-forwarded-for": "72.12.164.125",
			        "x-forwarded-port": "80",
			        "x-forwarded-proto": "http",
			        "x-imforwards": "20"
			    },
			    "body": "",
			    "isBase64Encoded": false
			}

     	Expected Response:

			{
				"statusCode":200,
				"statusDescription":"200 OK",
				"multiValueHeaders":{
					"Content-Length":["13"],
					"Content-Type":["text/plain; charset=UTF-8"]
					},
				"body":"Hello World!!",
				"isBase64Encoded":false
			}
			 
	
	
	
	
	
	
