package com.flashback.domain;

/**
 * M3 life node categories for NODE_RECORD.
 */
public enum LifeNodeType {
    GRADUATION("毕业"),
    WORK("工作"),
    MOVE("搬家"),
    RELATIONSHIP("关系"),
    HEALTH("健康"),
    FAMILY("家庭"),
    TURNING_POINT("转折"),
    OTHER("其他");

    private final String label;

    LifeNodeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
