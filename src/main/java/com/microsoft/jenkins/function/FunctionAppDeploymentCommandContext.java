/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import com.microsoft.jenkins.function.commands.GitDeployCommand;
import com.microsoft.jenkins.function.commands.ZipDeployCommand;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.io.IOException;
import java.io.InputStream;

public class FunctionAppDeploymentCommandContext extends BaseCommandContext
        implements ZipDeployCommand.IZipDeployCommandData, GitDeployCommand.IGitDeployCommandData {

    private final String filePath;
    private String sourceDirectory;
    private String targetDirectory;
    private PublishingProfile pubProfile;
    private FunctionApp functionApp;

    public FunctionAppDeploymentCommandContext(final String filePath) {
        this.filePath = filePath;
        this.sourceDirectory = "";
        this.targetDirectory = "";
    }

    public void setSourceDirectory(final String sourceDirectory) {
        this.sourceDirectory = Util.fixNull(sourceDirectory);
    }

    public void setTargetDirectory(final String targetDirectory) {
        this.targetDirectory = Util.fixNull(targetDirectory);
    }

    public void configure(
            final Run<?, ?> run,
            final FilePath workspace,
            final Launcher launcher,
            final TaskListener listener,
            final FunctionApp app) throws AzureCloudException {
        this.functionApp = app;

        pubProfile = app.getPublishingProfile();

        boolean isJava = false;
        try {
            isJava = isJavaFunction(workspace, sourceDirectory, filePath);
        } catch (IOException | InterruptedException e) {
            throw new AzureCloudException(e);
        }

        CommandService.Builder builder = CommandService.builder();
        if (isJava) {
            // For Java function, use FTP-based deployment as it's the recommended way
            builder.withStartCommand(ZipDeployCommand.class);
        } else {
            // For non-Java function, use Git-based deployment
            builder.withStartCommand(GitDeployCommand.class);
        }

        final JobContext jobContext = new JobContext(run, workspace, launcher, listener);
        super.configure(jobContext, builder.build());
    }

    static boolean isJavaFunction(final FilePath workspace, final String sourceDirectory, final String filePath)
            throws IOException, InterruptedException {
        FilePath sourceDir = workspace.child(Util.fixNull(sourceDirectory));
        FilePath[] files = sourceDir.list(filePath);

        for (final FilePath file : files) {
            String fileName = file.getName();
            if (fileName.equals("function.json")) {
                String scriptPath = getScriptFileFromConfig(file);
                if (scriptPath.toLowerCase().endsWith(".jar")) {
                    return true;
                }
            }
        }

        return false;
    }

    static String getScriptFileFromConfig(final FilePath filePath) throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream = filePath.read()) {
            JsonNode root = mapper.readTree(stream);
            JsonNode scriptPathNode = root.get("scriptFile");
            if (scriptPathNode == null) {
                return "";
            }

            return scriptPathNode.asText("");
        }
    }

    @Override
    public StepExecution startImpl(StepContext stepContext) throws Exception {
        return null;
    }

    @Override
    public IBaseCommandData getDataForCommand(final ICommand command) {
        return this;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getSourceDirectory() {
        return sourceDirectory;
    }

    @Override
    public String getTargetDirectory() {
        return targetDirectory;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return pubProfile;
    }

    @Override
    public WebAppBase getWebAppBase() {
        return functionApp;
    }
}
