FROM hseeberger/scala-sbt:15.0.2_1.4.7_2.13.4 as builder

# fetch dependencies to speed up build process
COPY build.sbt .
COPY project project
RUN sbt update

# now copy the codebase and compile
COPY src src
RUN sbt clean compile assembly

# production image
FROM openjdk:11-jre-slim

COPY --from=builder /root/target/scala-2.13/cluster-chat-assembly-0.1.0-SNAPSHOT.jar /app/cluster-chat.jar

ENTRYPOINT ["java", "-cp", "/app/cluster-chat.jar", "dev.matiaspan.Main"]
