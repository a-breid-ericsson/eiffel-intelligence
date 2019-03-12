package com.ericsson.ei.utils;

import com.mongodb.BasicDBList;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import de.flapdoodle.embed.process.runtime.Network;

public class TestConfigs {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestConfigs.class);

    private MongoClient mongoClient = null;
    private MongodExecutable node1MongodExe;
    private MongodProcess node1Mongod;
    private MongoClient mongo;
    private MongodExecutable node2MongodExe;
    private MongodProcess node2Mongod;

    protected static Map<Integer, AMQPBrokerManager> amqpBrokerMap = new HashMap<>();

    public AMQPBrokerManager createAmqpBroker() throws Exception {
        // Generates a random port for amqpBroker and starts up a new broker
        int port = SocketUtils.findAvailableTcpPort();

        System.setProperty("rabbitmq.port", Integer.toString(port));
        System.setProperty("rabbitmq.user", "guest");
        System.setProperty("rabbitmq.password", "guest");
        System.setProperty("waitlist.initialDelayResend", "500");
        System.setProperty("waitlist.fixedRateResend", "3000");

        String config = "src/functionaltests/resources/configs/qpidConfig.json";
        File qpidConfig = new File(config);
        AMQPBrokerManager amqpBroker = new AMQPBrokerManager(qpidConfig.getAbsolutePath(), port);

        LOGGER.debug("Started embedded message bus for tests on port: " + port);
        amqpBroker.startBroker();

        // add new amqp broker to pool
        amqpBrokerMap.put(port, amqpBroker);

        return amqpBroker;
    }

    public void startUpMongoServerAndClient() throws IOException {
        //startUpMongoCluster();
        try {
            MongodForTestsFactory testsFactory = MongodForTestsFactory.with(Version.V3_4_5);
            mongoClient = testsFactory.newMongo();
            String port = "" + mongoClient.getAddress().getPort();
            System.setProperty("spring.data.mongodb.port", port);
            LOGGER.debug("Started embedded Mongo DB for tests on port: " + port);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void startUpMongoCluster() throws IOException {
        MongodStarter runtime = MongodStarter.getDefaultInstance();

        int node1Port = SocketUtils.findAvailableTcpPort();
        int node2Port = SocketUtils.findAvailableTcpPort();

        node1MongodExe = runtime
                .prepare(new MongodConfigBuilder().version(Version.Main.V4_0).withLaunchArgument("--replSet", "rs0")
                        .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
                        .net(new Net(node1Port, Network.localhostIsIPv6())).build());
        node1Mongod = node1MongodExe.start();

        node2MongodExe = runtime
                .prepare(new MongodConfigBuilder().version(Version.Main.V4_0).withLaunchArgument("--replSet", "rs0")
                        .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
                        .net(new Net(node2Port, Network.localhostIsIPv6())).build());
        node2Mongod = node2MongodExe.start();

        mongo = new MongoClient(new ServerAddress(InetAddress.getByName("localhost"), node1Port));

        MongoDatabase adminDatabase = mongo.getDatabase("admin");

        Document config = new Document("_id", "rs0");
        BasicDBList members = new BasicDBList();
        members.add(new Document("_id", 0).append("host", "localhost:" + node1Port));
        members.add(new Document("_id", 1).append("host", "localhost:" + node2Port));
        config.put("members", members);

        adminDatabase.runCommand(new Document("replSetInitiate", config));

//        System.out.println(">>>>>>>>" + adminDatabase.runCommand(new Document("replSetGetStatus", 1)));
//
//        MongoDatabase funDb = mongo.getDatabase("fun");
//        MongoCollection<Document> testCollection = funDb.getCollection("test");
//
//        System.out.println(">>>>>>>> inserting data");
//
//        testCollection.insertOne(new Document("fancy", "value"));
//
//        System.out.println(">>>>>>>> finding data");
//
//        assert(testCollection.find().first().get("fancy").equals("value"));
        System.setProperty("spring.data.mongodb.port", String.valueOf(node1Port));
    }

    public void closeMongoCluster() {
        System.out.println(">>>>>> shutting down");
        if (mongo != null) {
          mongo.close();
        }

        if (node1Mongod != null) {
          node1MongodExe.stop();
        }
        if (node1Mongod != null) {
          node1Mongod.stop();
        }
        if (node2Mongod != null) {
          node2MongodExe.stop();
        }
        if (node2Mongod != null) {
          node2Mongod.stop();
        }
    }

    public void setAuthorization() {
        String password = StringUtils.newStringUtf8(Base64.encodeBase64("password".getBytes()));
        System.setProperty("ldap.enabled", "true");
        System.setProperty("ldap.url", "ldap://ldap.forumsys.com:389/dc=example,dc=com");
        System.setProperty("ldap.base.dn", "dc=example,dc=com");
        System.setProperty("ldap.username", "cn=read-only-admin,dc=example,dc=com");
        System.setProperty("ldap.password", password);
        System.setProperty("ldap.user.filter", "uid={0}");
    }

    public static AMQPBrokerManager getBroker(int port) {
        return amqpBrokerMap.get(port);
    }

    public static void removeBroker(String port) {
        AMQPBrokerManager broker = amqpBrokerMap.get(Integer.parseInt(port));
        broker.stopBroker();
        amqpBrokerMap.remove(port);
    }
}