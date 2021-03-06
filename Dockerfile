FROM openjdk:8-jre-alpine

COPY build/libs/discord-relay-*-all.jar /usr/relay/discord-relay.jar
WORKDIR /usr/relay

ENTRYPOINT ["java", "-jar", "discord-relay.jar"]
