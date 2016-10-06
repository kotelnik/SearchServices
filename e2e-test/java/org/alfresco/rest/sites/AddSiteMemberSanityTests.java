package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.SiteMember;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "sites", "sanity" })
public class AddSiteMemberSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);

    }

    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify that manager is able to update site member and gets status code OK (200)")
    public void managerIsAbleToAddSiteMember() throws Exception
    {
        UserModel newUser = dataUser.createRandomTestUser();
        SiteMember siteMember = new SiteMember(Role.SiteConsumer.toString(), newUser.getUsername());
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteAPI.addPerson(siteModel, siteMember);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);       
    }
}
