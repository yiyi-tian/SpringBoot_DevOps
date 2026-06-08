package org.example.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devops.access-log")
public class AccessLogProperties {

    /** topbiz | user | message | log */
    private String serviceName = "unknown";

    private String outputDir = "logs/access";

    private String filePattern = "access-{date}.jsonl";

    private int localRetentionHours = 24;

    private boolean logBody = true;

    private boolean bodyOnErrorOnly = true;

    private int maxBodyLength = 4096;

    private long slowRequestThresholdMs = 3000;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public int getLocalRetentionHours() {
        return localRetentionHours;
    }

    public void setLocalRetentionHours(int localRetentionHours) {
        this.localRetentionHours = localRetentionHours;
    }

    public boolean isLogBody() {
        return logBody;
    }

    public void setLogBody(boolean logBody) {
        this.logBody = logBody;
    }

    public boolean isBodyOnErrorOnly() {
        return bodyOnErrorOnly;
    }

    public void setBodyOnErrorOnly(boolean bodyOnErrorOnly) {
        this.bodyOnErrorOnly = bodyOnErrorOnly;
    }

    public int getMaxBodyLength() {
        return maxBodyLength;
    }

    public void setMaxBodyLength(int maxBodyLength) {
        this.maxBodyLength = maxBodyLength;
    }

    public long getSlowRequestThresholdMs() {
        return slowRequestThresholdMs;
    }

    public void setSlowRequestThresholdMs(long slowRequestThresholdMs) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
    }
}
