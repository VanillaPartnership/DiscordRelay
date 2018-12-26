FROM openjdk:8-jre-alpine

COPY target/discord-relay-*.jar /usr/relay/discord-relay.jar
WORKDIR /usr/relay

ENTRYPOINT ["java", "-jar", "discord-relay.jar"]