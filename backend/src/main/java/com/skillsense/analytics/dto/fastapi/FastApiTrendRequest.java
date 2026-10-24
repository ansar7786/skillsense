package com.skillsense.analytics.dto.fastapi;

public class FastApiTrendRequest {

    private Object current;
    private Object previous;

    public FastApiTrendRequest() {
    }

    public FastApiTrendRequest(Object current, Object previous) {
        this.current = current;
        this.previous = previous;
    }

    public Object getCurrent() {
        return current;
    }

    public void setCurrent(Object current) {
        this.current = current;
    }

    public Object getPrevious() {
        return previous;
    }

    public void setPrevious(Object previous) {
        this.previous = previous;
    }
}
