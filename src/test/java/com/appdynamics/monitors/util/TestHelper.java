package com.appdynamics.monitors.util;

import com.appdynamics.extensions.xml.Xml;
import com.appdynamics.monitors.datapower.DataPowerMonitor;

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
