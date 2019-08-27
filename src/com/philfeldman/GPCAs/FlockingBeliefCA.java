package com.philfeldman.GPCAs;

import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.Philosophy.ParticleStatement;
import com.philfeldman.Philosophy.Statement;
import com.philfeldman.Utils.RunConfiguration;
import org.apache.commons.math3.linear.ArrayRealVector;


import java.util.*;

/**
 * Created by SooperYooser on 12/26/2016.
 */
public class FlockingBeliefCA extends BaseCA {
    public static int DEFAULT_CLUSTER_ID = -1;
    public enum PARTICLE_TYPE {Boidish, Particleish}


    protected class SpeedHelper{
        double curSpeed;
        double minSpeed;
        double maxSpeed;
        double prevSpeed;
        double[] speedBuffer;
        int index;
        int size;
        double scalar;

        public SpeedHelper(){
            index = 0;
            curSpeed = prevSpeed = 1;
            setMaxMinSpeed(0.5, 1.5);
            size = 10;
            scalar = 1.0;
            init(curSpeed);
        }

        public SpeedHelper(double speed, int size){
            index = 0;
            curSpeed = prevSpeed = speed;
            setMaxMinSpeed(speed/2.0, speed*speed/2.0);
            this.size = size;
            scalar = 1.0;
            init(speed);
        }

        public void init(double speed){
            speedBuffer = new double[size];
            for(int i = 0; i < size; ++i){
                speedBuffer[i] = speed;
            }
        }

        public double getScalar() {
            return scalar;
        }

        public void setScalar(double scalar) {
            this.scalar = scalar;
        }

        public double updateSpeed(double speed){
            prevSpeed = curSpeed;

            index = (index+1)%size;
            speedBuffer[index] = speed;
            curSpeed = 0;
            for(double s : speedBuffer){
                curSpeed += s;
            }
            curSpeed /= size;
            /***/
            if(curSpeed > maxSpeed){
                curSpeed = maxSpeed;
            }
            if(curSpeed < minSpeed){
                curSpeed = minSpeed;
            }
             /***/
            return curSpeed;
        }

        public void setMaxMinSpeed(double min, double max){
            minSpeed = Math.max(min, 0);
            maxSpeed = max;
        }

        public double getCurSpeed() {
            return curSpeed*scalar;
        }

        public double getPrevSpeed() {
            return prevSpeed*scalar;
        }
    }

    protected ParticleBelief curBelief;
    protected ParticleBelief prevBelief;
    protected ParticleBelief.BORDER_TYPE borderType;
    protected boolean handleBorderCollision;
    protected String name;
    protected SpeedHelper speedHelper;
    protected double slewRate;
    protected double influence;
    protected double attraction;
    protected int clusterId;
    protected RunConfiguration config;
    protected Map<FlockingBeliefCA, Double> boidDistanceMap;
    protected List<SourceBeliefCA> sourceBeliefCAList;
    protected ArrayDeque<String> cellNameQueue;

    public FlockingBeliefCA(String name, BIAS bias, RunConfiguration config) {
        super(bias);
        this.config = config;
        speedHelper = new SpeedHelper();
        double vel = config.velocityMean + (rand.nextDouble()*config.velocityVariance - config.velocityVariance/2);
        setSpeed(vel);
        double slew = config.slewRateMean + (rand.nextDouble()*config.slewRateVariance-config.slewRateVariance/2.0);
        setSlewRate(slew);
        influence = 0;
        attraction = 0;
        this.name = name;
        curBelief = new ParticleBelief(name+"_belief");
        prevBelief = new ParticleBelief(curBelief);
        borderType = config.borderType;
        setClusterId(DEFAULT_CLUSTER_ID);
        sourceBeliefCAList = null;
        cellNameQueue = new ArrayDeque<>(config.tailLength);
    }

    public FlockingBeliefCA(String name, double speed, double slewRate, BIAS bias, RunConfiguration config){
        this(name, bias, config);
        speedHelper = new SpeedHelper(speed, 10);
        setSpeed(speed);
        speedHelper.setMaxMinSpeed(speed+config.velocityVariance, speed-config.velocityVariance);
        setSlewRate(slewRate);
    }

    public double getSpeed() {
        if(config.particleType == PARTICLE_TYPE.Particleish) {
            return 1.0;
        }
        //return speedHelper.getPrevSpeed(); // TODO: Why this and not cur speed?
        return speedHelper.getCurSpeed();
    }

    public void setSpeed(double speed) {

        speedHelper.updateSpeed(Math.abs(speed));
    }

    public double getSlewRate() {
        return slewRate;
    }

    public double getInfluence() {
        return influence;
    }

    public void setAttraction(double attraction) {
        this.attraction = attraction;
    }

    public double getAttraction() {
        return attraction;
    }

    public void setSlewRate(double slewRate) {
        this.slewRate = Math.abs(slewRate);
    }

    public void setConfig(RunConfiguration config) {
        this.config = config;
    }

    public void setSourceBeliefCAList(ArrayList<SourceBeliefCA> sourceBeliefCAList) {
        this.sourceBeliefCAList = sourceBeliefCAList;
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public void setBorderType(ParticleBelief.BORDER_TYPE borderType) {
        this.borderType = borderType;
    }

    public String getCurCellName(){
        return cellNameQueue.peek();
    }
    public String getCellName() {
        Iterator<String> it = cellNameQueue.iterator();
        String retStr = it.next();
        while(it.hasNext()){
            retStr += ("||"+it.next());
        }
        return retStr;
    }

    public void setCellName(String cellName) {
        if(cellNameQueue.contains(cellName)) {
            cellNameQueue.remove(cellName);
        }
        cellNameQueue.push(cellName);
        while (cellNameQueue.size() > config.tailLength){
            cellNameQueue.removeLast();
        }
    }

    protected void axisMove(ParticleStatement particleStatement, double deltaTime){
        double vec = particleStatement.getVector();
        double pos = particleStatement.getValue()+vec*deltaTime*getSpeed();

        /*** Simple hack to keep particles on the viewing area ***/

        switch (borderType) {
            case REFLECTIVE:
            if (Math.abs(pos) > config.stageLimit) {
                particleStatement.setVector(-vec);
            }
            break;
            case RESPAWN:
                if (Math.abs(pos) > config.stageLimit) {
                    handleBorderCollision = true;
                }
            break;
            case RESTORING_FORCE:
                if (Math.abs(pos) > config.forceOnsetPoint) {
                    // do fancy math
                }
                break;
            default: // do nothing, same as NONE
        }

        particleStatement.setValue(pos);
    }
    /**
     * Behaviors that depend on the interactions with the population of other agents
     * @param otherCAList
     * @param groupRadius
     */
    public void localBehavior(List<FlockingBeliefCA> otherCAList, double groupRadius){

        ArrayRealVector unitOrientVector = getPrevBelief().calcUnitOrientVector();
        // set up the local position and orientation vecs. These will have the same content as the
        // curBelief, so we use it as a template and then just zero them out.
        ParticleBelief targetBelief = new ParticleBelief(getCurBelief());

        ArrayRealVector localAveragePosVec = targetBelief.calcPosVector();
        localAveragePosVec.set(0);
        ArrayRealVector localAttractionVec = targetBelief.calcPosVector();
        localAttractionVec.set(0);
        ArrayRealVector localAverageHeadingVec = targetBelief.calcOrientVector();
        localAverageHeadingVec.set(0);
        double avgSpeed = 0;

        // iterate over all the agents that we interact with and add up all their headings, positions
        // and speed based on the range. Count will keep track of how many agents we've interacted with
        double totalWeight = 0;
        ArrayList<CAInfluences> infList = new ArrayList<>();
        CAInfluences agentInfluences = new CAInfluences(CAInfluences.SourceType.AGENT);
        CAInfluences sourceInfluences = new CAInfluences(CAInfluences.SourceType.SOURCE);
        infList.add(agentInfluences);
        infList.add(sourceInfluences);

        ParticleBelief ppb = getPrevBelief();
        if(ppb.isHerding()){
            lprint("herding: "+ppb.getName());
        }

        for (FlockingBeliefCA otherCA : otherCAList) {

            // make it a little cleaner to lay out when we will interact with another agent
            boolean interact = otherCA.getState() != STATE.INACTIVE;
            interact &= config.crossBiasInteraction || (getBias() == otherCA.getBias());

            if(interact) {
                ParticleBelief otherBelief = otherCA.getPrevBelief();
                ArrayRealVector otherUnitOrientVector = otherBelief.calcUnitOrientVector();

                double interAgentAlignment = 1.0;
                if (config.alignmentScalar > 0) {
                    double interAgentDotProduct = unitOrientVector.dotProduct(otherUnitOrientVector);
                    double cosTheta = Math.min(1.0, interAgentDotProduct);
                    double beliefAlignment = Math.toDegrees(Math.acos(cosTheta));
                    interAgentAlignment = (1.0 - beliefAlignment / 180.0);
                }

                double dist = ppb.getDistanceTo(otherBelief);

                boolean herderExemption = false;
                if(ppb.isHerding() && otherBelief.isHerding() ) {
                    lprint("herding: "+ppb.getName()+", "+otherBelief.getName());
                    herderExemption = true;
                }

                // don't compare two herding agents. They would already be coordinated
                if(!herderExemption){

                    double headingRadius = groupRadius * otherBelief.getWeight();

                    // heading alignment
                    if (dist != -1 && dist < headingRadius) {
                        // the influence of an agent on us is propotional to the agent's weight and inversely proportional to our weight
                        double distanceScalar = 1.0 - dist / headingRadius;
                        double influence = distanceScalar * otherBelief.getWeight();
                        if (config.alignmentScalar > 0){
                            influence *= (interAgentAlignment * config.alignmentScalar);
                        }

                        avgSpeed += otherCA.getSpeed() * otherBelief.getWeight();
                        totalWeight += otherBelief.getWeight();

                        ArrayRealVector vec = otherBelief.calcPosVector();
                        vec.mapMultiplyToSelf(influence);
                        localAveragePosVec = localAveragePosVec.add(vec);

                        //vec = otherBelief.calcUnitOrientVector();
                        vec = otherBelief.calcOrientVector();
                        vec.mapMultiplyToSelf(influence);
                        localAverageHeadingVec = localAverageHeadingVec.add(vec);

                        agentInfluences.addInfluencer(otherCA.getName(), influence);
                   }
                }

                // social attraction, based on if you're in a source
                if (dist != -1 && dist < config.attractionRadius) {
                    // the influence of an agent on us is propotional to the agent's weight and inversely proportional to our weight
                    double distanceScalar = 1.0 - dist / config.attractionRadius;
                    double otherAttraction = distanceScalar * otherCA.getAttraction();

                    if(otherCA.getAttraction() > 0){
                        totalWeight += otherAttraction;
                        ArrayRealVector av = prevBelief.getVectorTo(otherBelief);
                        av.mapMultiplyToSelf(otherAttraction * otherCAList.size());
                        localAttractionVec.add(av);
                        localAverageHeadingVec = localAverageHeadingVec.add(av);
                    }
                }

            }
        }

        if(ppb.isHerding()){
            lprint("herding: "+ppb.getName());
            agentInfluences.lprint(lprinter);
        }

        // interact with sources, if they exist

        if(sourceBeliefCAList != null){
            setAttraction(0);
            speedHelper.setScalar(1);
            for(SourceBeliefCA sbca : sourceBeliefCAList){
                ParticleBelief spb = sbca.getCurBelief();
                double dist = ppb.getDistanceTo(spb);
                double radius = sbca.getInfluenceRadius();
                //lprint(String.format("%s to %s = %.2f, radius = %.2f", getName(), sbca.getName(), dist, radius));
                if(dist < radius){
                    double unitDistance = dist / radius;
                    double attraction = 1.0 - unitDistance;
                    setAttraction(attraction);
                    speedHelper.setScalar(unitDistance);
                    sourceInfluences.addInfluencer(sbca.getName(), attraction);
                    break;
                }
            }
        }

        // iterate over the statement list for the belief and update the using the normalized vector
        if(totalWeight > 0) {
            // reduce the large values of all the summings to this agent's fraction of that
            double scalar = 1.0/totalWeight;
            avgSpeed *= scalar;
            localAverageHeadingVec.mapMultiplyToSelf(scalar);
            localAveragePosVec.mapMultiplyToSelf(scalar);

            // adjust the speed
            //setSpeed(avgSpeed);
            // iterate over all the elements of the target that we are always aiming at
            List<Statement> slist = targetBelief.getSortedList(Belief.ORDERING.NAME);
            for (int i = 0; i < slist.size(); i++) {
                // get the this component of the vector
                double val = localAveragePosVec.getEntry(i);
                double vec = localAverageHeadingVec.getEntry(i);
                if(Double.isNaN(val) || Double.isNaN(vec)){
                    System.out.println("FlockingBeliefCA.localBehavior() NaN problem: "+toString()+"\n");
                }else {
                    // if it's good, then add the component
                    ParticleStatement s = (ParticleStatement) slist.get(i);
                    s.setValue(val);
                    s.setVector(vec);
                }
            }


            //targetBelief.calcUnitOrientVector();
            targetBelief.calcOrientVector();
            targetBelief.calcPosVector();

            setTargetBelief(targetBelief);
        }
        setInfluencesList(infList);
//      lprint("Local flock heading: " + localAverageHeadingVec.toString());
//      lprint("Local flock position: " + localAveragePosVec.toString());
    }

    /**
     * Basic local behaviors
     * @param deltaTime
     */
    public void behavior(double deltaTime){
        if(state == STATE.ACTIVE) {
            List<Statement> slist = curBelief.getSortedList(Belief.ORDERING.NAME);

            // interpolate headings
            if(config.particleType == PARTICLE_TYPE.Particleish) {
                curBelief.updateParticleHeading(1.0);
            }else{
                curBelief.interpolateBoidHeading(getSlewRate() * deltaTime);
            }

            // move
            handleBorderCollision = false;
            for (Statement s : slist) {
                ParticleStatement ps = (ParticleStatement) s;
                axisMove(ps, deltaTime);
            }
            if(handleBorderCollision){
                switch (borderType) {
                    case RESPAWN:
                    for (Statement s : slist) {
                        ParticleStatement ps = (ParticleStatement) s;
                        ps.setValue(rand.nextDouble() * 2.0 - 1.0);
                        ps.setVector(rand.nextDouble()*2.0-1.0);
                    }
                    break;
                    case LETHAL: // mark as 'dead'
                    default:
                }
            }

            // reset
            curBelief.calcUnitOrientVector();
            //curBelief.calcOrientVector();
            curBelief.calcPosVector();
        }
    }

    public void updatePrevState(){
        prevBelief.deepUpdate(curBelief);
    }

    public void setTargetBelief(ParticleBelief targetBelief){
        if(targetBelief != null) {
            if(config.particleType == PARTICLE_TYPE.Boidish) {
                curBelief.setTargetOrientVector(targetBelief, true);
            }else{
                curBelief.setTargetOrientVector(targetBelief, false);
            }
        }
    }

    protected static void addCoordinate(ParticleBelief b, ParticleStatement s, double initialWeight){
        ParticleStatement myParticleStatement;
        if(b.hasStatement(s.getName())){
            myParticleStatement = (ParticleStatement)b.getStatement(s.getName());
            myParticleStatement.setWeight(myParticleStatement.getWeight()+ParticleBelief.STEP_DELTA);
        }else {
            myParticleStatement = new ParticleStatement(s);
            myParticleStatement.setWeight(initialWeight);
            b.addStatement(myParticleStatement);
        }
    }

    public void addCoordinate(ParticleStatement s, double initialWeight){
        addCoordinate(curBelief, s, initialWeight);
    }

    public String getName() {
        return name;
    }

    public ParticleBelief getCurBelief() {
        return curBelief;
    }

    public ParticleBelief getPrevBelief(){
        return prevBelief;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n"+ curBelief.getName()+"\n");
        List<Statement> slist = curBelief.getSortedList(ParticleBelief.ORDERING.NAME);
        for(Statement s : slist){
            sb.append(String.format("CA '%s': %s\n", name, s.toString()));
        }
        return sb.toString();
    }

    public static void main(String[] args){
        boolean b = true;
        System.out.println("b = "+b);
        b &= false;
        System.out.println("b = "+b);
        b = true;
        System.out.println("b = "+b);
        b &= true;
        System.out.println("b = "+b);
        b = false;
        System.out.println("b = "+b);
        b &= false;
        System.out.println("b = "+b);
        b &= true;
        System.out.println("b = "+b);
    }
}
