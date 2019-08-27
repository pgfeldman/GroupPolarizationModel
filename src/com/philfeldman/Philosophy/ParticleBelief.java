package com.philfeldman.Philosophy;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by SooperYooser on 12/27/2016.
 */
public class ParticleBelief extends Belief {
    public static enum BORDER_TYPE {NONE, REFLECTIVE, RESPAWN, LETHAL, RESTORING_FORCE, TORUS}
    protected class HelperVec{
        String name;
        double x;
        double y;
        double midx;
        double midy;
        double m;
        double b;
        double radians;
        double degrees;
        double length;

        public HelperVec(String name, double rad){
            this.name = name;
            radians = rad;
            degrees = Math.toDegrees(radians);
            x = FastMath.sin(radians);
            y = FastMath.cos(radians);
            midx = x/2;
            midy = x/2;
            if(x != 0) {
                m = y / x;
            }else{
                m = Double.MAX_VALUE;
            }
            b = 0;
            length = 1;
        }

        public HelperVec(String name, double x1, double y1, double x2, double y2){
            this.name = name;
            x = x1;
            y = y1;
            midx = (x1+x2)/2;
            midy = (y1+y2)/2;
            double xdist = x2-x1;
            double ydist = y2-y1;
            if(xdist != 0) {
                m = ydist / xdist;
            }else{
                m = Double.MAX_VALUE;
            }
            b = y - m*x;
            radians = FastMath.atan2(ydist, xdist);
            degrees = FastMath.toDegrees(radians);
            length = Math.sqrt(xdist*xdist+ydist*ydist);
        }

        public HelperVec(String name, HelperVec hv1, HelperVec hv2){
            this(name, hv1.x, hv1.y, hv2.x, hv2.y);
        }

        @Override
        public String toString() {
            return String.format("%s: x=%.2f, y=%.2f, m=%.2f, degrees=%.2f mid=(%.2f, %.2f), len=%.2f", name, x, y, m, degrees, midx, midy, length);
        }
    }

    protected ArrayRealVector unitOrientVector;
    protected ArrayRealVector orientVector;
    protected ArrayRealVector targetOrientVector;
    protected ArrayRealVector posVector;
    protected boolean isHerding;


    public ParticleBelief(String n) {
        super(n);
        unitOrientVector = new ArrayRealVector();
        posVector = new ArrayRealVector();
        isHerding = false;
    }

    public ParticleBelief(ParticleBelief toCopy){
        this(toCopy.name);
        deepUpdate(toCopy);
    }

    public void deepUpdate(ParticleBelief toCopy){
        weight = toCopy.weight;
        isHerding = toCopy.isHerding;
        lastOrdering = toCopy.lastOrdering;
        statementList = new ArrayList<>();
        statementMap = new HashMap<>();
        for(Statement sToCpy : toCopy.statementList){
            ParticleStatement psToCpy = (ParticleStatement)sToCpy;
            ParticleStatement ps = new ParticleStatement(sToCpy.name, sToCpy.weight, sToCpy.credibility, sToCpy.accuracy);
            ps.setValue(psToCpy.getValue());
            ps.setVector(psToCpy.getVector());
            statementList.add(ps);
            statementMap.put(ps.getName(), ps);
        }
        if(toCopy.orientVector != null) {
            orientVector = new ArrayRealVector(toCopy.orientVector);
        }
        if(toCopy.unitOrientVector != null) {
            unitOrientVector = new ArrayRealVector(toCopy.unitOrientVector);
        }
        if(toCopy.targetOrientVector != null) {
            targetOrientVector = new ArrayRealVector(toCopy.targetOrientVector);
        }
        if(toCopy.posVector != null) {
            posVector = new ArrayRealVector(toCopy.posVector);
        }
    }


    public boolean isHerding() {
        return isHerding;
    }

    public void setHerding(boolean herding) {
        isHerding = herding;
    }

    public void addStatement(ParticleStatement f) {
        super.addStatement(f);
        calcUnitOrientVector();
        calcPosVector();
    }

    public ParticleStatement getStatement(String name){
        return (ParticleStatement)super.getStatement(name);
    }

    /**
     * Calculate a position vector of N dimensions
     * @return
     */
    public ArrayRealVector calcPosVector() {
        getSortedList(ORDERING.NAME); // so we compare like with like
        posVector = new ArrayRealVector(statementList.size());
        for(int i = 0; i < statementList.size(); ++i){
            ParticleStatement ps = (ParticleStatement)statementList.get(i);
            posVector.setEntry(i, ps.getValue());
        }
        return posVector;
    }

    public ArrayRealVector calcOrientVector(){
        getSortedList(ORDERING.NAME); // so we compare like with like
        orientVector = new ArrayRealVector(statementList.size());
        for(int i = 0; i < statementList.size(); ++i){
            ParticleStatement ps = (ParticleStatement)statementList.get(i);
            orientVector.setEntry(i, ps.getVector());
        }
        return orientVector;
    }
    /**
     * Calculate the orientation in N dimensions
     * @return
     */
    public ArrayRealVector calcUnitOrientVector(){
        calcOrientVector();
        unitOrientVector = new ArrayRealVector(orientVector);
        if(unitOrientVector.getL1Norm() > 0){
            unitOrientVector.unitize();
        }

        return unitOrientVector;
    }

    /**
     * Get the distance to another ParticleBelief
     * @param otherBelief
     * @return
     */
    public double getDistanceTo(ParticleBelief otherBelief){
        if(compare(otherBelief) == 1.0) {
            ArrayRealVector otherVec = otherBelief.calcPosVector();
            double dist = posVector.getDistance(otherVec);
            return dist;
        }
        return -1;
    }

    public ArrayRealVector getVectorTo(ParticleBelief otherBelief){
        ArrayRealVector otherVec = otherBelief.calcPosVector();
        ArrayRealVector vecTo = otherVec.subtract(posVector);
        return vecTo;
    }

    /**
     * Get the legth of the vector produced by subtracting the current vector from the target (other) vector
     * @param otherBelief
     * @return
     */
    public double getHeadingDifference(ParticleBelief otherBelief){
        if(compare(otherBelief) == 1.0) {
            ArrayRealVector otherVec = otherBelief.calcUnitOrientVector();
            double dist = unitOrientVector.getDistance(otherVec);
            targetOrientVector = otherVec.subtract(unitOrientVector);
            return dist;
        }
        return 0;
    }


    /**
     * Build the vector that we try to align to. This consists of two parts, the heading vector of the other, and the
     * vector that is built by subtracting our position from the target pos.
     * @param otherBelief
     * @return
     */
    public ArrayRealVector setTargetOrientVector(ParticleBelief otherBelief, boolean useUnitVec){
        if(compare(otherBelief) == 1.0) {
            ArrayRealVector otherOrientVec;
            if(useUnitVec){
                otherOrientVec = otherBelief.calcUnitOrientVector();
            } else {
                otherOrientVec = otherBelief.calcOrientVector();
            }
            ArrayRealVector otherPosVec = otherBelief.calcPosVector();
            calcPosVector();
            ArrayRealVector vecToOther = otherPosVec.subtract(posVector);
            targetOrientVector = otherOrientVec.add(vecToOther);
            if(useUnitVec) {
                targetOrientVector.unitize();
            }
            return targetOrientVector;
        }
        return null;
    }

    protected void setOrientVectorContents(ArrayRealVector replacementVector){
        if(unitOrientVector == null){
            unitOrientVector = new ArrayRealVector(replacementVector);
            return;
        }

        if(replacementVector.getDimension() != unitOrientVector.getDimension()){
            return;
        }

        for(int i = 0; i < unitOrientVector.getDimension(); ++i){
            double val = replacementVector.getEntry(i);
            if(!Double.isNaN(val)) {
                unitOrientVector.setEntry(i, val);
            }else{
                System.out.println("ParticleBelief.setOrientVectorContents() NaN problem: "+toString());
            }
        }
    }

    public void updateParticleHeading(double maxPercent){
        if(targetOrientVector != null && targetOrientVector.getDimension() > 0){
            orientVector = orientVector.add(targetOrientVector);
            unitOrientVector = new ArrayRealVector(orientVector.unitVector());

            // rescale so that we keep dimensions AND angles
            double newLen = orientVector.getNorm();
            if(newLen != 0.0) {
                for (int i = 0; i < orientVector.getDimension(); ++i) {
                    double val = orientVector.getEntry(i);
                    ParticleStatement s = (ParticleStatement) statementList.get(i);
                    s.setVector(val);
//                System.out.println(s.toString());
                }
            }
        }
    }

    /**
     * Interpolate the orientation vector up to a max, then re-normalize to the length.
     * @param maxSlewDegrees
     */
    public void interpolateBoidHeading(double maxSlewDegrees){
        if(targetOrientVector != null && targetOrientVector.getDimension() > 0){
            double maxSlewRadians = Math.toRadians(maxSlewDegrees);

            // calculate the angle between the current heading and the target heading. If we're close, then just finish up and return
            double dotProduct = unitOrientVector.dotProduct(targetOrientVector);
            if(Double.isNaN(dotProduct)){
                System.out.println("ParticleBelief.interpolateBoidHeading() NaN problem: "+toString());
            }
            double targLen = targetOrientVector.getNorm();
            double cosTheta = Math.min(1.0, dotProduct/targLen);
            double fullSlewRadians = Math.acos(cosTheta);
//            System.out.println("\nAngle to Target= "+Math.toDegrees(fullSlewRadians));
//            System.out.println("Angle increment= "+Math.toDegrees(maxSlewRadians));
            if(fullSlewRadians < maxSlewRadians){
                setOrientVectorContents(targetOrientVector);
                targetOrientVector = null;
                return;
            }

            // build 2d models of our angles. We're going to use this to see what to scale the fullSlewVec by.
            HelperVec sourceHvec = new HelperVec("sourceHvec", 0);
            HelperVec targetHvec = new HelperVec("targetHvec", fullSlewRadians);
            HelperVec stepHvec = new HelperVec("stepHvec", maxSlewRadians);
            HelperVec sourceToTargHvec = new HelperVec("sourceToTargHvec", sourceHvec, targetHvec);

//            System.out.println(sourceHvec.toString());
//            System.out.println(targetHvec.toString());
//            System.out.println(stepHvec.toString());
//            System.out.println(sourceToTargHvec.toString());

            // solve the intersection of the line between the vector formed by the (1,0)(cos(fullSlewRadians), sin(fullSlewRadians) and the line
            // (0, 0) (cos(maxSlewRadians), sin(maxSlewRadians). From this we can determine how much to adjust out n-dimensional vectors
            double[][] values = {{-sourceToTargHvec.m, 1}, {-stepHvec.m, 1}};
            double[] rhs = {sourceToTargHvec.b, stepHvec.b};
            Array2DRowRealMatrix mat = new Array2DRowRealMatrix(values);
            DecompositionSolver solver = new LUDecomposition(mat).getSolver();
            RealMatrix invMat = solver.getInverse();
            ArrayRealVector vec = new ArrayRealVector(rhs);
            double x = vec.dotProduct(invMat.getRowVector(0));
            double y = vec.dotProduct(invMat.getRowVector(1));
            HelperVec sourceToStepVec = new HelperVec("sourceToStepVec", x, y, sourceHvec.x, sourceHvec.y);

//            System.out.println("Matrix: "+mat.toString());
//            System.out.println("Inverse Matrix: "+invMat.toString());
//            System.out.printf("intersection: x = %.2f, y = %.2f\n", x, y);
//            System.out.println(sourceToStepVec.toString());

            // get the vector between here and the target
            ArrayRealVector slewVec = targetOrientVector.subtract(unitOrientVector);

            // calculate how far we want to go along this vector and scale
            double scalar = sourceToStepVec.length/sourceToTargHvec.length;
//            System.out.println("slewVec (pre scale)= "+slewVec.toString());
            slewVec.mapMultiplyToSelf(scalar);

            // change our orientation by that amount
//            System.out.println("OrientVector (Before) = "+unitOrientVector.toString());
//            System.out.println("slewVec (post scale)= "+slewVec.toString());
            ArrayRealVector newVec = unitOrientVector.add(slewVec);

            // rescale so that we keep dimensions AND angles
            double newLen = newVec.getNorm();
            if(newLen != 0.0) {
                scalar = 1.0 / newLen;
                newVec.mapMultiplyToSelf(scalar);
                for (int i = 0; i < unitOrientVector.getDimension(); ++i) {
                    double val = newVec.getEntry(i);
                    unitOrientVector.setEntry(i, val);
                    ParticleStatement s = (ParticleStatement) statementList.get(i);
                    s.setVector(val);
//                System.out.println(s.toString());
                }
            }
//            System.out.println("OrientVector (After) = "+unitOrientVector.toString());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ParticleBelief: " + name + ", uvec = "+unitOrientVector+"\n");
        for (Statement s : statementList) {
            ParticleStatement ps = (ParticleStatement)s;
            sb.append("\t" + ps.toString() + "\n");
        }
        return sb.toString();
    }

    public static void main(String[] args){
        double angle = 90;
        while(angle > 10) {
            double radians = Math.toRadians(angle);
            double x = Math.cos(radians);
            double y = Math.sin(radians);
            ParticleBelief pb1 = new ParticleBelief("sourceHeading");
            pb1.addStatement(new ParticleStatement("X", 0, .5));
            pb1.addStatement(new ParticleStatement("Y", 0, 0));
            ParticleBelief pb2 = new ParticleBelief("targetHeading");
            pb2.addStatement(new ParticleStatement("X", 0, x));
            pb2.addStatement(new ParticleStatement("Y", 0, y));

            pb1.setTargetOrientVector(pb2, true);
            pb1.interpolateBoidHeading(angle/2);
            angle /= 2;

            System.out.println(pb1.toString());
        }

    }
}
