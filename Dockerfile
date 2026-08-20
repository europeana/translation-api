# Builds a docker image from a locally built Maven war. Requires 'mvn package' to have been run beforehand
#FROM eclipse-temurin:17-jre-alpine
FROM tomcat:10-jre21
LABEL Author="Europeana Foundation <development@europeana.eu>"

# Configure APM and add APM agent
# requires 
COPY ./misc/apm/ /usr/local/

COPY ./translation-web/target/translation-web-executable.jar /opt/app/translation-web-executable.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/app/translation-web-executable.jar"]
