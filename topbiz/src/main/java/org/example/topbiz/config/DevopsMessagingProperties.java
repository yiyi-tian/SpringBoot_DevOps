package org.example.topbiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devops.messaging")
public class DevopsMessagingProperties {

    private Welcome welcome = new Welcome();
    private Template template = new Template();

    public Welcome getWelcome() {
        return welcome;
    }

    public void setWelcome(Welcome welcome) {
        this.welcome = welcome;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public static class Welcome {
        private long templateId = 1;
        private String channelType = "IN_APP";

        public long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(long templateId) {
            this.templateId = templateId;
        }

        public String getChannelType() {
            return channelType;
        }

        public void setChannelType(String channelType) {
            this.channelType = channelType;
        }
    }

    public static class Template {
        private String defaultStatus = "DRAFT";

        public String getDefaultStatus() {
            return defaultStatus;
        }

        public void setDefaultStatus(String defaultStatus) {
            this.defaultStatus = defaultStatus;
        }
    }
}
