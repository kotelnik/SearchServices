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

package org.alfresco.test.search.functional.searchServices.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.search.FacetFieldBucket;
import org.alfresco.rest.search.FacetQuery;
import org.alfresco.rest.search.RestGenericBucketModel;
import org.alfresco.rest.search.RestGenericFacetResponseModel;
import org.alfresco.rest.search.RestRequestFacetFieldModel;
import org.alfresco.rest.search.RestRequestFacetFieldsModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.RestResultBucketsModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.search.TestGroup;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.testng.Assert;
import org.testng.TestException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Faceted search test.
 *
 * @author Michael Suzuki
 */
public class FacetedSearchTest extends AbstractSearchServicesE2ETest
{
    /**
     * Perform the below facet query.
     * {
     *    "query": {
     *      "query": "cars",
     *      "language": "afts"
     *    },
     *      "facetQueries": [
     *          {"query": "content.size:[o TO 102400]", "label": "small"},
     *          {"query": "content.size:[102400 TO 1048576]", "label": "medium"},
     *          {"query": "content.size:[1048576 TO 16777216]", "label": "large"}
     *    ],
     *      "facetFields": {"facets": [{"field": "'content.size'"}]}
     * }
     * 
     * Expected response
     * {"list": {
     *     "entries": [... All the results],
     *     "pagination": {
     *        "maxItems": 100,
     *        "hasMoreItems": false,
     *        "totalItems": 61,
     *        "count": 61,
     *        "skipCount": 0
     *     },
     *     "facetsFields": [
     *       {
     *         "type": "query",
     *         "label": "foo",
     *         "buckets": [
     *           {
     *             "label": "small",
     *             "filterQuery": "content.size:[0 TO 102400]",
     *             "display": 1
     *           },
     *           {
     *             "label": "large",
     *             "filterQuery": "content.size:[1048576 TO 16777216]",
     *             "metrics": [
     *               {
     *                 "type": "count",
     *                 "value": {
     *                   "count": 0
     *                 }
     *               }
     *             ]
     *           },
     * }}
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
        waitForContentIndexing(file4.getContent(), true);
    }

    @Test(groups={TestGroup.CONFIG_ENABLED_CASCADE_TRACKER})
    @TestRail(section = { TestGroup.REST_API, TestGroup.SEARCH}, executionType = ExecutionType.REGRESSION, description = "Checks facet queries for the Search api")
    public void searchWithQueryFaceting() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cars");
        query.setQuery(queryReq);

        List<FacetQuery> facets = new ArrayList<>();
        facets.add(new FacetQuery("content.size:[0 TO 102400]", "small"));
        facets.add(new FacetQuery("content.size:[102400 TO 1048576]", "medium"));
        facets.add(new FacetQuery("content.size:[1048576 TO 16777216]", "large"));
        query.setFacetQueries(facets);
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("'content.size'"));
        facetFields.setFacets(list);
        query.setFacetFields(facetFields);
        query.setIncludeRequest(true);
        
        SearchResponse response = query(query);
        
        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("facetQueries").isNotEmpty();
        
        FacetFieldBucket facet = response.getContext().getFacetQueries().get(0);
        facet.assertThat().field("label").contains("small").and().field("count").isGreaterThan(0);
        facet.assertThat().field("label").contains("small").and().field("filterQuery").is("content.size:[0 TO 102400]");
        Assert.assertEquals(response.getContext().getFacetQueries().size(), 1, "Results with count=0 must be omitted");

        // We don't expect to see the FacetFields if group is being used.
        Assert.assertNull(response.getContext().getFacetsFields());
        Assert.assertNull(response.getContext().getFacets());
    }
    
    /**
     * Verify this query is returning the same results for both single server and shard environments.
     * @throws Exception
     */
    @Test(groups={TestGroup.CONFIG_SHARDING})
    @TestRail(section = { TestGroup.REST_API, TestGroup.SEARCH}, executionType = ExecutionType.ACCEPTANCE, description = "Checks facet queries for the Search api in Shard environments")
    public void searchWithQueryFacetingCluster() throws Exception
    {
        searchWithQueryFaceting();
    }

    /**
     * * Perform a group by faceting, below test groups the facet by group name foo.
     * {
     *    "query": {
     *      "query": "cars",
     *      "language": "afts"
     *    },
     *      "facetQueries": [
     *          {"query": "content.size:[o TO 102400]", "label": "small","group":"foo"},
     *          {"query": "content.size:[102400 TO 1048576]", "label": "medium","group":"foo"},
     *          {"query": "content.size:[1048576 TO 16777216]", "label": "large","group":"foo"}
     *    ],
     *      "facetFields": {"facets": [{"field": "'content.size'"}]}
     * }
     *
     * Expected response
     * {"list": {
     *     "entries": [... All the results],
     *     "pagination": {
     *        "maxItems": 100,
     *        "hasMoreItems": false,
     *        "totalItems": 61,
     *        "count": 61,
     *        "skipCount": 0
     *     },
     *     "context": {
     *        "consistency": {"lastTxId": 512},
     *        //Added below as part of SEARCH-374
     *        "facets": [
     *          {  "label": "foo",
     *             "buckets": [
     *               { "label": "small", "count": 61, "filterQuery": "content.size:[o TO 102400]"},
     *               { "label": "large", "count": 0, "filterQuery": "content.size:[1048576 TO 16777216]"},
     *               { "label": "medium", "count": 61, "filterQuery": "content.size:[102400 TO 1048576]"}
     *             ]
     *          }
     *     }
     * }}
     */
    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
              description = "Checks facet queries for the Search api")
    public void searchQueryFacetingWithGroup() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cars");
        query.setQuery(queryReq);

        List<FacetQuery> facets = new ArrayList<>();
        facets.add(new FacetQuery("content.size:[0 TO 102400]", "small", "foo"));
        facets.add(new FacetQuery("content.size:[102400 TO 1048576]", "medium", "foo"));
        facets.add(new FacetQuery("content.size:[1048576 TO 16777216]", "large", "foo"));
        query.setFacetQueries(facets);

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> list = new ArrayList<>();
        list.add(new RestRequestFacetFieldModel("'content.size'"));
        facetFields.setFacets(list);
        query.setFacetFields(facetFields);
        
        SearchResponse response = query(query);
        
        // We don't expect to see the FacetQueries if group is being used.
        Assert.assertNull(response.getContext().getFacetQueries());
        // Validate the facet field structure is correct.
        Assert.assertFalse(response.getContext().getFacets().isEmpty());
        Assert.assertEquals(response.getContext().getFacets().get(0).getLabel(), "foo");

        RestGenericBucketModel bucket = response.getContext().getFacets().get(0).getBuckets().get(0);
        bucket.assertThat().field("label").isNotEmpty();
        Assert.assertEquals(bucket.getMetrics().get(0).getType(), "count");
        Assert.assertNotEquals(bucket.getMetrics().get(0).getValue(), "");
        bucket.assertThat().field("filterQuery").isNotEmpty();
        response.getContext().getFacets().get(0).getBuckets().forEach(action -> {
            switch (action.getLabel())
            {
                case "small":
                    Assert.assertEquals(action.getFilterQuery(), "content.size:[0 TO 102400]");
                    break;
                case "medium":
                    Assert.assertEquals(action.getFilterQuery(), "content.size:[102400 TO 1048576]");
                    break;
                case "large":
                    Assert.assertEquals(action.getFilterQuery(), "content.size:[1048576 TO 16777216]");
                    break;

                default:
                    throw new TestException("Unexpected value returned");
            }
        });

    }

    /**
     * {
     *  "query": {
     *              "query": "*"
     *           },
     *  "facetFields": {
     *      "facets": [{"field": "cm:mimetype"},{"field": "modifier"}]
     *  }
     * }
     */
    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
              description = "Checks facet queries for the Search api")
    public void searchWithFactedFields() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:content:" + unique_searchString);
        query.setQuery(queryReq);

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();
        facets.add(new RestRequestFacetFieldModel("cm:mimetype"));
        facets.add(new RestRequestFacetFieldModel("modifier"));
        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);

        SearchResponse response = query(query);

        Assert.assertFalse(response.getContext().getFacetsFields().isEmpty());
        Assert.assertNull(response.getContext().getFacetQueries());
        Assert.assertNull(response.getContext().getFacets());

        RestResultBucketsModel model = response.getContext().getFacetsFields().get(0);
        Assert.assertEquals(model.getLabel(), "modifier");

        model.assertThat().field("label").is("modifier");
        FacetFieldBucket bucket1 = model.getBuckets().get(0);
        bucket1.assertThat().field("label").is(testUser.getUsername());
        bucket1.assertThat().field("display").is("FN-" + testUser.getUsername() + " LN-" + testUser.getUsername());
        bucket1.assertThat().field("filterQuery").is("modifier:\"" + testUser.getUsername() + "\"");
        bucket1.assertThat().field("count").is(1);
    }

    /**
     * Test that items returned are in the format of generic facets.
     * {
     *  "query": {
     *              "query": "*"
     *           },
     *  "facetFields": {
     *      "facets": [{"field": "cm:mimetype"},{"field": "modifier"}]
     *  },
     *  "facetFormat":"V2"
     * }
     */
    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
              description = "Checks facet queries for the Search api")
    public void searchWithFactedFieldsFacetFormatV2() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:content:" + unique_searchString);
        query.setQuery(queryReq);
        query.setFacetFormat("V2");
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();
        facets.add(new RestRequestFacetFieldModel("cm:mimetype"));
        facets.add(new RestRequestFacetFieldModel("modifier"));
        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);

        SearchResponse response = query(query);

        Assert.assertNull(response.getContext().getFacetsFields());
        Assert.assertNull(response.getContext().getFacetQueries());
        Assert.assertFalse(response.getContext().getFacets().isEmpty());
        RestGenericFacetResponseModel model = response.getContext().getFacets().get(0);
        Assert.assertEquals(model.getLabel(), "modifier");

        model.assertThat().field("label").is("modifier");
        RestGenericBucketModel bucket1 = model.getBuckets().get(0);
        bucket1.assertThat().field("label").is(testUser.getUsername());
        bucket1.assertThat().field("display").is("FN-" + testUser.getUsername() + " LN-" + testUser.getUsername());
        bucket1.assertThat().field("filterQuery").is("modifier:\"" + testUser.getUsername() + "\"");
        bucket1.assertThat().field("metrics").is("[{type=count, value={count=1}, entry=null}]");
    }
    
    /**
     * Test that facet fields return results for single and multivalued fields.
     * {
     *  "query": {
     *              "query": "cm:addressee:'first'"
     *           },
     *  "facetFields": {
     *      "facets": [{"field": "cm:addressee"}, {"field": "cm:addressees"}]
     *  },
     *  "facetFormat":"V2"
     * }
     */
    @Test
    @TestRail(section = {TestGroup.REST_API, TestGroup.SEARCH }, executionType = ExecutionType.REGRESSION,
              description = "Checks facet queries for the Search api, single and multi-valued properties")
    public void searchWithMultiValuedFieldsFacet() throws Exception
    {
        
        // Create properties with single (cm:addressee) and multi-valued (cm:addressees) values
        FileModel emailFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Email");

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, emailFile.getName());
        properties.put(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, List.of("P:cm:emailed"));
        properties.put("cm:addressee", "first");
        properties.put("cm:addressees", List.of("first", "second"));

        cmisApi.authenticateUser(testUser)
                .usingSite(testSite)
                .usingResource(folder)
                .createFile(emailFile, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        String addresseeQuery = "cm:addressee:'first'";
        Assert.assertTrue(waitForIndexing(addresseeQuery, true));
        
        // Search facets fields cm:addressee and cm:addressess
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:addressee:'first'");
        query.setQuery(queryReq);
        query.setFacetFormat("V2");
        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();
        facets.add(new RestRequestFacetFieldModel("cm:addressee"));
        facets.add(new RestRequestFacetFieldModel("cm:addressees"));
        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);

        SearchResponse response = query(query);
        
        // Verify results
        Assert.assertNull(response.getContext().getFacetsFields());
        Assert.assertNull(response.getContext().getFacetQueries());
        Assert.assertFalse(response.getContext().getFacets().isEmpty());
        
        // Facets for cm:addressees (multi-valued)
        RestGenericFacetResponseModel model = response.getContext().getFacets().get(0);
        Assert.assertEquals(model.getLabel(), "cm:addressees");
        model.assertThat().field("label").is("cm:addressees");
        RestGenericBucketModel bucket = model.getBuckets().get(0);
        bucket.assertThat().field("label").is("{en}first");
        bucket.assertThat().field("filterQuery").is("cm:addressees:\"{en}first\"");
        bucket.assertThat().field("metrics").is("[{type=count, value={count=1}, entry=null}]");
        bucket = model.getBuckets().get(1);
        bucket.assertThat().field("label").is("{en}second");
        bucket.assertThat().field("filterQuery").is("cm:addressees:\"{en}second\"");
        bucket.assertThat().field("metrics").is("[{type=count, value={count=1}, entry=null}]");
        
        // Facets for cm:addressee (singel valued)
        model = response.getContext().getFacets().get(1);
        Assert.assertEquals(model.getLabel(), "cm:addressee");
        model.assertThat().field("label").is("cm:addressee");
        bucket = model.getBuckets().get(0);
        bucket.assertThat().field("label").is("{en}first");
        bucket.assertThat().field("filterQuery").is("cm:addressee:\"{en}first\"");
        bucket.assertThat().field("metrics").is("[{type=count, value={count=1}, entry=null}]");
        
    }
}
