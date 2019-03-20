package com.ericsson.ei.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

import com.mongodb.BasicDBList;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;

public class TestConfigs {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestConfigs.class);

//    private MongoClient mongoClient = null;
    private static MongodExecutable node1MongodExe;
    private static MongodProcess node1Mongod;
    private static MongoClient mongo;
    private static MongodExecutable node2MongodExe;
    private static MongodProcess node2Mongod;

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

    public void startUpMongoServerAndClient() throws Exception {
        startUpMongoCluster();
//        try {
//            MongodForTestsFactory testsFactory = MongodForTestsFactory.with(Version.V3_4_5);
//            mongoClient = testsFactory.newMongo();
//            String port = "" + mongoClient.getAddress().getPort();
//            System.setProperty("spring.data.mongodb.port", port);
//            LOGGER.debug("Started embedded Mongo DB for tests on port: " + port);
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }
    }

    public void startUpMongoCluster() throws Exception {
    	try {

    		IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
    			.defaultsWithLogger(Command.MongoD,LOGGER)
    			.processOutput(ProcessOutput.getDefaultInstanceSilent())    			
    			.build();
    		
    		MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
    		
    		int node1Port = SocketUtils.findAvailableTcpPort();
    		int node2Port = SocketUtils.findAvailableTcpPort();
    		System.out.println("Faejked ERROR:Trying to start embedded message bus for tests on ports: " + node1Port + ", " + node2Port);
    		node1MongodExe = runtime
    				.prepare(new MongodConfigBuilder().version(Version.Main.V3_4).withLaunchArgument("--replSet", "rs0")
    						.cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
    						.net(new Net(node1Port, false)).build());
    		node1Mongod = node1MongodExe.start();

    		node2MongodExe = runtime
    				.prepare(new MongodConfigBuilder().version(Version.Main.V3_4).withLaunchArgument("--replSet", "rs0")
    						.cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
    						.net(new Net(node2Port, false)).build());
    		node2Mongod = node2MongodExe.start();
    		
    		mongo = new MongoClient("localhost", node1Port);

    		MongoDatabase adminDatabase = mongo.getDatabase("admin");

    		Document config = new Document("_id", "rs0");
    		BasicDBList members = new BasicDBList();
    		members.add(new Document("_id", 0).append("host", "localhost:" + node1Port));
    		members.add(new Document("_id", 1).append("host", "localhost:" + node2Port));
    		config.put("members", members);

    		adminDatabase.runCommand(new Document("replSetInitiate", config));
    		System.out.println("Faejked ERROR:Started embedded message bus for tests on ports: " + node1Port + ", " + node2Port);

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
    	} catch (Exception e) {
    		closeMongoCluster();
    		throw e;
    	}
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
    
    public static void closeMongo() {
    	mongo.close();
    	node1Mongod.stop();
    	node1MongodExe.stop();    	    
    }
}