/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.util;

import com.appdynamics.extensions.datapower.DataPowerMonitor;
import com.appdynamics.extensions.datapower.util.SoapMessageUtil;
import com.appdynamics.extensions.datapower.util.Xml;

import java.io.InputStream;

/**
 * Created by abey.tom on 7/31/15.
 */
public class TestHelper {
    public static Xml[] getResponse(String operation) {
        InputStream in = DataPowerMonitor.class.getResourceAsStream("/output/" + operation + ".xml");
        if (in != null) {
            return new SoapMessageUtil().getSoapResponseBody(in, operation);
        } else {
            return null;
        }
    }
}
