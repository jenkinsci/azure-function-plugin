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