package com.philfeldman.MediatedInfo;

import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.FlockingBeliefCA;
import com.philfeldman.Graphics.FlockingShape;

import java.util.HashMap;

public class CellAccumulator {
    public static double DEFAULT_DECAY_FACTOR = 0.999;
    protected HashMap<String, Double> agentCountMap;
    protected HashMap<BaseCA.BIAS, Double> biasCountMap;
    protected String name;

    public CellAccumulator(String name){
        agentCountMap = new HashMap<>();
        biasCountMap = new HashMap<>();
        this.name = name;
    }

    public void add(FlockingShape fs){
        FlockingBeliefCA fbca = fs.getAgent();
        fbca.setCellName(name);
        double count = 0;
        if(agentCountMap.containsKey(fbca.getCurCellName())){
            count = agentCountMap.get(fbca.getCurCellName());
        }
        count += fs.getDeltaTime();
        agentCountMap.put(fbca.getCurCellName(), count);

        count = 0;
        if(biasCountMap.containsKey(fbca.getBias())){
            count = biasCountMap.get(fbca.getBias());
        }
        count += fs.getDeltaTime();
        biasCountMap.put(fbca.getBias(), count);
    }

    public void decay(double decayFactor){
        for(String name : agentCountMap.keySet()){
            double val = agentCountMap.get(name);
            agentCountMap.put(name, val*decayFactor);
        }

        for(BaseCA.BIAS bias : biasCountMap.keySet()){
            double val = biasCountMap.get(bias);
            biasCountMap.put(bias, val*decayFactor);
        }
    }

    public double getTotalAgentValue(){
        double total = 0;
        for(Double c : agentCountMap.values()){
            total += c;
        }
        return total;
    }

    public double getBiasValue(BaseCA.BIAS bias){
        if(biasCountMap.containsKey(bias)) {
            return biasCountMap.get(bias);
        }
        return 0;
    }

    public HashMap<String, Double> getAgentCountMap() {
        return agentCountMap;
    }

    public HashMap<BaseCA.BIAS, Double> getBiasCountMap() {
        return biasCountMap;
    }

    @Override
    public String toString() {
        return String.format("%.2f", getTotalAgentValue());
    }
}
