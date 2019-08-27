package com.philfeldman.GPMain;

import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.Statement;
import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.BaseBeliefCA;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.philfeldman.GPCAs.BaseBeliefCA.ATTRACT_THRESHOLD;
import static com.philfeldman.GPCAs.BaseBeliefCA.REJECT_THRESHOLD;

/**
 * Created by philip.feldman on 7/26/2016.
 */
public class BeliefMain {
    public static int STATEMENT_POP_SIZE = 10;
    public static int STATEMENT_SAMPLE_SIZE = 2;


    protected Belief masterBelief;
    protected HashMap<String, BaseBeliefCA> population;
    protected Random rand;

    public BeliefMain(int numStatements){
        rand = new Random();
        masterBelief = new Belief("masterBelief");
        population = new HashMap<>();
        for(int i = 0; i < numStatements; ++i){
            String name = Integer.toString(i);
            Statement f = new Statement(name, 1, rand.nextDouble(), rand.nextDouble());
            masterBelief.addStatement(f);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Master Beliefs");
        sb.append("\n");
        List<Statement> slist = masterBelief.getSortedList(Belief.ORDERING.ACCURACY);
        for(Statement f : slist){
            sb.append(f.toString());
            sb.append("\n");
        }
        System.out.println(sb);
    }

    public void initCAs(int numCAs){
        for(int i = 0; i < numCAs; ++i){
            String name = "CA_"+i;
            BaseBeliefCA cbca = new BaseBeliefCA(name, BaseCA.BIAS.CONTROL);
            List<Statement> slist = masterBelief.getList();
            while(cbca.statementCount() < STATEMENT_POP_SIZE){
                int index = rand.nextInt(slist.size());
                Statement f = slist.get(index);
                cbca.addStatement(f, Belief.INITIAL_WEIGHT);
            }
            population.put(name, cbca);
        }
    }

    public boolean testSimilarity(){
        boolean stable = true;
        for(String srcName : population.keySet()){
            for(String targName : population.keySet()){
                if(srcName.equals(targName)){
                    continue;
                }
                Belief srcB = population.get(srcName).getBelief();
                Belief targB = population.get(targName).getBelief();
                double similarity = srcB.compare(targB);
                System.out.println(srcName+"/"+targName+" similarity = "+similarity);
                if(similarity > 0.0 && similarity < 1.0){
                    stable &= false;
                }
            }
        }

        return stable;
    }

    public void converge(){
        for(String srcName : population.keySet()){
            BaseBeliefCA sourceCA = population.get(srcName);
            for(String targName : population.keySet()){
                if(srcName.equals(targName)){
                    continue;
                }
                Belief srcB = sourceCA.getBelief();
                BaseBeliefCA targCA = population.get(targName);
                Belief targB = targCA.getBelief();
                double similarity = srcB.compare(targB);
                if(similarity > ATTRACT_THRESHOLD) {
                    srcB.adjustBeliefStatements(targB, 1);
                }else if (similarity < REJECT_THRESHOLD){
                    srcB = sourceCA.getAntiBelief();
                    srcB.adjustBeliefStatements(targB, 1);
                }
                sourceCA.rectifyBeliefs(BaseBeliefCA.DELETE_MODE.KEEP_ANTIBELIEF);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();


        for(String name : population.keySet()){
            sb.append("\n");
            BaseBeliefCA cbca = population.get(name);
            sb.append(name+": "+cbca.toString());
        }

        return sb.toString();
    }

    public static void main(String[] args){
        BeliefMain bm = new BeliefMain(STATEMENT_POP_SIZE);
        bm.initCAs(STATEMENT_SAMPLE_SIZE);
        System.out.print(bm.toString());

        boolean stable = false;
        int i = 0;
        while(!stable && (i < 5)) {
            System.out.println("\n["+(i++)+"]-----------");
            System.out.println(bm.toString());
            stable = bm.testSimilarity();
            bm.converge();
        }
        bm.testSimilarity();
    }
}
