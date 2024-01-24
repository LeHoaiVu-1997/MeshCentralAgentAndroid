package com.meshcentral.agent.control;

public class MouseAction {
    private int x;
    private int y;
    private int actionFlag;

    public MouseAction(int x, int y, int actionFlag) {
        this.x = x;
        this.y = y;
        this.actionFlag = actionFlag;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getActionFlag() {
        return actionFlag;
    }
}
