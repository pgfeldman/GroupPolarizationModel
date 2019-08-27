package com.philfeldman.Graphics;

import com.philfeldman.Philosophy.Belief;
import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.BaseBeliefCA;
import javafx.scene.paint.Paint;

import java.util.Comparator;

import static com.philfeldman.GPCAs.BaseBeliefCA.ATTRACT_THRESHOLD;
import static com.philfeldman.GPCAs.BaseBeliefCA.REJECT_THRESHOLD;

/**
 * Created by philip.feldman on 8/2/2016.
 */
public class BeliefAgentShape extends ForceShape {
    protected BaseBeliefCA cbca;
    protected boolean adjustBeliefs;
    protected double pursuasiveness;
    protected double antiBeliefScalar;
    protected double attractThreshold = ATTRACT_THRESHOLD;
    protected double rejectThreshold = REJECT_THRESHOLD;
    protected Comparator<ForceShape> originDistanceComparator;
    protected BeliefAgentShape influencerBas;


    public BeliefAgentShape(String n, Paint c1, Paint c2) {
        super(n, c1, c2);
        adjustBeliefs = false;
        pursuasiveness = 1;
        antiBeliefScalar = 1;
        originDistanceComparator = new Comparator<ForceShape>() {
            @Override
            public int compare(ForceShape o1, ForceShape o2) {
                if(o1.getOriginDistance() < o2.getOriginDistance()){
                    return 1;
                }else if (o1.getOriginDistance() > o2.getOriginDistance()){
                    return -1;
                }
                return 0;
            }
        };
    }

    public double getPursuasiveness() {
        return pursuasiveness;
    }

    public void setPursuasiveness(double pursuasiveness) {
        this.pursuasiveness = pursuasiveness;
    }

    public double getAntiBeliefScalar() {
        return antiBeliefScalar;
    }

    public void setAntiBeliefScalar(double antiBeliefScalar) {
        this.antiBeliefScalar = antiBeliefScalar;
    }

    public void setAttractThreshold(double attractThreshold) {
        this.attractThreshold = attractThreshold;
    }

    public void setRejectThreshold(double rejectThreshold) {
        this.rejectThreshold = rejectThreshold;
    }

    public void setAdjustBeliefs(boolean adjustBeliefs) {
        this.adjustBeliefs = adjustBeliefs;
    }

    @Override
    public void behavior(double e) {
        super.behavior(e);
        if(adjustBeliefs) {
            fitnessTest();
        }
    }

    double calcCost(SmartShape target){
        // a naive implementation based on distance
        ShapeRelations sr = distanceMap.get(target.getName());
        double dist = sr.hSquared;
        double cost = 1.0 - (dist / maxDist);
        return cost;
    }

    protected void fitnessTest(){

        if(maxDist > 0) {

            switch (cbca.getBias()) {
                case AVOIDER:
                    break;
                case CONFIRMER:
                    break;
                case RED_BIAS:
                    break;
                case SOURCE:
                    break;
                default: // CONTROL
                    for(ShapeRelations sr : distanceList){
                        if(sr.shape == this){
                            continue;
                        }
                        BeliefAgentShape targ = (BeliefAgentShape)sr.shape;
                        Belief srcB = getAgent().getBelief();
                        BeliefAgentShape tbas = (BeliefAgentShape)sr.shape;
                        Belief targB = tbas.getAgent().getBelief();

                        BaseBeliefCA sourceCA = getAgent();

                        if(sr.similarity > attractThreshold) {
                            srcB.adjustBeliefStatements(targB, 1);
                        }else if (sr.similarity < rejectThreshold){
                            srcB = sourceCA.getAntiBelief();
                            srcB.adjustBeliefStatements(targB, 1);
                        }
                        sourceCA.rectifyBeliefs(BaseBeliefCA.DELETE_MODE.KEEP_BELIEF);
                    }

            }
        }else{
            dprint("BeliefAgentShape.fitnessTest(): maxDist = 0");
        }

    }

    @Override
    protected double calcSimilarity(ForceShape targ) {
        BeliefAgentShape bas = (BeliefAgentShape)targ;
        Belief b1 = cbca.getBelief();
        Belief b2 = bas.getAgent().getBelief();
        double s = b1.compare(b2);

        return s;
    }

    @Override
    protected double calcDifference(ForceShape targ) {
        // this is the 'default' difference
        BeliefAgentShape bas = (BeliefAgentShape)targ;
        Belief b1 = cbca.getBelief();
        Belief b2 = bas.getAgent().getBelief();
        double baseDif = 1 - b1.compare(b2);

        // now we add the similarity from our antiBelif and the target Belief
        b1 = cbca.getAntiBelief();
        double antiSim = 0;
        if(b1.getList().size() > 0){
            //antiSim = 1;
            antiSim = b1.compare(b2)*antiBeliefScalar;
        }

        return antiSim + baseDif;
    }

    public BaseBeliefCA setAgent(BaseBeliefCA cbca) {
        this.cbca = cbca;
        return this.cbca;
    }

    public BaseBeliefCA setAgent(BaseCA.BIAS bias){
        cbca = new BaseBeliefCA(name, bias);
        return this.cbca;
    }

    public BaseBeliefCA getAgent(){
        return cbca;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String str = super.toString();
        if(influencerBas != null){
            str += ", influencer = "+influencerBas.getName();
        }
        sb.append(str);
        sb.append(cbca.toString());
        return sb.toString();
    }

}
