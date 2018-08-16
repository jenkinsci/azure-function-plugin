package com.microsoft.jenkins.function.integration;

import com.google.common.io.Files;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import com.microsoft.jenkins.function.commands.GitDeployCommand;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ITGitDeployCommand extends IntegrationTest{

    private GitDeployCommand command = null;
    private GitDeployCommand.IGitDeployCommandData commandDataMock = null;
    private AppServicePlan appServicePlan = null;
    private FilePath workspace = null;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        command = new GitDeployCommand();
        commandDataMock = mock(GitDeployCommand.IGitDeployCommandData.class);
        setUpBaseCommandMockErrorHandling(commandDataMock);

        Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());

        // Create resource group
        final ResourceGroup resourceGroup = azureClient.resourceGroups()
                .define(testEnv.azureResourceGroup)
                .withRegion(testEnv.azureLocation)
                .create();
        Assert.assertNotNull(resourceGroup);

        // Create app service plan
        appServicePlan = azureClient.appServices().appServicePlans()
                .define(testEnv.appServicePlanName)
                .withRegion(testEnv.azureLocation)
                .withNewResourceGroup(testEnv.azureResourceGroup)
                .withPricingTier(testEnv.appServicePricingTier)
                .withOperatingSystem(OperatingSystem.WINDOWS)
                .create();
        Assert.assertNotNull(appServicePlan);

        // Create workspace
        File workspaceDir = Files.createTempDir();
        workspaceDir.deleteOnExit();
        JobContext jobContextMock = mock(JobContext.class);
        when(commandDataMock.getJobContext()).thenReturn(jobContextMock);
        workspace = new FilePath(workspaceDir);
        when(commandDataMock.getJobContext().getWorkspace()).thenReturn(workspace);

        // Mock run
        final Run run = mock(Run.class);
        final EnvVars env = new EnvVars("BUILD_TAG", "jenkins-job-1");
        try {
            when(run.getEnvironment(any(TaskListener.class))).thenReturn(env);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        when(commandDataMock.getJobContext().getRun()).thenReturn(run);

        // Mock task listener
        final TaskListener listener = new StreamBuildListener(System.out, Charset.defaultCharset());
        when(commandDataMock.getJobContext().getTaskListener()).thenReturn(listener);
    }

    /**
     * This test deploys a NodeJS application
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void deployNodeJS() throws IOException, InterruptedException {
        final Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());
        final FunctionApp webApp = azureClient.appServices().functionApps()
                .define(testEnv.appServiceName)
                .withExistingAppServicePlan(appServicePlan)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .create();
        Assert.assertNotNull(webApp);
        when(commandDataMock.getPublishingProfile()).thenReturn(webApp.getPublishingProfile());
        when(commandDataMock.getWebAppBase()).thenReturn(webApp);

        Utils.extractResourceFile(getClass(), "sample-nodejs-func/host.json", workspace.child("host.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-nodejs-func/JSExample/function.json", workspace.child("JSExample/function.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-nodejs-func/JSExample/index.js", workspace.child("JSExample/index.js").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("**/*.js,**/*.json");

        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName()+"/api/JSExample?name=Azure"),"Hello Azure", 300);
    }

    /**
     * This test deploys a CSharp function
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void deployCSharp() throws IOException, InterruptedException {
        final Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());
        final FunctionApp webApp = azureClient.appServices().functionApps()
                .define(testEnv.appServiceName)
                .withExistingAppServicePlan(appServicePlan)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .create();
        Assert.assertNotNull(webApp);
        when(commandDataMock.getPublishingProfile()).thenReturn(webApp.getPublishingProfile());
        when(commandDataMock.getWebAppBase()).thenReturn(webApp);

        Utils.extractResourceFile(getClass(), "sample-csharp-func/host.json", workspace.child("host.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-csharp-func/CSharpExample/function.json", workspace.child("CSharpExample/function.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-csharp-func/CSharpExample/run.csx", workspace.child("CSharpExample/run.csx").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("**/*.csx,**/*json");

        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName()+"/api/CSharpExample?name=Azure"),"Hello Azure", 300);
    }

    /**
     * This test deploys a FSharp function
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void deployFSharp() throws IOException, InterruptedException {
        final Azure azureClient = AzureClientFactory.getClient(
                servicePrincipal.getClientId(),
                servicePrincipal.getClientSecret(),
                servicePrincipal.getTenant(),
                servicePrincipal.getSubscriptionId(),
                servicePrincipal.getAzureEnvironment());
        final FunctionApp webApp = azureClient.appServices().functionApps()
                .define(testEnv.appServiceName)
                .withExistingAppServicePlan(appServicePlan)
                .withExistingResourceGroup(testEnv.azureResourceGroup)
                .withPythonVersion(PythonVersion.PYTHON_34)
                .create();
        Assert.assertNotNull(webApp);
        when(commandDataMock.getPublishingProfile()).thenReturn(webApp.getPublishingProfile());
        when(commandDataMock.getWebAppBase()).thenReturn(webApp);

        Utils.extractResourceFile(getClass(), "sample-fsharp-func/FSharpExample/function.json", workspace.child("FSharpExample/function.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-fsharp-func/FSharpExample/project.json", workspace.child("FSharpExample/project.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-fsharp-func/FSharpExample/project.lock.json", workspace.child("FSharpExample/project.lock.json").getRemote());
        Utils.extractResourceFile(getClass(), "sample-fsharp-func/FSharpExample/run.fsx", workspace.child("FSharpExample/run.fsx").getRemote());
        Utils.extractResourceFile(getClass(), "sample-fsharp-func/host.json", workspace.child("host.json").getRemote());
        when(commandDataMock.getFilePath()).thenReturn("**/*.json,**/*.fsx");

        command.execute(commandDataMock);

        Utils.waitForAppReady(new URL("https://" + webApp.defaultHostName()+"/api/FSharpExample?name=Azure"),"Hello Azure", 300);
    }
}
