# Builds a docker image from a locally built Maven war. Requires 'mvn package' to have been run beforehand
FROM eclipse-temurin:17-jre-alpine
LABEL Author="Europeana Foundation <development@europeana.eu>"

# Configure APM and add APM agent
ENV ELASTIC_APM_VERSION 1.34.1
#disabled by default, to be enabled by kustomize/build params for specific servers only
ENV ELASTIC_APM_ENABLED false
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /usr/local/elastic-apm-agent.jar

COPY ./translation-web/target/translation-web-executable.jar /opt/app/translation-web-executable.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/app/translation-web-executable.jar"]
