# AppDynamics DataPower Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

## Perquisites
The DataPower

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

4. The metrics will be registered under 
```
Application Infrastructure Performance|Custom Metrics|DataPower|...
```

##Default Metrics

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

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/datapower-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/Apache-Monitoring-Extension/idi-p/753) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
