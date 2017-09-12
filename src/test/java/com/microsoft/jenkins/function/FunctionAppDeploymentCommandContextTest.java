/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.exceptions.AzureCloudException;
import com.microsoft.jenkins.function.commands.FTPDeployCommand;
import com.microsoft.jenkins.function.commands.GitDeployCommand;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FunctionAppDeploymentCommandContextTest {

    @Rule
    public TemporaryFolder workspaceDir = new TemporaryFolder();

    @Test
    public void getterSetter() throws AzureCloudException, IOException {
        FunctionAppDeploymentCommandContext ctx = new FunctionAppDeploymentCommandContext("**/*.js,**/*.json");

        Assert.assertEquals("", ctx.getSourceDirectory());
        Assert.assertEquals("", ctx.getTargetDirectory());
        Assert.assertEquals("**/*.js,**/*.json", ctx.getFilePath());
        Assert.assertFalse(ctx.getLastCommandState().isError());
        Assert.assertFalse(ctx.getLastCommandState().isFinished());
        Assert.assertEquals(CommandState.Unknown, ctx.getLastCommandState());

        File srcDir = workspaceDir.newFolder();
        ctx.setSourceDirectory(srcDir.getPath());
        Assert.assertEquals(srcDir.getPath(), ctx.getSourceDirectory());

        ctx.setTargetDirectory("target");
        Assert.assertEquals("target", ctx.getTargetDirectory());

        final PublishingProfile pubProfile = mock(PublishingProfile.class);
        when(pubProfile.ftpUrl()).thenReturn("ftp://example.com");
        when(pubProfile.ftpUsername()).thenReturn("user");
        when(pubProfile.ftpPassword()).thenReturn("pass");

        final Run run = mock(Run.class);
        final TaskListener listener = mock(TaskListener.class);
        final Launcher launcher = mock(Launcher.class);
        final FilePath workspace = new FilePath(workspaceDir.getRoot());
        final FunctionApp app = mock(FunctionApp.class);
        when(app.getPublishingProfile()).thenReturn(pubProfile);

        ctx.configure(run, workspace, launcher, listener, app);

        Assert.assertEquals(workspace, ctx.getJobContext().getWorkspace());
        Assert.assertEquals("ftp://example.com", ctx.getPublishingProfile().ftpUrl());
        Assert.assertEquals("user", ctx.getPublishingProfile().ftpUsername());
        Assert.assertEquals("pass", ctx.getPublishingProfile().ftpPassword());
    }

    @Test
    public void configureNonJava() throws AzureCloudException {
        FunctionAppDeploymentCommandContext ctx = new FunctionAppDeploymentCommandContext("**/*.js,**/*.json");

        final Run run = mock(Run.class);
        final FilePath workspace = new FilePath(workspaceDir.getRoot());
        final Launcher launcher = mock(Launcher.class);
        final TaskListener listener = mock(TaskListener.class);
        final FunctionApp app = mock(FunctionApp.class);

        ctx.configure(run, workspace, launcher, listener, app);
        Assert.assertEquals(1, ctx.getCommandService().getRegisteredCommands().size());
        Assert.assertEquals(GitDeployCommand.class, ctx.getCommandService().getStartCommandClass());
    }

    @Test
    public void configureJava() throws AzureCloudException, IOException {
        FunctionAppDeploymentCommandContext ctx = new FunctionAppDeploymentCommandContext("**/*.jar,**/*.json");

        File file = new File(workspaceDir.getRoot(), "function.json");
        FileUtils.write(file, "{\"scriptFile\": \"program.jar\"}");

        final Run run = mock(Run.class);
        final FilePath workspace = new FilePath(workspaceDir.getRoot());
        final Launcher launcher = mock(Launcher.class);
        final TaskListener listener = mock(TaskListener.class);
        final FunctionApp app = mock(FunctionApp.class);

        ctx.configure(run, workspace, launcher, listener, app);
        Assert.assertEquals(1, ctx.getCommandService().getRegisteredCommands().size());
        Assert.assertEquals(FTPDeployCommand.class, ctx.getCommandService().getStartCommandClass());
    }

    @Test
    public void assertGetScriptFileFromConfig() throws IOException, InterruptedException {
        assertGetScriptFileFromConfig("{\"scriptFile\": \"program.jar\"}", "program.jar");
        assertGetScriptFileFromConfig("{}", "");
    }

    private void assertGetScriptFileFromConfig(String content, String expScriptFile) throws IOException, InterruptedException {
        File file = workspaceDir.newFile();
        FileUtils.write(file, content);

        String scriptFile = FunctionAppDeploymentCommandContext.getScriptFileFromConfig(new FilePath(file));
        Assert.assertEquals(expScriptFile, scriptFile);
    }

    @Test
    public void assertIsJavaFunction() throws IOException, InterruptedException {
        assertIsJavaFunction("{\"scriptFile\": \"program.jar\"}", true);
        assertIsJavaFunction("{\"scriptFile\": \"program.js\"}", false);
        assertIsJavaFunction("{}", false);
    }

    public void assertIsJavaFunction(String content, boolean exp) throws IOException, InterruptedException {
        File file = new File(workspaceDir.getRoot(), "function.json");
        FileUtils.write(file, content);

        boolean isJava = FunctionAppDeploymentCommandContext.isJavaFunction(
                new FilePath(workspaceDir.getRoot()), "", "**/*.json");
        Assert.assertEquals(exp, isJava);
    }
}
