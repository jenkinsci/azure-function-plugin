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
import org.mockito.Mockito;

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
        temporaryFolder.newFolder("target");
        temporaryFolder.newFile("target/a.jar");
        temporaryFolder.newFile("target/host.json");
        temporaryFolder.newFile("target/template.json");
        temporaryFolder.newFolder("target", "bin");
        temporaryFolder.newFile("target/bin/runtime1.dll");
        temporaryFolder.newFile("target/bin/runtime2.dll");


        Run run = mock(Run.class);
        FilePath workspace = new FilePath(temporaryFolder.getRoot());
        Launcher launcher = mock(Launcher.class);
        TaskListener listener = mock(TaskListener.class);
        JobContext jobContext = new JobContext(run, workspace, launcher, listener);

        ZipDeployCommand.IZipDeployCommandData context = mock(ZipDeployCommand.IZipDeployCommandData.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getSourceDirectory()).thenReturn("target");
        WebAppBase functionApp = mock(FunctionApp.class);
        when(context.getWebAppBase()).thenReturn(functionApp);
        when(context.getFilePath()).thenReturn("*.json,**/*.dll,*jar");

        ZipDeployCommand command = new ZipDeployCommand();
        command.execute(context);

        verify(functionApp).zipDeploy(any(InputStream.class));
        verify(context).logStatus(Mockito.matches("Archive 5 target files under.*"));
    }
}
