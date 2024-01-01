# Extend vert.x image
FROM vertx/vertx4

#                                                       (1)
ENV VERTICLE_NAME com.restvert.RestVerticle
ENV VERTICLE_FILE target/restverticle-1.0.0-SNAPSHOT.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

# Copy your verticle to the container                   (2)
COPY ./verticles $VERTICLE_HOME

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]
