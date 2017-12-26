/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.function;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.jenkins.appservice.BaseDeploymentRecorder;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import com.microsoft.jenkins.function.util.AzureUtils;
import com.microsoft.jenkins.function.util.Constants;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class FunctionAppDeploymentRecorder extends BaseDeploymentRecorder {

    @DataBoundConstructor
    public FunctionAppDeploymentRecorder(
            final String azureCredentialsId,
            final String resourceGroup,
            final String appName) {
        super(azureCredentialsId, resourceGroup, appName);
    }

    @Override
    public void perform(
            @Nonnull final Run<?, ?> run,
            @Nonnull final FilePath workspace,
            @Nonnull final Launcher launcher,
            @Nonnull final TaskListener listener) throws InterruptedException, IOException {
        // Only deploy on build succeeds
        // Also check if result is null here because in pipeline function app deploy is not run as a post-build action.
        // In this case result is null and pipeline will stop if previous step failed. So no need to check result in
        // this case.
        if (run.getResult() != null && run.getResult() != Result.SUCCESS && isDeployOnlyIfSuccessful()) {
            listener.getLogger().println("Deploy to Azure Function App is skipped due to previous steps failed.");
            return;
        }

        listener.getLogger().println("Starting Azure Function App Deployment");

        // Get app info
        final Azure azureClient = AzureUtils.buildClient(getAzureCredentialsId());
        final String resourceGroup = getResourceGroup();
        final String appName = getAppName();

        AzureFunctionPlugin.sendEvent(Constants.AI_FUNCTION_APP, Constants.AI_START_DEPLOY,
                "Run", AppInsightsUtils.hash(run.getUrl()),
                "Subscription", AppInsightsUtils.hash(azureClient.subscriptionId()),
                "ResourceGroup", AppInsightsUtils.hash(resourceGroup),
                "FunctionApp", AppInsightsUtils.hash(appName));

        final FunctionApp app = azureClient.appServices().functionApps().getByResourceGroup(resourceGroup, appName);
        if (app == null) {
            throw new AbortException(String.format("Function App %s in resource group %s not found",
                    appName, resourceGroup));
        }

        final String expandedFilePath = run.getEnvironment(listener).expand(getFilePath());
        final FunctionAppDeploymentCommandContext commandContext =
                new FunctionAppDeploymentCommandContext(expandedFilePath);
        commandContext.setSourceDirectory(getSourceDirectory());
        commandContext.setTargetDirectory(getTargetDirectory());

        try {
            commandContext.configure(run, workspace, launcher, listener, app);
        } catch (AzureCloudException e) {
            AzureFunctionPlugin.sendEvent(Constants.AI_FUNCTION_APP, Constants.AI_CONFIGURE_FAILED,
                    "Run", AppInsightsUtils.hash(run.getUrl()),
                    "Subscription", AppInsightsUtils.hash(azureClient.subscriptionId()),
                    "ResourceGroup", AppInsightsUtils.hash(resourceGroup),
                    "FunctionApp", AppInsightsUtils.hash(appName),
                    "Message", e.getMessage());

            throw new AbortException(e.getMessage());
        }

        commandContext.executeCommands();

        if (!commandContext.getLastCommandState().isError()) {
            listener.getLogger().println("Done Azure Function App deployment.");
        } else {
            throw new AbortException("Azure Function App deployment failed.");
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("azureFunctionAppPublish")
    public static final class DescriptorImpl extends BaseDeploymentRecorder.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "Publish an Azure Function App";
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath final Item owner) {
            return listAzureCredentialsIdItems(owner);
        }

        public ListBoxModel doFillResourceGroupItems(@QueryParameter final String azureCredentialsId) {
            return listResourceGroupItems(azureCredentialsId);
        }

        public ListBoxModel doFillAppNameItems(@QueryParameter final String azureCredentialsId,
                                               @QueryParameter final String resourceGroup) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = AzureUtils.buildClient(azureCredentialsId);
                for (FunctionApp app : azureClient.appServices().functionApps().listByResourceGroup(resourceGroup)) {
                    model.add(app.name());
                }
            }
            return model;
        }

    }
}
