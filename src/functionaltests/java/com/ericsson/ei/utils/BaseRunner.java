package com.ericsson.ei.utils;

import org.junit.AfterClass;

public class BaseRunner {

    @AfterClass()
    public static void shutdownAmqpBroker() {
        String rabbitMQPort = System.getProperty("rabbitmq.port");
        String mongoPort = System.getProperty("spring.data.mongodb.port");
        System.out.println("Closing mongodb port: " + mongoPort + " and rabbitMQ : " + rabbitMQPort );
        TestContextInitializer.removeBroker(rabbitMQPort);
        TestContextInitializer.closeMongo();
    }
}
