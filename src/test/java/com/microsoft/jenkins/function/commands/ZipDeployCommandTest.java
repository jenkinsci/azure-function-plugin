package com.microsoft.jenkins.function.commands;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.jenkins.azurecommons.JobContext;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZipDeployCommandTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void zipDeploy() throws IOException {
        temporaryFolder.newFile("app.jar");
        temporaryFolder.newFile("host.json");
        temporaryFolder.newFolder("bin");

        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        ZipDeployCommand.IZipDeployCommandData context = mock(ZipDeployCommand.IZipDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("");
        WebAppBase functionApp = mock(FunctionApp.class);
        when(context.getWebAppBase()).thenReturn(functionApp);
        when(context.getFilePath()).thenReturn("**");

        ZipDeployCommand command = new ZipDeployCommand();
        command.execute(context);

        verify(functionApp).zipDeploy(any(InputStream.class));
    }
}
