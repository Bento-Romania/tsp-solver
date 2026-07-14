FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src/ src/
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
VOLUME /data
COPY --from=builder /build/target/tsp-solver-*.jar app.jar
ENV APP_GRAPHHOPPER_GRAPH_LOCATION=/data/graph-cache
ENV APP_GRAPHHOPPER_OSM_FILE=/data/romania-latest.osm.pbf
ENV APP_GRAPHHOPPER_OSM_DOWNLOAD_URL=https://download.geofabrik.de/europe/romania-latest.osm.pbf
ENV APP_GRAPHHOPPER_THREAD_POOL_SIZE=8
ENV APP_GRAPHHOPPER_PROFILE=car
ENV APP_GRAPHHOPPER_HAVERSINE_FALLBACK_SPEED_KMH=20
ENV APP_SECURITY_API_KEYS=""
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx4g", "-Xms512m", "-XX:+UseG1GC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
