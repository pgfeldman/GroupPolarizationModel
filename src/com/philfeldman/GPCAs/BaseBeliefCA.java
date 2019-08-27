package com.philfeldman.GPCAs;

import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philip.feldman on 7/26/2016.
 */
public class BaseBeliefCA extends BaseCA {
    public static enum DELETE_MODE {KEEP_BELIEF, KEEP_ANTIBELIEF, LOWEST, RANDOM, LOWEST_THEN_RANDOM}
    public static double ATTRACT_THRESHOLD = 0.5;
    public static double REJECT_THRESHOLD = 0.5;

    protected Belief belief;
    protected Belief antiBelief;
    protected String name;



    public BaseBeliefCA(String name, BIAS bias) {
        super(bias);
        this.name = name;
        belief = new Belief(name+"_belief");
        antiBelief = new Belief(name+"antiBelief");
    }

    protected void addStatement(Belief b, Statement s, double initialWeight){
        Statement myStatement;
        if(b.hasStatement(s.getName())){
            myStatement = b.getStatement(s.getName());
            myStatement.setWeight(myStatement.getWeight()+Belief.STEP_DELTA);
        }else {
            myStatement = new Statement(s);
            myStatement.setWeight(initialWeight);
            b.addStatement(myStatement);
        }
    }

    public void addStatement(Statement s, double initialWeight){
        addStatement(belief, s, initialWeight);
    }

    public void addAntiStatement(Statement s, double initialWeight){
        addStatement(antiBelief, s, initialWeight);
    }

    public void rectifyBeliefs(DELETE_MODE mode){
        List<Statement> slist;
        ArrayList<String> toDelete;
        switch (mode){
            case KEEP_BELIEF:
                slist = getBelief().getSortedList(Belief.ORDERING.NONE);
                toDelete = new ArrayList<>();
                for(Statement s : slist){
                    toDelete.add(s.getName());
                }
                for(String s : toDelete){
                    antiBelief.deleteStatement(s, 0.5);
                }
                break;
            case KEEP_ANTIBELIEF:
                slist = getAntiBelief().getSortedList(Belief.ORDERING.NONE);
                toDelete = new ArrayList<>();
                for(Statement s : slist){
                    toDelete.add(s.getName());
                }
                for(String s : toDelete){
                    belief.deleteStatement(s, 0.5);
                }
                break;
            case RANDOM: break;
            case LOWEST_THEN_RANDOM:
                break;
        }
    }

    public String getName() {
        return name;
    }

    public Belief getBelief() {
        return belief;
    }

    public Belief getAntiBelief() {
        return antiBelief;
    }


    public double statementCount(){
        double count = 0;
        for(Statement s : belief.getList()){
            count += s.getWeight();
        }
        return count;
    }

    public double antistatementCount(){
        double count = 0;
        for(Statement s : antiBelief.getList()){
            count += s.getWeight();
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n"+belief.getName()+"\n");
        List<Statement> slist = belief.getSortedList(Belief.ORDERING.NAME);
        for(Statement s : slist){
            sb.append(String.format("CA '%s': %s\n", name, s.toString()));
        }
        /****
        slist = antiBelief.getSortedList(Belief.ORDERING.NAME);
        for(Statement s : slist){
            sb.append(String.format("AntiCA '%s': %s\n", name, s.toString()));
        }
         ****/
        return sb.toString();
    }
}
