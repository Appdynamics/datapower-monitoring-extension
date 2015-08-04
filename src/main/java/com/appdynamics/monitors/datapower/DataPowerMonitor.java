package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.file.FileLoader;
import com.appdynamics.extensions.yml.YmlReader;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataPowerMonitor extends AManagedMonitor {
    public static final Logger logger = LoggerFactory.getLogger(DataPowerMonitor.class);
    private static final String METRIC_PREFIX = "Custom Metrics|Data Power|";

    private SoapMessageUtil soapMessageUtil;
    private JAXBContext jaxbContext;
    private Stat[] metricConfig;
    private Map<String, ?> config;
    protected boolean initialized;
    private String metricPrefix;
    private ExecutorService executorService;
    private int executorServiceSize;

    public DataPowerMonitor() {
        String version = getClass().getPackage().getImplementationTitle();
        String msg = String.format("Using Monitor Version [%s]", version);
        logger.info(msg);
        System.out.println(msg);
        soapMessageUtil = new SoapMessageUtil();
        try {
            jaxbContext = JAXBContext.newInstance(Stat.Stats.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initialize(Map<String, String> argsMap) {
        if (!initialized) {
            final String configFilePath = argsMap.get("config-file");
            final String metricFilePath = argsMap.get("metric-info-file");
            FileLoader.load(new FileLoader.Listener() {
                public void load(File file) {
                    String path = file.getAbsolutePath();
                    try {
                        if (path.contains(configFilePath)) {
                            logger.info("The file [{}] has changed, reloading the config", file.getAbsolutePath());
                            reloadConfig(file);
                        } else if (path.contains(metricFilePath)) {
                            reloadMetricConfig(file);
                            logger.info("The file [{}] has changed, reloading the metrics", file.getAbsolutePath());
                        } else {
                            logger.warn("Unknown file [{}] changed, ignoring", file.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while reloading the file " + file.getAbsolutePath(), e);
                    }
                }
            }, configFilePath, metricFilePath);
            initialized = true;
        }
    }

    protected void reloadMetricConfig(File metricFile) {
        metricConfig = getStatsInfo(metricFile);
    }

    protected void reloadConfig(File file) {
        config = YmlReader.readFromFileAsMap(file);
        if (config != null) {
            metricPrefix = getMetricPrefix();
            Integer numberOfThreads = (Integer) config.get("numberOfThreads");
            if (numberOfThreads == null) {
                numberOfThreads = 5;
            }
            if (executorService == null) {
                executorService = createThreadPool(numberOfThreads);
                logger.info("Initializing the ThreadPool with size {}", numberOfThreads);
            } else if (numberOfThreads != executorServiceSize) {
                logger.info("The ThreadPool size has been updated from {} -> {}", executorServiceSize, numberOfThreads);
                executorService.shutdown();
                executorService = createThreadPool(numberOfThreads);
            }
            executorServiceSize = numberOfThreads;
        } else {
            throw new IllegalArgumentException("The config cannot be initialized from the file " + file.getAbsolutePath());
        }
    }

    private ExecutorService createThreadPool(Integer numberOfThreads) {
        return Executors.newFixedThreadPool(numberOfThreads.intValue(), new ThreadFactory() {
            private int count;

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DataPower-Task-Thread" + (++count));
                thread.setContextClassLoader(AManagedMonitor.class.getClassLoader());
                return thread;
            }
        });
    }

    protected String getMetricPrefix() {
        if (this.config != null) {
            String prefix = (String) this.config.get("metricPrefix");
            logger.debug("The metric prefix from the config file is {}", prefix);
            if (StringUtils.hasText(prefix)) {
                prefix = StringUtils.trim(prefix, "|");
                metricPrefix = prefix + "|";
            } else {
                metricPrefix = METRIC_PREFIX;
            }
            logger.info("The metric prefix is initialized as {}", metricPrefix);
            return metricPrefix;
        }
        return METRIC_PREFIX;
    }

    public TaskOutput execute(Map<String, String> argsMap, TaskExecutionContext executionContext) throws TaskExecutionException {
        Thread thread = Thread.currentThread();
        ClassLoader originalCl = thread.getContextClassLoader();
        thread.setContextClassLoader(AManagedMonitor.class.getClassLoader());
        try {
            if (!initialized) {
                initialize(argsMap);
            }
            logger.debug("The raw arguments are {}", argsMap);
            if (config != null && metricConfig != null) {
                List<Map> servers = (List) config.get("servers");
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        MetricFetcher task = createTask(server);
                        executorService.execute(task);
                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                if (config == null) {
                    logger.error("The config.yml is not loaded due to previous errors.The task will not run");
                }
                if (metricConfig == null) {
                    logger.error("The metrics.xml is not loaded due to previous errors.The task will not run");
                }
            }
        } finally {
            thread.setContextClassLoader(originalCl);
        }
        return new TaskOutput("DataPower Monitor Completed");
    }

    private boolean useBulkApi(Map server) {
        Boolean useLegacyBulkApi = (Boolean) server.get("useBulkApi");
        boolean useBulkApi;
        if (Boolean.TRUE.equals(useLegacyBulkApi)) {
            useBulkApi = true;
        } else {
            useBulkApi = false;
        }
        return useBulkApi;
    }

    protected MetricFetcher createTask(Map server) {
        return new MetricFetcher.Builder(useBulkApi(server))
                .server(server)
                .metricPrefix(metricPrefix)
                .metricConfig(metricConfig)
                .config(config)
                .soapMessageUtil(soapMessageUtil)
                .metricWriter(this)
                .build();
    }

    protected Stat[] getStatsInfo(File file) {
        if (file.exists()) {
            try {
                return readStatsInfoFile(new FileInputStream(file));
            } catch (Exception e) {
                logger.error("Error while reading the metric file " + file.getAbsolutePath(), e);
            }
        }
        return null;
    }

    protected Stat[] readStatsInfoFile(InputStream inputStream) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Stat.Stats stats = (Stat.Stats) unmarshaller.unmarshal(inputStream);
            if (stats != null) {
                return stats.getStats();
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Error while unmarshalling the input file", e);
            return null;
        }
    }

    protected void printMetric(String metricPath, String value, MetricType metricType) {
        if (metricType == null) {
            printCollectiveObservedCurrent(metricPath, value);
            logger.warn("The metric type was not found for metricPath={} and value={}, defaulting to COLLECTIVE_OBSERVED_CURRENT"
                    , metricPath, value);
        } else {
            switch (metricType) {
                case COLLECTIVE_OBSERVED_AVERAGE:
                    printCollectiveObservedAverage(metricPath, value);
                    break;
                case COLLECTIVE_OBSERVED_CURRENT:
                    printCollectiveObservedCurrent(metricPath, value);
                    break;
                default:
                    logger.error("Unknown metric type {}. Ignoring the metric={} and value={}",
                            metricType, metricPath, value);
                    break;
            }
        }
    }

    protected void printCollectiveObservedCurrent(String metricName, String metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    protected void printCollectiveObservedAverage(String metricName, String metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    public void printMetric(String metricPath, String metricValue, String aggregation, String timeRollup, String cluster) {
        MetricWriter metricWriter = getMetricWriter(metricPath,
                aggregation,
                timeRollup,
                cluster
        );
        if (metricValue != null) {
            metricWriter.printMetric(metricValue.toString());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Metric [" + aggregation + "/" + timeRollup + "/" + cluster
                    + "] metric = " + metricPath + " = " + metricValue);
        }
    }


}
