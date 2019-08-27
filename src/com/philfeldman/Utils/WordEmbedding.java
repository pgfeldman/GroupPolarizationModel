package com.philfeldman.Utils;

import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

public class WordEmbedding{
    protected String entry;
    protected int count;
    protected ArrayList<Double> coordinates;

    public WordEmbedding(Element root) {
        coordinates = new ArrayList<>();
        entry = root.attributeValue("value");
        Element ce = root.element("count");
        count = Integer.parseInt(ce.attributeValue("value"));
        List<Element> coords = root.element("coordinates").elements();
        for(Element coord : coords){
            try {
                int index = Integer.parseInt(coord.attributeValue("name"));
                double val = Double.parseDouble(coord.attributeValue("value"));
                coordinates.add(val);
            } catch (NumberFormatException e){
                e.printStackTrace();
            }
        }
    }

    public String getEntry() {
        return entry;
    }

    public int getCount() {
        return count;
    }

    public ArrayList<Double> getCoordinates() {
        return coordinates;
    }

    public double getCoordinate(int index){
        return coordinates.get(index);
    }
}
