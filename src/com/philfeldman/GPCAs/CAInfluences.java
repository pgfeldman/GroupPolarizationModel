package com.philfeldman.GPCAs;

import com.philfeldman.GPGuis.LivePrintable;

import java.util.ArrayList;

public class CAInfluences {
    class Influence{
        String name;
        double influence;

        public Influence(String name, double influence) {
            this.name = name;
            this.influence = influence;
        }

        @Override
        public String toString() {
            return String.format("%s = %.2f", name, influence);
        }
    }

    public static enum SourceType {AGENT, SOURCE};
    protected SourceType type;
    protected ArrayList<Influence> influenceList;

    public CAInfluences(SourceType type) {
        this.type = type;
        influenceList = new ArrayList<>();
    }

    // Copy ctor
    public CAInfluences(CAInfluences other){
        this.type = other.type;
        influenceList = new ArrayList<>();
        for(Influence inf : other.influenceList){
            addInfluencer(inf.name, inf.influence);
        }
    }

    public void addInfluencer(String name, double val){
        Influence influence = new Influence(name, val);
        influenceList.add(influence);
    }

    public boolean hasInfluences(){
        return (influenceList.size() > 0);
    }

    public void clear(){
        influenceList.clear();
    }

    public String toXMLString(){
        StringBuilder sb = new StringBuilder();
        String str = "\t\t\t<influences type = \""+type+"\">\n";
        sb.append(str);
        for(Influence infl : influenceList){
            str = "\t\t\t\t<influence name = \""+infl.name+"\" val = \""+infl.influence+"\"/>\n";
            sb.append(str);
        }
        str = "\t\t\t</influences>\n";
        sb.append(str);

        return sb.toString();
    }

    public void lprint(LivePrintable lprinter){
        for(Influence influence : influenceList){
            String s = influence.toString();
            if(lprinter != null){
                lprinter.lprint("influence: "+s);
            } else {
                System.out.println("influence: "+s);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Influence influence : influenceList){
            sb.append(influence.toString()+"\n");
        }
        return super.toString();
    }
}
