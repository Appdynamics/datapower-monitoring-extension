# AppDynamics DataPower Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case
The IBM® WebSphere® DataPower® SOA Appliance (hereafter called DataPower) is a purpose-built hardware platform designed to simplify, secure, and accelerate XML, Web services, and Enterprise Service Bus deployments.

## Perquisites
The DataPower XML Management Interface must be enabled. The DataPower Monitor uses the "/service/mgmt/current" SOAP endpoint to collect the metrics.
Please refer to the IBM Redbook [WebSphere DataPower SOA Appliance: The XML Management Interface](http://www.redbooks.ibm.com/redpapers/pdfs/redp4446.pdf) Section 1.3, 1.4 and 1.5 to enable it. 

##Installation
1. Build the project with maven.
```
mvn clean install
```
2. Unzip the file target/DataPowerMonitor.zip to <MachineAgentDir>/monitors.

3. Edit the file <MachineAgentDir>/monitors/DataPowerMonitor/monitor.xml and set the following properties.

| Metric Path  | Required  | Default  | Description|
| ---------------------------- | ------------- | ------------- | ------------- |
| uri | Required | - | The full uri to the DataPower SOAP API eg.https://172.16.153.100:5550/service/mgmt/current |
| username | Required | - | DataPower SOAP API User Name  |
| password | Required | - | DataPower SOAP API Password |
| proxy-host | Optional | - | If a Proxy Server is used to connect to the DataPower XML API |
| proxy-port | Optional | - | If a Proxy Server is used to connect to the DataPower XML API |
| proxy-username | Optional | - | If the Proxy Server needs authentication |
| proxy-password | Optional | - | If the Proxy Server needs authentication |
| metric-prefix | Optional | |  Custom Metrics&#124;DataPower |


###Default Metrics

The metrics will be registered under 
```
Application Infrastructure Performance|Custom Metrics|DataPower|
```
The following metrics are collected OOTB. To customize the collected metrics, please refer to the section below.
```
System|CPU Usage %
System|Load 
System|Work List
System|Memory|Used %
System|Memory|Total (MB)
System|Memory|Used (MB)
System|Memory|Free (MB)
System|Memory|Requested (MB)
System|File System|Free Encrypted (MB)
System|File System|Total Encrypted (MB)
System|File System|Free Temporary (MB)
System|File System|Total Temporary (MB)
System|File System|Free Internal (MB)
System|File System|Total Internal (MB)
Network|eth0|Incoming KB/sec
Network|eth1|Incoming KB/sec
Network|eth2|Incoming KB/sec
Network|eth3|Incoming KB/sec
Network|Total Incoming KB/sec
Transactions|helloworld_xmlfw|Average Response Time (ms)
Transactions|http_proxy_8085|Average Response Time (ms)
Transactions|userws_proxy|Average Response Time (ms)
Transactions|wsproxy|Average Response Time (ms)
Transactions|Average Transaction Time (ms)
```

### Customize Metrics

The list of collected metrics can be customized by configuring the <MachineAgent>/monitors/DataPowerMonitor/metrics.xml. The following screenshots depicts the metric rules.

This section explains how to extract the metric information from the soap response. The metrics.xml rules and the soap response from DataPower API is shown side-by-side

![](https://raw.githubusercontent.com/Appdynamics/datapower-monitoring-extension/master/wiki/images/datapower_conf_00.png?token=5995632__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvZGF0YXBvd2VyLW1vbml0b3JpbmctZXh0ZW5zaW9uL21hc3Rlci93aWtpL2ltYWdlcy9kYXRhcG93ZXJfY29uZl8wMC5wbmciLCJleHBpcmVzIjoxNDAwNjk0NjMxfQ%3D%3D--04675c0498c1236238f40d4836f20568aea807ec)

![](https://raw.githubusercontent.com/Appdynamics/datapower-monitoring-extension/master/wiki/images/datapower_conf_01.png?token=5995632__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvZGF0YXBvd2VyLW1vbml0b3JpbmctZXh0ZW5zaW9uL21hc3Rlci93aWtpL2ltYWdlcy9kYXRhcG93ZXJfY29uZl8wMS5wbmciLCJleHBpcmVzIjoxNDAwNjk0NzAwfQ%3D%3D--56b3b98b25474f43132639e1c3c3ef115e9e8b9e)

![](https://raw.githubusercontent.com/Appdynamics/datapower-monitoring-extension/master/wiki/images/datapower_conf_02.png?token=5995632__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvZGF0YXBvd2VyLW1vbml0b3JpbmctZXh0ZW5zaW9uL21hc3Rlci93aWtpL2ltYWdlcy9kYXRhcG93ZXJfY29uZl8wMi5wbmciLCJleHBpcmVzIjoxNDAwNjk0NzI5fQ%3D%3D--b4474195f029021a9eb4cfd32ec5545b699d75a3)

##Custom Dashboard
![](https://raw.githubusercontent.com/Appdynamics/datapower-monitoring-extension/master/wiki/images/custom_dashboard.png?token=5995632__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvZGF0YXBvd2VyLW1vbml0b3JpbmctZXh0ZW5zaW9uL21hc3Rlci93aWtpL2ltYWdlcy9jdXN0b21fZGFzaGJvYXJkLnBuZyIsImV4cGlyZXMiOjE0MDA2OTQ4NTR9--d7af0d55b8d526ffe2f58f6d9a09241b637d5fc1)

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/datapower-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/Apache-Monitoring-Extension/idi-p/753) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
