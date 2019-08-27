package com.philfeldman.Graphics;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.util.*;

/**
 * Created by philip.feldman on 8/10/2016.
 */
public class ClusterCanvas extends ResizableCanvas {
    protected class DoubleComparator implements Comparator<Double> {
        @Override
        public int compare(Double o1, Double o2) {
            if(o1 > o2){
                return 1;
            }else if(o1 < o2){
                return -1;
            }
            return 0;
            }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
    protected DBSCANClusterer<SmartShape> dbscan;
    protected List<Cluster<SmartShape>> clusterList;
    protected DoubleComparator doubleComparator;
    protected double epsScalar = 0.1f;

    public ClusterCanvas(int w, int h) {
        super(w, h);
        doubleComparator = new DoubleComparator();
    }

    public double getEpsScalar() {
        return epsScalar;
    }

    public void setEpsScalar(double epsScalar) {
        this.epsScalar = epsScalar;
    }

    public boolean calcClusters(){
        if(getShapeList().size() == 0){
            return false;
        }

        //dprint("ClusterCanvas.CalcClusters()");
        EuclideanDistance ed = new EuclideanDistance();
        ArrayList<Double> distList = new ArrayList<>();
        for(SmartShape outerSS : getShapeList()){
            outerSS.setFillColor(Paint.valueOf("white"));
        }
        for(SmartShape outerSS : getShapeList()){
            for(SmartShape innerSS : getShapeList()){
                if(outerSS == innerSS){
                    continue;
                }
                if(Double.isNaN(outerSS.getPos().getX())){
                    outerSS.setFillColor(Paint.valueOf("black"));
                    System.out.println(outerSS.toString()+"\n"+innerSS.toString()+"\n");
                    continue;
                }else if(Double.isNaN(innerSS.getPos().getX())){
                    innerSS.setFillColor(Paint.valueOf("black"));
                    System.out.println(innerSS.toString()+"\n"+innerSS.toString()+"\n");
                    continue;
                }
                double[] op = outerSS.getPoint();
                double[] ip = innerSS.getPoint();
                double dist = ed.compute(ip, op);
                if(!Double.isNaN(dist)) {
                    distList.add(dist);
                }
            }
        }
        distList.sort(doubleComparator);
        int index = (int)Math.round(distList.size()*epsScalar);
        index = Math.min(index, distList.size()-1); // make sure we don't go sailing out of bounds
        double eps = distList.get(index);
        DBSCANClusterer<SmartShape> dbscan = new DBSCANClusterer<SmartShape>(eps, 3);
        clusterList = dbscan.cluster(getShapeList());

        StringBuilder sb = new StringBuilder();
        //sb.append(clusterList.size()+" clusters with eps of < "+eps+"\n");
        int hue = 0;
        for(Cluster<SmartShape> cluster : clusterList){
            Color c = Color.hsb(hue, 1, 1);
            //sb.append("\ncluster ("+cluster.getPoints().size()+" elements):\n");
            for(SmartShape ss : cluster.getPoints()){
                //sb.append(String.format("\t%s: (%.2f, %.2f)\n", ss.getName(), ss.posVec.getX(), ss.posVec.getY()));
                ss.setFillColor(c);
            }
            hue += 31;
        }
        return true;
    }

    public List<Cluster<SmartShape>> getClusterList() {
        return clusterList;
    }

    public static void main(String[] args){
        EuclideanDistance ed = new EuclideanDistance();
        Random rand = new Random();
        ArrayList<Double> distList = new ArrayList<>();
        Comparator<Double> doubleComparator = new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                if(o1 > o2){
                    return 1;
                }else if(o1 < o2){
                    return -1;
                }
                return 0;
            }
        };
        List<SmartShape> ssList = new ArrayList();
        int numClusters = 3;
        int clusterPop = 10;
        double width = 100;
        double clusterWidth = width/(numClusters+10);

        for(int i = 0; i < numClusters; ++i){
            for(int j = 0; j < clusterPop; ++j){
                SmartShape ss = new SmartShape("Shape["+i+"-"+j+"]", Paint.valueOf("red"), Paint.valueOf("black"));
                double xoff = width/numClusters * i;
                double yoff = width/numClusters * i;
                double xpos = xoff + rand.nextDouble()*clusterWidth;
                double ypos = yoff + rand.nextDouble()*clusterWidth;
                ss.setPos(xpos, ypos);
                ssList.add(ss);
                System.out.println(String.format("%s, %.2f, %.2f", ss.getName(), ss.getPoint()[0], ss.getPoint()[1]));
            }
        }

        for(SmartShape outerSS : ssList){
            for(SmartShape innerSS : ssList){
                if(outerSS == innerSS){
                    continue;
                }
                double[] op = outerSS.getPoint();
                double[] ip = innerSS.getPoint();
                double dist = ed.compute(op, ip);
                distList.add(dist);
            }
        }
        distList.sort(doubleComparator);
        double eps = distList.get(distList.size()/5);
        DBSCANClusterer<SmartShape> dbscan = new DBSCANClusterer<SmartShape>(eps, 3);
        final List<Cluster<SmartShape>> clusterList = dbscan.cluster(ssList);

        System.out.println(clusterList.size()+" clusters with eps of < "+eps);
        for(Cluster<SmartShape> cluster : clusterList){
            System.out.println("\ncluster ("+cluster.getPoints().size()+" elements):");
            for(SmartShape ss : cluster.getPoints()){
                System.out.println(ss.toString());
            }
        }

    }
}
