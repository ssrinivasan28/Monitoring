package com.islandpacific.sentinel.fleet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sentinel.fleet")
public class FleetHeadroomProperties {

    private Weights weights = new Weights();
    private double criticalThreshold = 95.0;
    private double amberThreshold = 75.0;
    private Cutoffs cutoffs = new Cutoffs();

    public Weights getWeights() {
        return weights;
    }

    public void setWeights(Weights weights) {
        this.weights = weights;
    }

    public double getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public double getAmberThreshold() {
        return amberThreshold;
    }

    public void setAmberThreshold(double amberThreshold) {
        this.amberThreshold = amberThreshold;
    }

    public Cutoffs getCutoffs() {
        return cutoffs;
    }

    public void setCutoffs(Cutoffs cutoffs) {
        this.cutoffs = cutoffs;
    }

    public static class Weights {
        private double asp = 0.40;
        private double disk = 0.25;
        private double cpu = 0.20;
        private double memJobq = 0.15;

        public double getAsp() { return asp; }
        public void setAsp(double asp) { this.asp = asp; }

        public double getDisk() { return disk; }
        public void setDisk(double disk) { this.disk = disk; }

        public double getCpu() { return cpu; }
        public void setCpu(double cpu) { this.cpu = cpu; }

        public double getMemJobq() { return memJobq; }
        public void setMemJobq(double memJobq) { this.memJobq = memJobq; }
    }

    public static class Cutoffs {
        private double red = 90.0;
        private double amber = 75.0;

        public double getRed() { return red; }
        public void setRed(double red) { this.red = red; }

        public double getAmber() { return amber; }
        public void setAmber(double amber) { this.amber = amber; }
    }
}
