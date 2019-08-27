package com.philfeldman.Philosophy;

/**
 * Created by SooperYooser on 12/26/2016.
 */
public class ParticleStatement extends Statement {
    protected double value;
    protected double vector;


    public ParticleStatement(String n, double w, double c, double a) {
        super(n, w, c, a);
        value = 0;
        vector = 0;
    }

    public ParticleStatement(String n, double val, double vec){
        name = n;
        weight = 1;
        credibility = 1;
        accuracy = 1;
        value = val;
        vector = vec;
    }

    public ParticleStatement(ParticleStatement f) {
        super(f);
        value = f.getValue();
        vector = f.getVector();
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        if(Double.isNaN(value)){
            System.out.println("ParticleStatement.setValue() NaN problem");
        }else {
            this.value = value;
        }
    }

    public double getVector() {
        return vector;
    }

    public void setVector(double vector) {
        if(Double.isNaN(vector)){
            System.out.println("ParticleStatement.setVector() NaN problem");
        }else {
            this.vector = vector;
        }
    }

    @Override
    public String toString() {
        return String.format("ParticleStatement %s: value = %.2f, vector = %.2f, credibility = %.2f, accuracy = %.2f", name, value, vector, credibility, accuracy);
    }
}
