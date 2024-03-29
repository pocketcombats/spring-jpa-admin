package com.pocketcombats.admin.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "spring.jpa-admin")
public class JpaAdminProperties {

    private int autoConfigurationOrder = Ordered.LOWEST_PRECEDENCE;
    private boolean disableHistory;
    private int historySize = 10;
    private Templates templates = new Templates();

    public int getAutoConfigurationOrder() {
        return autoConfigurationOrder;
    }

    public void setAutoConfigurationOrder(int autoConfigurationOrder) {
        this.autoConfigurationOrder = autoConfigurationOrder;
    }

    public boolean isDisableHistory() {
        return disableHistory;
    }

    public void setDisableHistory(boolean disableHistory) {
        this.disableHistory = disableHistory;
    }

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    public Templates getTemplates() {
        return templates;
    }

    public void setTemplates(Templates templates) {
        this.templates = templates;
    }

    public static class Templates {

        private String index = "admin/index";
        private String list = "admin/list";
        private String form = "admin/form";
        private String actionPrompt = "admin/action";

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getList() {
            return list;
        }

        public void setList(String list) {
            this.list = list;
        }

        public String getForm() {
            return form;
        }

        public void setForm(String form) {
            this.form = form;
        }

        public String getActionPrompt() {
            return actionPrompt;
        }

        public void setActionPrompt(String actionPrompt) {
            this.actionPrompt = actionPrompt;
        }
    }
}
