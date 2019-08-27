package com.philfeldman.Philosophy;

/**
 * Created by philip.feldman on 7/25/2016.
 */
public class Statement {
    protected String name;
    protected double weight;
    protected double credibility;
    protected double accuracy;

    public Statement() {
    }

    public Statement(String n, double w, double c, double a){
        name = n;
        weight = w;
        credibility = c;
        accuracy = a;
    }


    public Statement(Statement f){
        name = f.getName();
        weight = f.getWeight();
        credibility = f.getCredibility();
        accuracy = f.getAccuracy();
    }

    public String getName() {
        return name;
    }


    public double getWeight() {
        return weight;
    }

    public double getCredibility() {
        return credibility;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setWeight(double weight) {
        this.weight = weight;
        //System.out.println(name+": Statement.setWeight("+weight+")");
    }

    public void addWeight(double weight ){
        this.weight += weight;
    }

    public double compare(Statement wt){
        if(wt.getName().equals(name)){
            return Math.abs(wt.getWeight()-weight);
        }
        return -1;
    }

    @Override
    public String toString() {
        return String.format("Statement %s: weight = %.2f, credibility = %.2f, accuracy = %.2f", name, weight, credibility, accuracy);
    }
}
