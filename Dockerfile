FROM gradle:jdk11

WORKDIR /usr/src/app

ENV WORDLINK_DATABASE_URL mongodb://192.168.1.15:27018/
ENV WORDLINK_FRONTEND_DOMAIN wordlink-demo-b1.mozhok.me
ENV WORDLINK_SESSION_SECRET wordlink-session-secret
ENV WORDLINK_ALLOWED_ROOT 1
ENV WORDLINK_ROOT_PASSWORD root
ENV WORDLINK_PASSWORD_SECRET wordlink-pass-secret
ENV KTOR_ENV prod

COPY . .

RUN gradle shadowJar

EXPOSE 8000

#java -Xms1024M -Xmx2048M -server -Djava.awt.headless=true -XX:+DisableExplicitGC -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -XX:SurvivorRatio=16 -XX:UseSSE=3 -XX:ParallelGCThreads=4 -jar $SERVICE nogui -host=$HOST -port=$PORT
CMD ["java", "-server", "-Djava.awt.headless=true", "-XX:+DisableExplicitGC", "-XX:+UseParallelGC", "-XX:MaxGCPauseMillis=50","-XX:SurvivorRatio=16", "-XX:UseSSE=3", "-XX:ParallelGCThreads=4", "-jar", "./build/libs/wordlink-application-0.1.1.jar", "nogui", "-host=0.0.0.0", "-port=8000"]




