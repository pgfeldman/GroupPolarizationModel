package com.philfeldman.Philosophy;

import java.util.*;

/**
 * Created by philip.feldman on 7/25/2016.
 */
public class Belief {
    public class CollectionCompare {
        double mismatches = 0;
        double total = 0;
        double matches = 0;
    }

    public static enum ORDERING {NONE, NAME, WEIGHT, CREDIBILITY, ACCURACY}
    public static double STEP_DELTA = 1;
    public static double INITIAL_WEIGHT = 1.0;
    public static double MINIMUM_WEIGHT = 0.1;


    protected String name;
    protected double weight;
    protected ArrayList<Statement> statementList;
    protected HashMap<String, Statement> statementMap;
    protected Comparator<Statement> weightComparator;
    protected Comparator<Statement> nameComparator;
    protected Comparator<Statement> credibilityComparator;
    protected Comparator<Statement> accuracyComparator;
    protected ORDERING lastOrdering;
    protected Random rand;

    public Belief(String n) {
        rand = new Random();
        lastOrdering = ORDERING.NONE;
        name = n;
        weight = INITIAL_WEIGHT;
        statementList = new ArrayList<>();
        statementMap = new HashMap<>();
        weightComparator = new Comparator<Statement>() {
            @Override
            public int compare(Statement o1, Statement o2) {
                if (o1.getWeight() > o2.getWeight()) {
                    return -1;
                } else if (o1.getWeight() < o2.getWeight()) {
                    return 1;
                }
                return 0;
            }
        };

        accuracyComparator = new Comparator<Statement>() {
            @Override
            public int compare(Statement o1, Statement o2) {
                if (o1.accuracy > o2.accuracy) {
                    return -1;
                } else if (o1.accuracy < o2.accuracy) {
                    return 1;
                }
                return 0;
            }
        };

        credibilityComparator = new Comparator<Statement>() {
            @Override
            public int compare(Statement o1, Statement o2) {
                if (o1.credibility > o2.credibility) {
                    return -1;
                } else if (o1.credibility < o2.credibility) {
                    return 1;
                }
                return 0;
            }
        };

        nameComparator = new Comparator<Statement>() {
            @Override
            public int compare(Statement o1, Statement o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
    }

    public Belief(Belief toCopy){
        this(toCopy.name);
        weight = toCopy.getWeight();
        lastOrdering = toCopy.lastOrdering;
        statementList = new ArrayList<>();
        statementMap = new HashMap<>();
        for(Statement sToCpy : toCopy.statementList){
            Statement s = new Statement(sToCpy);
            statementList.add(s);
            statementMap.put(s.getName(), s);
        }

    }

    public String getName() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void addStatement(Statement f) {
        statementList.add(f);
        statementMap.put(f.getName(), f);
        lastOrdering = ORDERING.NONE;
    }

    public void deleteStatement(String name){
        if(statementMap.containsKey(name)){
            Statement s = statementMap.get(name);
            statementList.remove(s);
            statementMap.remove(name);
            lastOrdering = ORDERING.NONE;
        }
    }

    public void deleteStatement(String name, double probability){
        if(rand.nextDouble() < probability){
            deleteStatement(name);
        }
    }

    public boolean hasStatement(String fname) {
        return statementMap.containsKey(fname);
    }

    public Statement getStatement(String fname) {
        if (statementMap.containsKey(fname)) {
            return statementMap.get(fname);
        }
        return null;
    }

    public double compare(Belief targBelief) {
        CollectionCompare cc = new CollectionCompare();

        // look for all matches and partial matches from teh target's point of view
        for (Statement targStatement : targBelief.getList()) {
            if (statementMap.containsKey(targStatement.getName())) {
                Statement sourceStatement = statementMap.get(targStatement.getName());
                double matches = Math.min(sourceStatement.getWeight(), targStatement.getWeight());
                cc.matches += matches;
                cc.mismatches += Math.max(sourceStatement.getWeight(), targStatement.getWeight()) - matches;

            } else {
                cc.mismatches += targStatement.getWeight();
            }
        }

        // look for all mismatches from our point of view
        Map<String, Statement> targMap = targBelief.getMap();
        for (Statement sourceStatement : getList()) {
            if (!targMap.containsKey(sourceStatement.getName())) {
                cc.mismatches += sourceStatement.getWeight();
            }
        }

        cc.total = cc.matches+cc.mismatches;
        if(cc.total == 0){
            return 1;
        }

        double ratio = cc.matches/cc.total;

        return ratio;
    }

    private boolean coinFlip(double probability){
        return(rand.nextDouble() < probability);
    }

    // first pass just adjusts weights, no inserts or deletes
    public void adjustBeliefStatements(Belief targBelief, double pursuasiveness){
        if(statementList.size() == 0){
            if(targBelief.getList().size() == 0) {
                return;
            }else{
                for(Statement ts : targBelief.getList()){
                    addStatement(ts);
                }
            }
        }
        double stepDelta = STEP_DELTA;
        if(coinFlip(.1)){ // TODO: make this adjustable/read in.
            stepDelta *= -1;
        }
        // pick one of our beliefs at random, look for it in the target curBelief. If it's there, move one unit towards its value
        int index = rand.nextInt(statementList.size());
        Statement myStatement = statementList.get(index);
            if (targBelief.hasStatement(myStatement.getName())) {
                Statement targStatement = targBelief.getStatement(myStatement.getName());
                if (myStatement.getWeight() < targStatement.getWeight()) {
                    myStatement.addWeight(stepDelta);
                } else if (myStatement.getWeight() > targStatement.getWeight()) {
                    myStatement.addWeight(-stepDelta);
                }
            }
        //}

        // pick one of their beliefs at random, look for it in the target curBelief. If it's there, move one unit towards its value
        List<Statement> targStatementList = targBelief.getSortedList(ORDERING.NONE);
        if(targStatementList.size() > 0) {
            index = rand.nextInt(targStatementList.size());
            Statement targStatement = targStatementList.get(index);
                if (hasStatement(targStatement.getName())) {
                    myStatement = getStatement(targStatement.getName());
                    if (myStatement.getWeight() < targStatement.getWeight()) {
                        myStatement.addWeight(stepDelta);
                    } else if (myStatement.getWeight() > targStatement.getWeight()) {
                        myStatement.addWeight(-stepDelta);
                    }
                } else {
                    // add this statement to ours
                    Statement s = new Statement(targStatement);
                    s.setWeight(INITIAL_WEIGHT);
                    addStatement(s);
                }
            //}
        }

        // Last, make sure that none of our weights drop below 1.0;
        for(Statement s : statementList){

            if(coinFlip(0.0)){
                s.addWeight(stepDelta);
            }
            s.setWeight(Math.max(MINIMUM_WEIGHT, s.getWeight()));
        }
    }

    public Map<String, Statement> getMap() {
        return statementMap;
    }

    public List<Statement> getList() {
        return statementList;
    }

    public List<Statement> getSortedList(ORDERING o) {
        if(o == lastOrdering){ // don't sort unless we need to.
            return statementList;
        }

        switch (o) {
            case ACCURACY:
                statementList.sort(accuracyComparator);
                break;
            case CREDIBILITY:
                statementList.sort(credibilityComparator);
                break;
            case WEIGHT:
                statementList.sort(weightComparator);
                break;
            case NAME:
                statementList.sort(nameComparator);
                break;
            default: // do nothing
        }
        return statementList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Belief: " + name + "\n");
        for (Statement s : statementList) {
            sb.append("\t" + s.toString() + "\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {

    }
}
