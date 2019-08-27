package com.philfeldman.Graphics;

import com.philfeldman.GPCAs.FlockingBeliefCA;
import javafx.scene.paint.Color;
import org.apache.commons.math3.ml.clustering.Cluster;

/**
 * Created by SooperYooser on 1/4/2017.
 */
public class FlockingClusterCanvas extends ClusterCanvas{
    public FlockingClusterCanvas(int w, int h) {
        super(w, h);
    }

    @Override
    public boolean calcClusters() {
        boolean result = super.calcClusters();
        if (result) {
            for (SmartShape ss : getShapeList()) {
                FlockingShape fs = (FlockingShape) ss;
                fs.getAgent().setClusterId(FlockingBeliefCA.DEFAULT_CLUSTER_ID);
            }

            for (int i = 0; i < clusterList.size(); ++i) {
                Cluster<SmartShape> cluster = clusterList.get(i);
                //sb.append("\ncluster ("+cluster.getPoints().size()+" elements):\n");
                for (SmartShape ss : cluster.getPoints()) {
                    FlockingShape fs = (FlockingShape) ss;
                    fs.getAgent().setClusterId(i);
                }
            }
        }
        return result;
    }
}
