/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.datapower.util;

public class Constants {
    public static String DISPLAY_NAME;
    public static String USER_NAME;
    public static String PASSWORD;
    public static String ENCRYPTED_PASSWORD;
    public static String ENCRYPTION_KEY;
    public static String METRIC_SEPARATOR;
    public static String METRIC_PREFIX;
    public static String MonitorName;
    public static String HEARTBEAT;

    static {
        DISPLAY_NAME = "displayName";
        METRIC_PREFIX = "Custom Metrics|DataPower|";
        MonitorName = "DataPowerMonitor";
        USER_NAME = "username";
        PASSWORD = "password";
        ENCRYPTED_PASSWORD = "encryptedPassword";
        ENCRYPTION_KEY = "encryptionKey";
        METRIC_SEPARATOR = "|";
        HEARTBEAT = "HeartBeat";
    }
}