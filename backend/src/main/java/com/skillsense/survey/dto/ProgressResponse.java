package com.skillsense.survey.dto;

public class ProgressResponse {

    private long responded;
    private int eligible;
    private double rate;

    public ProgressResponse() {
    }

    public ProgressResponse(long responded, int eligible, double rate) {
        this.responded = responded;
        this.eligible = eligible;
        this.rate = rate;
    }

    public long getResponded() {
        return responded;
    }

    public void setResponded(long responded) {
        this.responded = responded;
    }

    public int getEligible() {
        return eligible;
    }

    public void setEligible(int eligible) {
        this.eligible = eligible;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long responded;
        private int eligible;
        private double rate;

        public Builder responded(long responded) {
            this.responded = responded;
            return this;
        }

        public Builder eligible(int eligible) {
            this.eligible = eligible;
            return this;
        }

        public Builder rate(double rate) {
            this.rate = rate;
            return this;
        }

        public ProgressResponse build() {
            return new ProgressResponse(responded, eligible, rate);
        }
    }
}
