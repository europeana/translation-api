# Builds a docker image from a locally built Maven war. Requires 'mvn package' to have been run beforehand
#FROM eclipse-temurin:17-jre-alpine
FROM tomcat:10-jre21
LABEL Author="Europeana Foundation <development@europeana.eu>"

# Configure APM and add APM agent
ARG ELASTIC_APM_VERSION 1.56.0
#ENV arg is probably  
ENV APM_URL https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/${ELASTIC_APM_VERSION}/elastic-apm-agent-${ELASTIC_APM_VERSION}.jar

ADD ${APM_URL} /usr/local/elastic-apm-agent.jar

COPY ./translation-web/target/translation-web-executable.jar /opt/app/translation-web-executable.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/app/translation-web-executable.jar"]
