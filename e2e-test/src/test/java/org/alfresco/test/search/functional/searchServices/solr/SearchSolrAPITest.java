/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.test.search.functional.searchServices.solr;

import javax.json.JsonArrayBuilder;
import java.net.URLEncoder;

import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.model.RestTextResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for solr/alfresco Solr API.
 * 
 * @author Meenal Bhave
 */
public class SearchSolrAPITest extends AbstractE2EFunctionalTest
{
    @Test(priority = 1)
    public void testGetSolrConfig()
    {
        RestTextResponse response = restClient.authenticateUser(adminUserModel).withSolrAPI().getConfig();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body(Matchers.containsString("config"));
        Assert.assertNotNull(response.getJsonValueByPath("config.requestHandler"));
        Assert.assertNotNull(response.getJsonObjectByPath("config.requestHandler"));

        // TODO: Following asserts fail with error: 
        /*
         * java.lang.IllegalStateException: Expected response body to be verified as JSON, HTML or XML but body-type 'text/plain' is not supported out of the box.
         * Try registering a custom parser using: RestAssured.registerParser("text/plain", <parser type>);
         */

        // response.assertThat().body("config.requestHandler", Matchers.notNullValue());
        // restClient.onResponse().assertThat().body("config.requestHandler",Matchers.notNullValue());
    }
    
    @Test(priority = 2)
    public void testEditSolrConfig()
    {
        String expectedError = "solrconfig editing is not enabled due to disable.configEdit";

        JsonArrayBuilder argsArray = JsonBodyGenerator.defineJSONArray();
        argsArray.add("ANYARGS");

        String postBody = JsonBodyGenerator.defineJSON()
            .add("add-listener", JsonBodyGenerator.defineJSON()
            .add("event", "postCommit")
            .add("name", "newlistener")
            .add("class", "solr.RunExecutableListener")
            .add("exe", "ANYCOMMAND")
            .add("dir", "/usr/bin/")
            .add("args", argsArray)).build().toString();

        // RestTextResponse response =
        restClient.authenticateUser(adminUserModel).withSolrAPI().postConfig(postBody);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);

        restClient.onResponse().assertThat().body(Matchers.containsString(expectedError));

        // TODO: Following asserts fail with error: 
        /*
         * java.lang.IllegalStateException: Expected response body to be verified as JSON, HTML or XML but body-type 'text/plain' is not supported out of the box.
         * Try registering a custom parser using: RestAssured.registerParser("text/plain", <parser type>);
         */

        // response.assertThat().body("error.msg", Matchers.contains(expectedError));
    }

    @Test(priority = 3)
    public void testGetSolrConfigOverlay()
    {
        restClient.authenticateUser(adminUserModel).withSolrAPI().getConfigOverlay();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body(Matchers.containsString("overlay"));
    }

    @Test(priority = 4)
    public void testGetSolrConfigParams()
    {
        restClient.authenticateUser(adminUserModel).withSolrAPI().getConfigParams();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body(Matchers.containsString("response"));
    }

    @Test(priority = 5)
    public void testGetSolrSelect() throws Exception
    {
        String queryParams = "{!xmlparser v='<!DOCTYPE a SYSTEM \"http://localhost:4444/executed\"><a></a>'}";

        String encodedQueryParams = URLEncoder.encode(queryParams, "UTF-8");
        
        restClient.authenticateUser(dataContent.getAdminUser()).withParams(encodedQueryParams).withSolrAPI().getSelectQuery();
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        String errorMsg = "No QueryObjectBuilder defined for node a in {q={!xmlparser";
        Assert.assertTrue(restClient.onResponse().getResponse().body().xmlPath().getString("response").contains(errorMsg));
    }
}
