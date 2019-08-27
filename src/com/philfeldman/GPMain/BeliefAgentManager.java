package com.philfeldman.GPMain;

import com.philfeldman.Graphics.ClusterCanvas;
import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.Statement;
import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.BaseBeliefCA;
import com.philfeldman.GPGuis.Dprintable;
import com.philfeldman.Graphics.BeliefAgentShape;
import com.philfeldman.Graphics.ResizableCanvas;
import com.philfeldman.Graphics.SmartShape;
import com.philfeldman.utils.math.Labeled2DMatrix;
import com.philfeldman.utils.xmlParsing.Dom4jUtils;
import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by philip.feldman on 8/2/2016.
 */
public class BeliefAgentManager implements Dprintable{
    public class GuiVars{
        public boolean success = false;
        public boolean useRandom = false;
        public double attractionPercent = .1;
        public double repulsionPercent = 10;
        public double antiBeliefScalar = 1;
        public double originScalar = 1;
        public float epsScalar = .1f;
        public double similarityPercent = .5;
        public double dragCoef = 0.1;
    }

    protected ResizableCanvas canvas;
    protected Dprintable dprinter;
    protected Random rand;
    protected Belief allBelief;
    protected Belief redBelief;
    protected Belief greenBelief;
    protected Belief blueBelief;
    protected boolean simRunning;

    public BeliefAgentManager() {
        rand = new Random();
        allBelief = new Belief("positionBelief");
        redBelief = new Belief("redBelief");
        greenBelief = new Belief("greenBelief");
        blueBelief = new Belief("blueBelief");
        for(int i = 0; i < 10; ++i) {
            String big = Integer.toHexString(192 + rand.nextInt(64));
            String small1 = Integer.toHexString(rand.nextInt(128)+16);
            String small2 = Integer.toHexString(rand.nextInt(128)+16);
            String redStr = "#"+big+small1+small2;
            String greenStr = "#"+small1+big+small2;
            String blueStr = "#"+small1+small2+big;
            redBelief.addStatement(new Statement(redStr, 1, 1, 1));
            greenBelief.addStatement(new Statement(greenStr, 1, 1, 1));
            blueBelief.addStatement(new Statement(blueStr, 1, 1, 1));
            allBelief.addStatement(new Statement(redStr, 1, 1, 1));
            allBelief.addStatement(new Statement(greenStr, 1, 1, 1));
            allBelief.addStatement(new Statement(blueStr, 1, 1, 1));
        }
        System.out.println(redBelief.toString());
        System.out.println(greenBelief.toString());
        System.out.println(blueBelief.toString());
    }

    public BeliefAgentManager(ResizableCanvas canvas) {
        this.canvas = canvas;
    }

    public void setCanvas(ResizableCanvas canvas) {
        this.canvas = canvas;
    }

    public void setOriginScalar(double originScalar){
        for (SmartShape ss : canvas.getShapeList()){
            BeliefAgentShape bas = (BeliefAgentShape)ss;
            bas.setOriginScalar(originScalar);
        }
    }

    public void setAntiBeliefScalar(double antiBeliefScalar) {
        for (SmartShape ss : canvas.getShapeList()){
            BeliefAgentShape bas = (BeliefAgentShape)ss;
            bas.setAntiBeliefScalar(antiBeliefScalar);
        }
    }

    public void setAttractionK(double attractionK) {
        for (SmartShape ss : canvas.getShapeList()){
            BeliefAgentShape bas = (BeliefAgentShape)ss;
            bas.setAttractionK(attractionK);
        }
    }

    public void setRepulsionK(double repulsionK){
        for (SmartShape ss : canvas.getShapeList()){
            BeliefAgentShape bas = (BeliefAgentShape)ss;
            bas.setRepulsionK(repulsionK);
        }
    }

    public void setDragCoef(double dragCoef) {
        for (SmartShape ss : canvas.getShapeList()){
            BeliefAgentShape bas = (BeliefAgentShape)ss;
            bas.setDragCoef(dragCoef);
        }
    }
    public void setEpsScalar(float epsScalar) {
        ClusterCanvas cc = (ClusterCanvas)canvas;
        cc.setEpsScalar(epsScalar);
    }

    public void setSimilarityThresholds(double similarity){
        for (SmartShape ss : canvas.getShapeList()) {
            BeliefAgentShape bas = (BeliefAgentShape) ss;
            bas.setAttractThreshold(similarity);
            bas.setRejectThreshold(1-similarity);
        }
    }

    public boolean isSimRunning() {
        return simRunning;
    }

    public void setSimRunning(boolean simRunning) {
        this.simRunning = simRunning;
        List<SmartShape> agentShapes = canvas.getShapeList();
        for(SmartShape ss : agentShapes){
            BeliefAgentShape bam = (BeliefAgentShape)ss;
            ((BeliefAgentShape) ss).setAdjustBeliefs(simRunning);
        }

    }

    protected BeliefAgentShape initAgent(String name, String color, int size){
        return initAgent(name, Paint.valueOf(color), size);

    }

    protected BeliefAgentShape initAgent(String name, Paint paint, int size){
        BeliefAgentShape ss = new BeliefAgentShape(name, paint, Paint.valueOf("black"));
        ss.setSize(size, size);
        ss.setAngle(0);
        ss.setType(SmartShape.SHAPE_TYPE.OVAL);

        return ss;
    }


    public boolean initializeAgents(boolean homogeneous){
        if(canvas == null){
            return false;
        }

        Random rand = new Random();
        String shapeName;
        List<Statement> freindStatementList;
        List<Statement> enemyStatementList = null;

        for(int i = 0; i < 90; ++i) {
            enemyStatementList = null;
            if(homogeneous) {
                freindStatementList = allBelief.getList();
                shapeName = "shape_"+i;
            }else {
                switch (i % 3) {
                    case 0:
                        freindStatementList = redBelief.getList();
                        //enemyStatementList = blueBelief.getList();
                        shapeName = "redShape_" + i;
                        break;
                    case 1:
                        freindStatementList = greenBelief.getList();
                        //enemyStatementList = redBelief.getList();
                        shapeName = "greenShape_" + i;
                        break;
                    default:
                        freindStatementList = blueBelief.getList();
                        //enemyStatementList = greenBelief.getList();
                        shapeName = "blueShape_" + i;
                }
            }

            BeliefAgentShape bas = initAgent(shapeName, "grey", 16);
            bas.setPos(rand.nextDouble()*200-100, rand.nextDouble()*200-100);
            BaseBeliefCA cbca = bas.setAgent(BaseCA.BIAS.CONTROL);

            for(int j = 0; j < 10; ++j) {
                if(true) {
                    Statement friendstatement = freindStatementList.get(rand.nextInt(freindStatementList.size() - 1));
                    cbca.addStatement(friendstatement, Belief.INITIAL_WEIGHT);
                    String cstr = friendstatement.getName();
                    bas.setFillColor(Paint.valueOf(cstr));
                }

                if(enemyStatementList != null){
                    Statement enemyStatement = enemyStatementList.get(rand.nextInt(enemyStatementList.size()-1));
                    cbca.addAntiStatement(enemyStatement, Belief.INITIAL_WEIGHT);
                }
            }
            //System.out.println(ss.toString());

            canvas.addShape(bas);
        }


        return true;
    }

    public void saveConfig(File file){
        //TODO: write out currentConfig xml file that could be read in below
    }

    private boolean loadFromXML(GuiVars gv, File file) throws DocumentException {
        boolean retval = false;

        ArrayList<Element> eList = new ArrayList<>();
        Dom4jUtils d4ju = new Dom4jUtils();

        d4ju.create(file);
        Element root = d4ju.selectNode("variables");
        Dom4jUtils.selectNodes(root, "var", eList);
        for(Element e : eList){
            String name = e.attributeValue("name");
            String sval = e.getText();
            if(name.equals("randomAgents")){gv.useRandom = Boolean.valueOf(sval);
            }else if(name.equals("attractionk")){gv.attractionPercent = Double.valueOf(sval);
            }else if(name.equals("repulsionk")){gv.repulsionPercent = Double.valueOf(sval);
            }else if(name.equals("drag")){gv.dragCoef = Double.valueOf(sval);
            }else if(name.equals("antibeliefk")){gv.antiBeliefScalar = Double.valueOf(sval);
            }else if(name.equals("origink")){gv.originScalar = Double.valueOf(sval);
            }else if(name.equals("clusterEPS")){gv.epsScalar = Float.valueOf(sval);
            }else if(name.equals("similarity")){gv.similarityPercent = Double.valueOf(sval); }
        }

        canvas.clearShapes();
        root = d4ju.selectNode("agents");
        if(root != null){
            List<Element> agentList = d4ju.selectNodes("agent");
            if(agentList != null){
                for(Element ae : agentList){
                    String name = ae.attributeValue("name");
                    BeliefAgentShape bas = initAgent(name, "grey", 16);
                    bas.setPos(rand.nextDouble()*200-100, rand.nextDouble()*200-100);
                    BaseBeliefCA cbca = bas.setAgent(BaseCA.BIAS.CONTROL);

                    List<Element> statementList = new ArrayList<>();
                    Dom4jUtils.selectNodes(ae, "statement", statementList);
                    for(Element se : statementList){
                        name = se.attributeValue("name");
                        double val = Double.parseDouble(se.attributeValue("weight"));
                        if(val > 0){
                            Statement freindStatement = new Statement(name, val, 1, 1);
                            cbca.addStatement(freindStatement, Belief.INITIAL_WEIGHT);
                            canvas.addShape(bas);
                        }
                    }
                }
            }
            retval = true;
        }
        gv.success = true;
        return retval;
    }

    private boolean loadFromXLSX(GuiVars gv, File file){
        boolean retval = false;
        Labeled2DMatrix sourceMatrix = new Labeled2DMatrix();
        sourceMatrix.fromExcel(file.getAbsolutePath(), 0);
        System.out.println(sourceMatrix.toString());
        for (int c = 0; c < sourceMatrix.getColumnDimension(); c++) {

            if (sourceMatrix.getColumnSum(c) > 4) {
                double brightness = 1.0; //sourceMatrix.getColumnSum(c)/sourceMatrix.getRowDimension();
                double hue = brightness * 360.0;
                Color color = Color.hsb(hue, 1.0, brightness);

                retval = true;
                String name = sourceMatrix.getColumnHeaders()[c];
                BeliefAgentShape bas = initAgent(name, color, 16);
                bas.setPos(rand.nextDouble()*200-100, rand.nextDouble()*200-100);
                BaseBeliefCA cbca = bas.setAgent(BaseCA.BIAS.CONTROL);
                double rowCount = 0;
                for (int r = 0; r < sourceMatrix.getRowDimension(); r++) {
                    name = sourceMatrix.getRowNames()[r];
                    double val = sourceMatrix.getEntry(r, c);
                    if (val > 0) {
                        rowCount = r;
                        Statement freindStatement = new Statement(name, val, 1, 1);
                        cbca.addStatement(freindStatement, Belief.INITIAL_WEIGHT);
                    }else{
                        Statement antiStatement = new Statement(name, 1, 1, 1);
                        cbca.addAntiStatement(antiStatement, Belief.INITIAL_WEIGHT);
                    }
                }
                hue = rowCount / sourceMatrix.getRowDimension() * 360.0;
                color = Color.hsb(hue, 1.0, brightness);
                bas.setFillColor(color);
                canvas.addShape(bas);
            }

        }
        return retval;
    }

    public GuiVars loadConfig(File file){
        GuiVars gv = new GuiVars();
        boolean goodload = false;

        ArrayList<Element> eList = new ArrayList<>();
        Dom4jUtils d4ju = new Dom4jUtils();
        if(file.isFile()) {
            try {
                if(file.getName().endsWith(".xml")){
                    goodload = loadFromXML(gv, file);
                } else if(file.getName().endsWith(".xlsx")){
                    goodload = loadFromXLSX(gv, file);
                }

                if(!goodload) {
                    initializeAgents(gv.useRandom);
                }

                setAttractionK(gv.attractionPercent/100);
                setRepulsionK(gv.repulsionPercent);
                setDragCoef(gv.dragCoef/100);
                setAntiBeliefScalar(gv.antiBeliefScalar/10.0);
                setOriginScalar(gv.originScalar/10.0);
                setEpsScalar(gv.epsScalar/100.0f);
                setSimilarityThresholds(gv.similarityPercent/100.0);
                gv.success = true;
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }
            return gv;
    }

    public void setDprinter(Dprintable dprinter) {
        this.dprinter = dprinter;
    }

    public void dprint(String str){
        if(dprinter == null){
            System.out.println(str);
        }else{
            dprinter.dprint(str);
        }
    }

}
