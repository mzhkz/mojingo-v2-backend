FROM gradle:jdk11

#ENV MOJINGO_METADATA_KEY_SECRET meta_secret
#ENV MOJINGO_COOKIE_SECRET cookie_secret

WORKDIR /usr/src/app

ENV BUILD_APP: ./build/libs/wordlink_backend_app.jar # 成果物
ENV USER_NAME: mojingo-backend  # デプロイ先のユーザー名

ENV WORDLINK_DATABASE_HOST mojingo-pp43m.gcp.mongodb.net
ENV WORDLINK_DATABASE_PORT 27017
ENV WORDLINK_DATABASE_NAME wordlink
ENV WORDLINK_DATABASE_USER application
ENV KTOR_ENV prod



COPY . .

RUN gradle dependencies
RUN gradle jar

EXPOSE 8000

#java -Xms1024M -Xmx2048M -server -Djava.awt.headless=true -XX:+DisableExplicitGC -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -XX:SurvivorRatio=16 -XX:UseSSE=3 -XX:ParallelGCThreads=4 -jar $SERVICE nogui -host=$HOST -port=$PORT
CMD ["java", "-Xms512M", "-Xmx1024M", "-server", "-Djava.awt.headless=true", "-XX:+DisableExplicitGC", "-XX:+UseParallelGC", "-XX:MaxGCPauseMillis=50","-XX:SurvivorRatio=16", "-XX:UseSSE=3", "-XX:ParallelGCThreads=4", "-jar", "./build/libs/wordlink_backend_app.jar", "nogui", "-host=0.0.0.0", "-port=8000"]




