package com.skillsense.survey.dto;

public class EligibleCountResponse {

    private long count;

    public EligibleCountResponse() {
    }

    public EligibleCountResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
