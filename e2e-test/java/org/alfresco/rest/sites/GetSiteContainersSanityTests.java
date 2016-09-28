package org.alfresco.rest.sites;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { "rest-api", "sites", "sanity" })
public class GetSiteContainersSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private HashMap<UserRole, UserModel> usersWithRoles;

    @BeforeClass
    public void initTest() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                Arrays.asList(UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor));
    }

    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithManagerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteManager));
        siteAPI.getSiteContainers(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteCollaborator));
        siteAPI.getSiteContainers(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithContributorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteContributor));
        siteAPI.getSiteContainers(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site containers and gets status code OK (200)")
    public void getSiteContainersWithConsumerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.get(UserRole.SiteConsumer));
        siteAPI.getSiteContainers(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
    
    @TestRail(section = { "rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Admin user gets site containers information and gets status code OK (200)")
    public void getSiteContainersWithAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        siteAPI.getSiteContainers(siteModel.getId());
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK.toString());
    }
}
