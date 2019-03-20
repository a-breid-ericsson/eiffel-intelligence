package com.ericsson.ei.utils;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;

/**
 * This class will create one embedded mongoDb instance
 * per JVM. This means that when running tests the fork
 * value should be set to true.
 * 
 * This also implies that only one test at a time can be run 
 * in your IDE. Use maven to run several tests.
 * 
 * @author evasiba
 *
 */
public class EmbeddedMongo {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestConfigs.class);
	
	private static MongodExecutable mongodExecutable;

	private static MongodProcess mongodProcess;
	
	private static MongoClient mongoClient;
	
	public static MongoClient newMongo() throws IOException {
		if (mongoClient != null)
			return mongoClient;
		
		String ip = "localhost";
		 int port = SocketUtils.findAvailableTcpPort();
 
        IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.V3_4_15)
            .net(new Net(ip, port, false))
            .build();
        System.setProperty("spring.data.mongodb.port", ""+port);
//        System.out.println("Starting mongodb port: " + port);
//        String stackTrace = Arrays.toString(new Exception().getStackTrace());
//        System.out.println(stackTrace);
               
		IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
			.defaultsWithLogger(Command.MongoD,LOGGER)
			.processOutput(ProcessOutput.getDefaultInstanceSilent())    			
			.build();
		
        MongodStarter starter = MongodStarter.getInstance(runtimeConfig);
        mongodExecutable = starter.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();
        mongoClient = new MongoClient(ip, port);
        return mongoClient;
	}
	
	public static void shutDown() {
		String mongoPort = System.getProperty("spring.data.mongodb.port");
//		System.out.println("Closing mongodb port: " + mongoPort);
		mongodProcess.stop();
		mongodExecutable.stop();
		mongoClient.close();
		mongoClient = null;
	}

}
