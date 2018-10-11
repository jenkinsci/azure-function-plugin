/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.function.util;

import com.microsoft.rest.LogLevel;

public final class Constants {

    private Constants() {
        // Hide
    }

    public static final String PLUGIN_NAME = "AzureJenkinsFunction";

    public static final LogLevel DEFAULT_AZURE_SDK_LOGGING_LEVEL = LogLevel.NONE;

    // the first option for select element. Keep the same value as jenkins pre-defined default empty value.
    public static final String EMPTY_SELECTION = "- none -";

    /**
     * AI constants.
     */
    public static final String AI_FUNCTION_APP = "FunctionApp";
    public static final String AI_START_DEPLOY = "StartDeploy";
    public static final String AI_CONFIGURE_FAILED = "ConfigureFailed";
    public static final String AI_GIT_DEPLOY = "GitDeploy";
    public static final String AI_GIT_DEPLOY_FAILED = "GitDeployFailed";
    public static final String AI_ZIP_DEPLOY = "ZipDeploy";
    public static final String AI_ZIP_DEPLOY_FAILED = "ZipDeployFailed";
    public static final String AI_FTP_DEPLOY = "FTPDeploy";
    public static final String AI_FTP_DEPLOY_FAILED = "FTPDeployFailed";
}
