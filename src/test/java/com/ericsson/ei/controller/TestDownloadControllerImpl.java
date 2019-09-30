/*
   Copyright 2018 Ericsson AB.
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
package com.ericsson.ei.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.ei.App;
import com.ericsson.ei.utils.TestContextInitializer;

@TestPropertySource(properties = {
        "spring.data.mongodb.database: TestDownloadControllerImpl",
        "failed.notification.database-name: TestDownloadControllerImpl-failedNotifications",
        "rabbitmq.exchange.name: TestDownloadControllerImpl-exchange",
        "rabbitmq.consumerName: TestDownloadControllerImpl" })
@ContextConfiguration(
        classes = App.class,
        loader = SpringBootContextLoader.class,
        initializers = TestContextInitializer.class)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { App.class })
@AutoConfigureMockMvc
public class TestDownloadControllerImpl {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetDownload() throws Exception {
        JSONObject responseBody = new JSONObject();
        responseBody.put("subscriptions", "/download/subscriptionsTemplate");
        responseBody.put("rules", "/download/rulesTemplate");
        responseBody.put("events", "/download/eventsTemplate");
        mockMvc.perform(MockMvcRequestBuilders.get("/download").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(content().string(responseBody.toString())).andReturn();
    }

    @Test
    public void testGetSubscriptionsTemplate() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/download/subscriptionsTemplate").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    public void testGetRulesTemplate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/download/rulesTemplate").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    public void testGetEventsTemplate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/download/eventsTemplate").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();
    }
}