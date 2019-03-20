/*
   Copyright 2017 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ericsson.ei.mongoDBHandler.test;

import static org.junit.Assert.assertTrue;

import com.ericsson.ei.mongodbhandler.MongoDBHandler;
import com.ericsson.ei.utils.EmbeddedMongo;
import com.mongodb.MongoClient;

import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;

public class MongoDBHandlerTest {

    final static Logger log = LoggerFactory.getLogger(MongoDBHandlerTest.class);

    private static MongoDBHandler mongoDBHandler;

//    private MongodForTestsFactory testsFactory;
    private static MongoClient mongoClient = null;

    private String dataBaseName = "EventStorageDBbbb";
    private String collectionName = "SampleEvents";
    private String input = "{\"id\":\"eventId\",\"type\":\"eventType11\",\"test_cases\" : [{\"event_id\" : \"testcaseid1\", \"test_data\" : \"testcase1data\"},{\"event_id\" : \"testcaseid2\", \"test_data\" : \"testcase2data\"}]}";
    private String updateInput = "{\"id\":\"eventId\",\"type\":\"eventType11\",\"test_cases\" : [{\"event_id\" : \"testcaseid1\", \"test_data\" : \"testcase2data\"},{\"event_id\" : \"testcaseid3\", \"test_data\" : \"testcase3data\"}]}";
    private String condition = "{\"test_cases.event_id\" : \"testcaseid1\"}";

    public static void setUpEmbeddedMongo() throws Exception {
        try {
//            testsFactory = MongodForTestsFactory.with(Version.V4_0_2);
//            mongoClient = testsFactory.newMongo();
        	mongoClient = EmbeddedMongo.newMongo();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
    	setUpEmbeddedMongo();
        mongoDBHandler = new MongoDBHandler();
        mongoDBHandler.setMongoClient(mongoClient);
    }
    
    @Before
    public void init() throws Exception {        
        assertTrue(mongoDBHandler.insertDocument(dataBaseName, collectionName, input));
    }

    @Test
    public void testGetDocuments() {
        ArrayList<String> documents = mongoDBHandler.getAllDocuments(dataBaseName, collectionName);
        assertTrue(documents.size() > 0);
    }

    @Test
    public void testGetDocumentsOnCondition() {
        ArrayList<String> documents = mongoDBHandler.find(dataBaseName, collectionName, condition);
        assertTrue(documents.size() > 0);
    }

    // @Test
    // TODO fix this test case
    // public void testGetDocumentOnCondition(){
    // ArrayList<String> documents =
    // mongoDBHandler.getDocumentsOnCondition(dataBaseName, collectionName,
    // condition);
    // String document = documents.get(0);
    // assertEquals(document, input);
    // }

    @Test
    public void testUpdateDocument() {
        assertTrue(mongoDBHandler.updateDocument(dataBaseName, collectionName, input, updateInput));
    }

    @After
    public void dropCollection() {
        assertTrue(mongoDBHandler.dropDocument(dataBaseName, collectionName, condition));
//        mongoClient.close();
//        testsFactory.shutdown();

    }
    
    @AfterClass
    public static void afterClass() {
        EmbeddedMongo.shutDown();
    }
}
