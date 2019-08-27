package com.philfeldman.Graphics;

import com.philfeldman.GPCAs.SourceBeliefCA;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.Utils.WordEmbedding;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class SourceShape extends SmartShape {
    protected String xDimensionName;
    protected String yDimensionName;
    protected double groupRadiusScalar;
    protected SourceBeliefCA myAgent;


    public SourceShape(String n, Paint c1, Paint c2) {
        super(n, c1, c2);
        groupRadiusScalar = 1.0;
        selectedColor = new Color(1, 1, 0, 0.5);
        xDimensionName = "s_0";
        yDimensionName = "s_1";
    }

    public void setAgent(SourceBeliefCA ca){
        myAgent = ca;
        WordEmbedding we = myAgent.getWordEmbedding();
        double size = myAgent.getInfluenceRadius()*2.0;
        setSize(size, size);
        setAngle(0);
        setType(SmartShape.SHAPE_TYPE.OVAL);
        ParticleBelief pb = myAgent.getCurBelief();
        double xpos = pb.getStatement(xDimensionName).getValue();
        double ypos = pb.getStatement(yDimensionName).getValue();
        setPos(xpos* pixelWorldScalar, ypos* pixelWorldScalar);
    }

    @Override
    public void selectedBehavior() {
        super.selectedBehavior();
        double size = myAgent.getInfluenceRadius()*2.0;
        setSize(size* pixelWorldScalar, size* pixelWorldScalar);
    }

    public void setDimensionNames(String xName, String yName){
        xDimensionName = xName;
        yDimensionName = yName;
    }

    public void setGroupRadiusScalar(double scalar) {
        groupRadiusScalar = scalar;
    }

    public SourceBeliefCA getMyAgent() {
        return myAgent;
    }

    @Override
    public String toString() {
        return String.format("%s pos = (%.2f, %.2f), size = %.2f", name, posVec.getX(), posVec.getY(), myAgent.getInfluenceRadius());
    }
}
