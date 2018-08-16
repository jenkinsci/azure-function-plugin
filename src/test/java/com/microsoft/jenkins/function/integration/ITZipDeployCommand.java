package com.microsoft.jenkins.function.integration;

import com.google.common.io.Files;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import com.microsoft.jenkins.function.commands.ZipDeployCommand;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ITZipDeployCommand extends IntegrationTest{
    private ZipDeployCommand command = null;
    private ZipDeployCommand.IZipDeployCommandData commandDataMock = null;
    private WebAppBase function = null;
    private FilePath workspace = null;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        command = new ZipDeployCommand();
        commandDataMock = mock(ZipDeployCommand.IZipDeployCommandData.class);
        JobContext jobContextMock = mock(JobContext.class);
        when(commandDataMock.getJobContext()).thenReturn(jobContextMock);
        StreamBuildListener listener = new StreamBuildListener(System.out, Charset.defaultCharset());
        when(commandDataMock.getJobContext().getTaskListener()).thenReturn(listener);
        setUpBaseCommandMockErrorHandling(commandDataMock);

        Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());

        // Setup function
        final ResourceGroup resourceGroup = azureClient.resourceGroups()
                .define(testEnv.azureResourceGroup)
                .withRegion(testEnv.azureLocation)
                .create();
        Assert.assertNotNull(resourceGroup);

        final AppServicePlan asp = azureClient.appServices().appServicePlans()
                .define(testEnv.appServicePlanName)
                .withRegion(testEnv.azureLocation)
                .withNewResourceGroup(testEnv.azureResourceGroup)
                .withPricingTier(testEnv.appServicePricingTier)
                .withOperatingSystem(OperatingSystem.WINDOWS)
                .create();
        Assert.assertNotNull(asp);

        function = azureClient.appServices().functionApps()
                .define(testEnv.appServiceName)
                .withExistingAppServicePlan(asp)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .create();
        Assert.assertNotNull(function);
        when(commandDataMock.getWebAppBase()).thenReturn(function);


        File workspaceDir = Files.createTempDir();
        workspaceDir.deleteOnExit();
        workspace = new FilePath(workspaceDir);

        final Run run = mock(Run.class);
        when(commandDataMock.getJobContext().getRun()).thenReturn(run);
        when(commandDataMock.getJobContext().getWorkspace()).thenReturn(workspace);
    }

    /**
     *  This test uploads a zip file to deploy java app and verifies web page content
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void zipDeploy() throws IOException, InterruptedException {
        Utils.extractResourceFile(getClass(), "sample-java-func/host.json", workspace.child("host.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-java-func/jenkins-function-1.0-SNAPSHOT.jar", workspace.child("jenkins-function-1.0-SNAPSHOT.jar").getRemote());
        Utils.extractResourceFile(getClass(), "sample-java-func/HttpTrigger-Java/function.json", workspace.child("HttpTrigger-Java/function.json").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("*.zip");

        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + function.defaultHostName()), "Greetings from Spring Boot!", 300);
    }
}
