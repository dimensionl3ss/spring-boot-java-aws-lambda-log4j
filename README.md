# Spring Boot Application [with Log4j] Deployment on AWS Lambda with Container Image

### Prerequisites
- Java 17
- Maven 
- Spring Boot Version 3
- Docker 

### Configure Serverless Lambda in existing Spring boot application

#### 1. Add Dependency

First step is to add maven dependency for spring implementation

	<dependency>
	    <groupId>com.amazonaws.serverless</groupId>
	    <artifactId>aws-serverless-java-container-springboot3</artifactId>
	    <version>2.0.0-M2</version>
	</dependency>


#### 2. Create the Lambda handler