# Azure Function Plugin

A Jenkins plugin to deploy an Azure Function.

## How to Install

You can install/update this plugin in Jenkins update center (Manage Jenkins -> Manage Plugins, search Azure Function Plugin).

You can also manually install the plugin if you want to try the latest feature before it's officially released.
To manually install the plugin:

1. Clone the repo and build:
   ```
   mvn package
   ```
2. Open your Jenkins dashboard, go to Manage Jenkins -> Manage Plugins.
3. Go to Advanced tab, under Upload Plugin section, click Choose File.
4. Select `azure-function.hpi` in `target` folder of your repo, click Upload.
5. Restart your Jenkins instance after install is completed.

## Deploy to Azure Function

### Prerequisites

To use this plugin to deploy to Azure Function, first you need to have an Azure Service Principal in your Jenkins instance.

1. Create an Azure Service Principal through [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?toc=%2fazure%2fazure-resource-manager%2ftoc.json) or [Azure portal](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal).
2. Open Jenkins dashboard, go to Credentials, add a new Microsoft Azure Service Principal with the credential information you just created.

Then create a Function in Azure portal or through Azure CLI.

### Deploy

You can deploy your project to Azure Function by uploading your build artifacts using Git or FTP.

1. Create a new freestyle project in Jenkins, add necessary build steps to build your code.
2. Add a post-build action 'Publish an Azure Function'.
3. Select your Azure credential in Azure Profile Configuration section.
4. In App Configuration section, choose the resource group and function app in your subscription, and also fill in the files you want to deploy (for example, a jar package if you're using Java).
5. There are two optional parameters Source Directory and Target Directory that allows you to specify source and target folders when uploading files.
6. Save the project and build it, your function app will be deployed to Azure when build is completed.

## Deploy using Pipeline

You can also use this plugin in pipeline (Jenkinsfile). Here are some samples to use the plugin in pipeline script:

To deploy a NodeJS function app:

```groovy
azureFunctionAppPublish azureCredentialsId: '<credential_id>',
                   resourceGroup: '<resource_group_name>', appName: '<app_name>',
                   filePath: '**/*.js,**/*.json'
```

For advanced options, you can use Jenkins Pipeline Syntax tool to generate a sample script.

## Data/Telemetry

Azure Function Plugin collects usage data and sends it to Microsoft to help improve our products and services. Read our [privacy statement](http://go.microsoft.com/fwlink/?LinkId=521839) to learn more.

You can turn off usage data collection in Manage Jenkins -> Configure System -> Azure -> Help make Azure Jenkins plugins better by sending anonymous usage statistics to Azure Application Insights.

## Reporting bugs and feature requests

We use [Jenkins JIRA](https://issues.jenkins-ci.org/) to record all bugs and feature requests. Please follow below steps to create your own issues.

1. Search in Jira to see if the issue was existed already.
2. Create a new issue with the component `azure-function-plugin` .

You can refer to [Jira doc](https://confluence.atlassian.com/jiracoreserver/creating-issues-and-sub-tasks-939937904.html#Creatingissuesandsub-tasks-Creatinganissue) for detailed instructions about creating an issue.