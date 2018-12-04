/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.function.commands;

import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.function.AzureFunctionPlugin;
import com.microsoft.jenkins.function.util.Constants;
import hudson.FilePath;
import hudson.Util;
import hudson.util.DirScanner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;

public class ZipDeployCommand implements ICommand<ZipDeployCommand.IZipDeployCommandData> {
    private static final String ZIP_FOLDER_NAME = "fileArchive";
    private static final String ZIP_NAME = "archive.zip";
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";

    @Override
    public void execute(IZipDeployCommandData context) {
        FilePath workspace = context.getJobContext().getWorkspace();
        String filePattern = Util.fixNull(context.getFilePath());

        final FilePath tempDir;
        try {
            tempDir = workspace.createTempDir(ZIP_FOLDER_NAME, null);
            final FilePath zipPath = tempDir.child(ZIP_NAME);
            final DirScanner.Glob globScanner = new DirScanner.Glob(filePattern, excludedFilesAndZip());
            final FilePath sourceDir = workspace.child(Util.fixNull(context.getSourceDirectory()));
            int count = sourceDir.zip(zipPath.write(), globScanner);
            context.logStatus(String.format("Archive %d target files under %s", count, sourceDir.getRemote()));

            WebAppBase functionApp = context.getWebAppBase();
            try (InputStream stream = zipPath.read()) {
                functionApp.zipDeploy(stream);
            }
            context.logStatus("Deploy to function " + functionApp.name() + " using file: " + zipPath.getRemote());
            context.logStatus("Tmp file location " + tempDir.getRemote());
            tempDir.deleteRecursive();

            context.setCommandState(CommandState.Success);
            AzureFunctionPlugin.sendEvent(Constants.AI_FUNCTION_APP, Constants.AI_ZIP_DEPLOY,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebAppBase().resourceGroupName()),
                    "FunctionApp", AppInsightsUtils.hash(context.getWebAppBase().name()));
        } catch (IOException e) {
            context.logError("Fail to deploy to zip: ", e);
            AzureFunctionPlugin.sendEvent(Constants.AI_FUNCTION_APP, Constants.AI_ZIP_DEPLOY_FAILED,
                    "Run", AppInsightsUtils.hash(context.getJobContext().getRun().getUrl()),
                    "ResourceGroup", AppInsightsUtils.hash(context.getWebAppBase().resourceGroupName()),
                    "FunctionApp", AppInsightsUtils.hash(context.getWebAppBase().name()),
                    "Message", e.getMessage());
        } catch (InterruptedException e) {
            context.logError("Interrupted: ", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Make sure we exclude the tempPath and local setting file from archiving.
     * @return excluded files in ant pattern
     */
    private String excludedFilesAndZip() {
        String excludesWithoutZip = "**/" + ZIP_FOLDER_NAME + "*/" + ZIP_NAME;
        excludesWithoutZip = LOCAL_SETTINGS_FILE + "," + excludesWithoutZip;
        return excludesWithoutZip;
    }

    public interface IZipDeployCommandData extends IBaseCommandData {
        String getFilePath();

        String getSourceDirectory();

        String getTargetDirectory();

        WebAppBase getWebAppBase();
    }
}
