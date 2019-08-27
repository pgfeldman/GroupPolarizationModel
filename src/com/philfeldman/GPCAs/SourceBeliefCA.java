package com.philfeldman.GPCAs;

import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.Philosophy.ParticleStatement;
import com.philfeldman.Philosophy.Statement;
import com.philfeldman.Utils.RunConfiguration;
import com.philfeldman.Utils.WordEmbedding;

import java.util.List;

public class SourceBeliefCA extends BaseCA {
    protected WordEmbedding wordEmbedding;
    protected ParticleBelief curBelief;
    protected RunConfiguration config;

    public SourceBeliefCA(WordEmbedding we) {
        super(BIAS.SOURCE, STATE.ACTIVE);
        wordEmbedding = we;
        curBelief = new ParticleBelief(we.getEntry()+"_belief");
        List<Double> cl = we.getCoordinates();
        for( int i = 0; i < cl.size(); ++i ){
            double val = cl.get(i);
            ParticleStatement s = new ParticleStatement("s_"+i, val, 0);
            curBelief.addStatement(s);
        }
    }


    /**
     * Basic local behaviors
     * @param deltaTime
     */
    public void behavior(double deltaTime){
        if(state == STATE.ACTIVE) {
            curBelief.calcPosVector();
        }
    }

    public void setConfig(RunConfiguration config){
        this.config = config;
    }

    public String getName() {
        return wordEmbedding.getEntry();
    }

    public double getInfluenceRadius(){
        return wordEmbedding.getCount()*config.sourceScalar*0.5; // take half because count is the diameter
    }

    public ParticleBelief getCurBelief() {
        return curBelief;
    }

    public WordEmbedding getWordEmbedding() {
        return wordEmbedding;
    }
}
