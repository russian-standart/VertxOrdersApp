# Extend vert.x image
FROM vertx/vertx3

#                                                       (1)
ENV VERTICLE_NAME com.ordersvert.OrderVerticle
ENV VERTICLE_FILE target/ordersverticle-1.0.0-SNAPSHOT-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

# Copy your verticle to the container                   (2)
COPY ./verticles $VERTICLE_HOME

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]
