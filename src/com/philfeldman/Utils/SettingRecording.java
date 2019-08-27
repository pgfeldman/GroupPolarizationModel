package com.philfeldman.Utils;

/**
 * Created by SooperYooser on 1/5/2017.
 */
public class SettingRecording {
    public static int UNSET = -1;
    public double timestamp = UNSET;
    public double greenExploitRadius = UNSET;
    public double redExploitRadius = UNSET;
    public double clusterEpsPercent = UNSET;
    public double stageLimit = UNSET;

    public SettingRecording(double timestamp, double greenExploitRadius, double redExploitRadius, double clusterEpsPercent, double stageLimit) {
        this.timestamp = timestamp;
        this.greenExploitRadius = greenExploitRadius;
        this.redExploitRadius = redExploitRadius;
        this.clusterEpsPercent = clusterEpsPercent;
        this.stageLimit = stageLimit;
    }

    public SettingRecording(double timestamp, RunConfiguration config){
        this.timestamp = timestamp;
        greenExploitRadius = config.greenExploitRadius;
        redExploitRadius = config.redExploitRadius;
        clusterEpsPercent = config.clusterPercent;
        stageLimit = config.stageLimit;
    }
}
