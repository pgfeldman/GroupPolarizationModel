package com.philfeldman.GPCAs;

import java.util.ArrayList;

/**
 * Created by philip.feldman on 7/21/2016.
 */
public class ConfirmerCASingleDist extends SingleDistributionCA {
    public ConfirmerCASingleDist() {
        super(BIAS.CONFIRMER);
    }

    @Override
    public void findValues(ArrayList<SingleDistributionCA> caList, int numSamples) {
        //super.findValues(caList, numSamples);

        for(int i = 0; i < numSamples; ++i){
            int index = rand.nextInt(caList.size());
            SingleDistributionCA bca = caList.get(index);
            double max = bca.getMean()+bca.getSD();
            double min = bca.getMean()-bca.getSD();
            if((getMean() < max) && getMean() > min){
                addValue(bca.getMean());
            }
        }
    }
}
