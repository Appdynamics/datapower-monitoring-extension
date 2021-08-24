# Datapower Monitoring Extension

## Use Case

The IBM® WebSphere® DataPower® SOA Appliance (hereafter called DataPower) is a purpose-built hardware platform designed to simplify, secure, and accelerate XML, Web services, and Enterprise Service Bus deployments.

Please refer to the screenshot link in the Metrics section for the list of reported metrics

This extension works only with the standalone machine agent.

## Prerequisites

1.  Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2.  The DataPower XML Management Interface must be enabled. The DataPower Monitor uses the `"/service/mgmt/current"` SOAP Endpoint to collect the metrics. Please refer to the IBM Redbook WebSphere [DataPower SOA Appliance: The XML Management Interface](http://www.redbooks.ibm.com/redpapers/pdfs/redp4446.pdf) Section 1.3, 1.4 and 1.5 to enable it.
3.  Make sure that the user has permissions to invoke the API. Refer troubleshooting section 5-6 for more details
4.  Start the Machine Agent before installing the extension and make sure that it is reporting the data fine.

## Installation

1. Run `mvn clean install`
2. Unzip the DataPowerMonitor-VERSION.zip (from targets folder of Datapower Repo) to the "<MachineAgent_Dir>/monitors" directory.
3. Edit the file config.yml located at <MachineAgent_Dir>/monitors/DataPowerMonitor The metricPrefix of the extension has to be configured as specified [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
4. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting, or add new entries as well.
5. Restart the Machine Agent.

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.
In the AppDynamics Metric Browser, look for **Application Infrastructure Performance|\<Tier\>|Custom Metrics|DataPower** and you should be able to see all the metrics.

## Configuration
### Config.yml

Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/DataPowerMonitor/`.

  1. Configure the "COMPONENT_ID" under which the metrics need to be reported. This can be done by changing the value of `<COMPONENT_ID>` in   **metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|DataPower|**.
       For example,
       ```
       metricPrefix: "Server|Component:100|Custom Metrics|DataPower|"
       ```
  2.  **Server Details**
        ```
                servers:
                  - uri: http://localhost:5550/service/mgmt/current
                    displayName: server1
                    username: user
                    password: welcome
                    domains:
                      - default
                      - domain1
                    useBulkApi: true
        
                  - uri: https://someotherhost:5550/service/mgmt/current
                    displayName: server2
                    username: user
                    encryptedPassword: C043Fag+LKEZM0AQTWPy2g==
                    domainsRegex:
                      - fin_domain.*
                      - .*security.*
                    useBulkApi: true
        ```

If `passwordEncrypted` is used, make sure to update the `encryptionKey` in `config.yml`. Please [refer here](Please read the extension documentation to generate encrypted password. https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-use-Password-Encryption-with-Extensions/ta-p/29397) for more details.

   3.  **Domains** You have to set the domains from which you need the data from. You can choose between the properties `domains` or `domainsRegex`. If you dont know the domain name use `default` as shown.
    
        ```
                domains:
                  - default
        ```

        `domainsRegex` property can be used to set a regex to match the domains.
        ```
                domainsRegex:
                      - fin_domain.*
                      - .*security.*
        ```

    4.  **Bulk Data Fetching** `useBulkApi: true` There is a legacy API to get the data in bulk. This minimizes the number of round trips to DP server.
    5.  **Proxy Support**
        ```
                proxy:
                  uri: http://localhost:9090
                  username: proxyyuser
                  password: proxypassword
        ```

## Metrics.xml

You can add/remove metrics of your choice by modifying the provided metrics.xml file. This file consists of all the metrics that will be monitored and sent to the controller. Please look how the metrics have been defined and follow the same convention, when adding new metrics. You do have the ability to chosoe your Rollup types as well as set an alias that you would like to be displayed on the metric browser.

   2. Metric Configuration
    Add the `metric` to be monitored with the metric tag as shown below.
        ```
                 <metric value-xpath="FreeEncrypted" label="Free Encrypted (MB)" aggregationType = "OBSERVATION" timeRollUpType = "AVERAGE" clusterRollUpType = "COLLECTIVE"/>
         ```
For configuring the metrics, the following properties can be used:

 |     Property      |   Default value |         Possible values         |                                               Description                                                      |
 | ----------------- | --------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------- |
 | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
 | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
 | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
 | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
 | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
 | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:1, OPEN:1  |
 | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |


 **All these metric properties are optional, and the default value shown in the table is applied to the metric (if a property has not been specified) by default.**

## Metrics

The metrics will be reported under the tree `Application Infrastructure Performance|$TIER|Custom Metrics|DataPower`

## Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130).

## Troubleshooting

1.  Please follow the steps listed in this [troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.
2.  **Verify Machine Agent Data:** Please start the Machine Agent without the extension and make sure that it reports data. Verify that the machine agent status is UP and it is reporting Hardware Metrics
3.  **config.yml:** Validate the file [here](http://www.yamllint.com)
4.  **Special chars in config** If you have special chars(like in passwords) in config.yml, make sure to wrap it in double quotes `""`
5.  **DataPower SOAP API:** Please update the `user:password` and `datapower:5550` with correct values. Invoke the URL from curl (or wget) and make sure that it is returning data. If it is not, then contact your DataPower Admin with these details
    ```
        curl -u user:password -d '<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"><SOAP-ENV:Header/><SOAP-ENV:Body xmlns:dp="http://www.datapower.com/schemas/management"><dp:request domain="default"><dp:get-status class="HTTPMeanTransactionTime"/></dp:request></SOAP-ENV:Body></SOAP-ENV:Envelope>' https://datapower:5550/service/mgmt/current
    ```

6.  **Enable Statistics** Enable Statistics should be set to `enabled` in the DataPower Admin screen.
7.  **CPU Issue** The issue is the default xml implementation which is bundled with the jdk is trying to read the factory class name from the jar file on every usage. This results in reading all the jar files in the classpath, which shoots up the CPU. Please use the following system properties resolve those. This should be added to machine agent startup before the -jar argument
    ```
        -Dcom.sun.org.apache.xalan.internal.xsltc.dom.XSLTCDTMManager=com.sun.org.apache.xalan.internal.xsltc.dom.XSLTCDTMManager
        -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl
        -Dcom.sun.org.apache.xml.internal.dtm.DTMManager=com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault
    ```

8.  **Metric Limit:** Please start the machine agent with the argument `-Dappdynamics.agent.maxMetrics=5000` if there is a metric limit reached error in the logs. If you dont see the expected metrics, this could be the cause.
9.  **Check Logs:** There could be some obvious errors in the machine agent logs. Please take a look.

## Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/datapower-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.0.0       |
|Controller Compatibility  |4.5 or Later|
|Machine Agent Compatibility|4.5.13+    |
|Last Update               |11/08/2021  |
|Changes list              |[ChangeLog](https://github.com/Appdynamics/datapower-monitoring-extension/blob/master/CHANGELOG.md)|
