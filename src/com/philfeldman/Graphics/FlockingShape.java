package com.philfeldman.Graphics;

import com.philfeldman.GPCAs.FlockingBeliefCA;
import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.Philosophy.ParticleStatement;
import com.philfeldman.Philosophy.Statement;
import com.philfeldman.Utils.RunConfiguration;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SooperYooser on 12/26/2016.
 */
public class FlockingShape extends SmartShape {
    RunConfiguration config;
    protected String flockName;
    protected FlockingBeliefCA myAgent;
    protected List<FlockingShape> flockingShapeList;
    protected double groupRadius;
    protected double deltaTime;
    protected double prevTime;
    protected String xDimensionName;
    protected String yDimensionName;

    public FlockingShape(String n, String fn, Paint c1, Paint c2) {
        super(n, c1, c2);
        flockingShapeList = null;
        flockName = fn;
        groupRadius = 0.1;

        xDimensionName = "X";
        yDimensionName = "Y";
    }

    public String getFlockName() {
        return flockName;
    }

    @Override
    public double[] getPoint() {
        ParticleBelief pb = myAgent.getCurBelief();
        List<Statement> sList = pb.getSortedList(Belief.ORDERING.NAME);
        double[] pArray = new double[sList.size()];
        for(int i = 0; i < sList.size(); ++i){
            ParticleStatement ps = (ParticleStatement)sList.get(i);
            pArray[i] = ps.getValue();
        }
        return pArray;
    }

    public void setDimensionNames(String xName, String yName){
        xDimensionName = xName;
        yDimensionName = yName;
    }

    public void setGroupRadius(double groupRadius) {
        this.groupRadius = groupRadius;
    }

    @Override
    public void behavior(double elapsed) {
        super.behavior(elapsed);

        // calculate the headings and speeds based on the local environment
        if(flockingShapeList != null) {
            List<FlockingBeliefCA> otherCAList = new ArrayList<>();
            for (FlockingShape fs : flockingShapeList) {
                if (fs != this) {
                    FlockingBeliefCA otherCA = fs.getAgent();
                    otherCAList.add(otherCA);
                }
            }
            myAgent.localBehavior(otherCAList, groupRadius);
        }
        calcDeltaTime(elapsed);
        // move the shape as calculated above.
        myAgent.behavior(deltaTime);
        setSizeScalar(myAgent.getInfluence()+DEFAULT_SCALE);
        calcPosFromBelief();
    }

    protected void calcDeltaTime(double elapsed){
        deltaTime = elapsed - prevTime;
        prevTime = elapsed;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public double getMappingX(){
        ParticleBelief b = myAgent.getCurBelief();
        ParticleStatement s = b.getStatement(xDimensionName);
        if(Double.isNaN(s.getValue())){
            System.out.println("FlockingShape.getMappingX() NaN error: "+toString());
        }
        return s.getValue()* pixelWorldScalar;
    }

    public double getMappingY(){
        ParticleBelief b = myAgent.getCurBelief();
        ParticleStatement s = b.getStatement(yDimensionName);
        if(Double.isNaN(s.getValue())){
            System.out.println("FlockingShape.getMappingY() NaN error: "+toString());
        }
        return s.getValue()* pixelWorldScalar;
    }

    public double getMappingAngleRadians(){
        ParticleBelief b = myAgent.getCurBelief();
        ParticleStatement xs = b.getStatement(xDimensionName);
        ParticleStatement ys = b.getStatement(yDimensionName);
        double radians = Math.atan2(ys.getVector(), xs.getVector());
        return radians;
    }

    public void calcPosFromBelief(){
        setPos(getMappingX(), getMappingY());
        setAngle(Math.toDegrees(getMappingAngleRadians()));
    }

    public FlockingBeliefCA setAgent(FlockingBeliefCA cbca) {
        this.myAgent = cbca;
        return this.myAgent;
    }

    public FlockingBeliefCA setAgent(BaseCA.BIAS bias, RunConfiguration config){
        this.config = config;
        myAgent = new FlockingBeliefCA(name, bias, config);
        return this.myAgent;
    }

    public void setFlockingShapeList(List<FlockingShape> flockingShapeList) {
        this.flockingShapeList = flockingShapeList;
    }

    public FlockingBeliefCA getAgent(){
        return myAgent;
    }

    @Override
    public void customDrawStep(GraphicsContext gc) {
        super.customDrawStep(gc);
        // draw the attraction oval
        if(myAgent.getAttraction() > 0) {
            gc.setFill(Color.WHITE);
            double offset = bbox.getMinY() / 2.0 * myAgent.getAttraction();
            double size = bbox.getHeight() / 2.0 * myAgent.getAttraction();
            gc.fillOval(offset, offset, size, size);
            gc.strokeOval(offset, offset, size, size);
        }
        if(config != null && config.debugLevel > 1) {
            double offset = -groupRadius * pixelWorldScalar;
            double size = groupRadius * pixelWorldScalar * 2.0;
            gc.strokeOval(offset, offset, size, size);
        }
    }

    @Override
    public String toString() {
        String s = name+": "
                +"("+ flockName+") groupRadius = "+String.format("%.2f ,", groupRadius)
                + myAgent.getCurBelief().toString()+"\n";
        return s;
    }
}
