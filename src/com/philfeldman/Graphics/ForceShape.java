package com.philfeldman.Graphics;

import javafx.geometry.Point2D;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by philip.feldman on 8/1/2016.
 */
public class ForceShape extends SmartShape {
    protected class ShapeRelations{
        ForceShape shape;
        double distanceTo;
        double angleToRad;
        double angleToDeg;
        double hSquared;
        double similarity;
        double difference;

        public ShapeRelations(ForceShape s){
            shape = s;
            calcRelations();
        }

        public void calcRelations(){
            distanceTo = 1;
            angleToRad = shape.getOriginAngle() - originAngle;
            angleToDeg = Math.toDegrees(angleToRad);
            double dx = shape.getPos().getX() - getPos().getX();
            double dy = shape.getPos().getY() - getPos().getY();
            hSquared = dx*dx + dy*dy;
            distanceTo = Math.sqrt(hSquared);
        }
    }

    protected boolean isAnchor;
    protected double attractionK;
    protected double repulsionK;
    protected Point2D attractionVec;
    protected Point2D repulsionVec;
    protected Point2D motionVec;
    protected double originAngle;
    protected double originDistance;
    protected double maxDist;
    protected HashMap<String, ShapeRelations> distanceMap;
    protected ArrayList<ShapeRelations> distanceList;
    protected double originScalar;
    protected double dragCoef;
    protected double moveThreshold;
    protected Comparator<ShapeRelations> angleComparator;
    protected Comparator<ShapeRelations> distanceComparator;


    public ForceShape(String n, Paint c1, Paint c2) {
        super(n, c1, c2);

        distanceMap = null;
        isAnchor = false;
        attractionK = 0.1;
        repulsionK = 10.0;
        originAngle = 0;
        originDistance = 0;
        dragCoef = 0.1;
        moveThreshold = 2.5;
        originScalar = 1;
        attractionVec = new Point2D(0, 0);
        repulsionVec = new Point2D(0, 0);
        motionVec = new Point2D(0, 0);
        if (randVal < .5) {
            randVal = 0.25 + rand.nextDouble() * .2 - .1;
        } else {
            randVal = 0.75 + rand.nextDouble() * .2 - .1;
        }

        angleComparator = new Comparator<ShapeRelations>() {
            @Override
            public int compare(ShapeRelations o1, ShapeRelations o2) {
                double a1 = Math.abs(o1.angleToRad);
                double a2 = Math.abs(o2.angleToRad);
                if(a1 < a2){
                    return -1;
                }else if (a1 < a2){
                    return 1;
                }
                return 0;
            }
        };

        distanceComparator = new Comparator<ShapeRelations>() {
            @Override
            public int compare(ShapeRelations o1, ShapeRelations o2) {
                if(o1.distanceTo > o2.distanceTo){
                    return -1;
                }else if (o1.distanceTo < o2.distanceTo){
                    return 1;
                }
                return 0;
            }
        };

    }

    public boolean isAnchor() {
        return isAnchor;
    }

    public void setAnchor(boolean anchor) {
        isAnchor = anchor;
    }

    public double getAttractionK() {
        return attractionK;
    }

    public double getOriginAngle() {
        return originAngle;
    }

    public double getOriginDistance() {
        return originDistance;
    }

    public void setAttractionK(double attractionK) {
        this.attractionK = attractionK;
    }

    public double getRepulsionK() {
        return repulsionK;
    }

    public void setRepulsionK(double repulsionK) {
        this.repulsionK = repulsionK;
    }

    public double getDragCoef() {
        return dragCoef;
    }

    public void setDragCoef(double dragCoef) {
        this.dragCoef = dragCoef;
    }

    public double getOriginScalar() {
        return originScalar;
    }

    public void setOriginScalar(double originScalar) {
        this.originScalar = originScalar;
    }

    public HashMap<String, ShapeRelations> getDistanceMap() {
        return distanceMap;
    }

    @Override
    public void setAllShapesList(ArrayList<SmartShape> allShapesList) {
        super.setAllShapesList(allShapesList);
    }

    @Override
    public void behavior(double e) {
        super.behavior(e);

        angle += dClock * timeScalar;
        angle += dClock * timeScalar;
        if (!isAnchor) {
            clearForces();
            addForces(null);

            // first time
            if(distanceMap == null) {
                distanceMap = new HashMap<>();
                distanceList = new ArrayList<>();
                for (SmartShape ss : allShapesList) {
                    ForceShape fs = (ForceShape) ss;
                    if (fs != this) {
                        if (!distanceMap.containsKey(fs.getName())) {
                            ShapeRelations sr = new ShapeRelations(fs);
                            distanceMap.put(fs.getName(), sr);
                            distanceList.add(sr);
                        }
                    }
                }
            }

            for (SmartShape ss : allShapesList) {

                ForceShape fs = (ForceShape) ss;
                if (fs != this) {
                    addForces(fs);
                }
            }
            calcForces();

            double xv = motionVec.getX() * dClock * timeScalar;
            double yv = motionVec.getY() * dClock * timeScalar;
            posVec = posVec.add(xv, yv);
            originDistance = posVec.distance(0,0);
            originAngle = Math.atan2(posVec.getY(), posVec.getX());
        }
    }

    public void clearForces() {
        maxDist = 0;
        attractionVec = new Point2D(0, 0);
        repulsionVec = new Point2D(0, 0);
    }

    // passing in a null sets an attraction to the origin
    public void addForces(ForceShape targ) {
        double attrScalar = 0.1;
        double repelScalar = 10.0;
        double originAttraction = 100.0 * originScalar;
        double originRepulsion = 0.1 * originScalar;

        double dx = posVec.getX();
        double dy = posVec.getY();

        if (targ != null) {
            dx = posVec.getX() - targ.getPos().getX();
            dy = posVec.getY() - targ.getPos().getY();
        }
        double hSquared = dx * dx + dy * dy;
        if (hSquared > 0.1) {
            // calculate the similarity distance.
            double a = originAttraction * attractionK;
            double r = originRepulsion * repulsionK;
            if (targ != null) {
                ShapeRelations sr = distanceMap.get(targ.getName());
                sr.calcRelations();
                hSquared = sr.hSquared;
                maxDist = Math.max(hSquared, maxDist);
                a = calcSimilarity(targ);
                r = calcDifference(targ);
                sr.similarity = a;
                sr.difference = r;
            }
            if(a > 100 || r > 100){
                System.out.println(getName()+"-"+targ.getName());
            }

            double ax = a * dx * attrScalar;
            double ay = a * dy * attrScalar;
            attractionVec = attractionVec.add(ax, ay);


            double rx = r * repelScalar * dx / hSquared;
            double ry = r * repelScalar * dy / hSquared;
            repulsionVec = repulsionVec.add(rx, ry);
        }
    }

    protected double calcSimilarity(ForceShape targ) {
        double dif = Math.abs(randVal - targ.randVal);
        double s = 1 - dif;
        return s;
    }

    protected double calcDifference(ForceShape targ) {
        double dif = Math.abs(randVal - targ.randVal);
        return dif;
    }

    public void calcForces() {
        double mx = repulsionK * repulsionVec.getX() - attractionK * attractionVec.getX();
        double my = repulsionK * repulsionVec.getY() - attractionK * attractionVec.getY();

        double dragx = mx * dragCoef;
        double dragy = my * dragCoef;

        mx -= dragx;
        my -= dragy;

        double velocity =  Math.sqrt(mx*mx + my*my);

        if(velocity < moveThreshold) {
            mx = 0;
            my = 0;
        }

        motionVec = new Point2D(mx, my);
    }

    @Override
    public String toString() {
        return String.format("%s pos = (%.2f, %.2f), dist = %.2f", name, posVec.getX(), posVec.getY(), originDistance);
    }
}
