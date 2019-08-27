package com.philfeldman.Utils;

import com.philfeldman.GPCAs.FlockingBeliefCA;
import com.philfeldman.GPMain.FlockRecorder;
import com.philfeldman.GPMain.FlockingAgentManager;
import com.philfeldman.utils.jfxGUI.WeightWidget;
import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.ParticleBelief;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.dom4j.Element;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by SooperYooser on 1/8/2017.
 */
public class RunConfiguration extends XmlLoadable{
    public static enum SAVE_FORMAT {EXCEL, WEKA, EXCEL_AND_WEKA, NONE}
    public static double DELTA_TIME = 0.01;
    public static double STAGE_LIMIT = 2.0;
    public static double FORCE_ONSET_POINT = STAGE_LIMIT*0.75;
    public static String XLSX_POSTFIX = "xlsx";
    public static String ARFF_POSTFIX = "arff";
    public static String CSV_POSTFIX = "csv";
    public static String XML_POSTFIX = "xml";
    public String userName;
    public String sessionName;
        protected String outfolderName;
    public String embedFileName;
    public SAVE_FORMAT saveFormat;
    public int dimensions;
    public int sampleIncrement = 10;
    public double redGreenRatio;
    public double redExploitRadius;
    public double greenExploitRadius;
    public double attractionRadius;
    public double slewRateMean;
    public double slewRateVariance;
    public double velocityMean;
    public double velocityVariance;
    public double herdingWeight;
    public double clusterPercent;
    public double sourceScalar;
    public double alignmentScalar;
    public double stageLimit;
    public double forceOnsetPoint;
    public double elapsed;
    public double settlingTime;
    public ParticleBelief.BORDER_TYPE borderType;
    public FlockRecorder.DATA_TYPE measureDataType;
    public int numAgents;
    public int tailLength;
    public FlockingBeliefCA.PARTICLE_TYPE particleType;
    public int samplesToRecord;
    public int samplesRecorded;
    public int debugLevel;
    public boolean crossBiasInteraction;
    public boolean loadSuccess;
    public boolean continouousClusterCalc;
    public boolean recordingState;
    public boolean inverseHerding;
    public FlockingAgentManager.HERDING_TYPE herdingType;
    public Date date;
    SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yy-kk_mm_ss");


    public RunConfiguration(){
        super();
        date = new Date();
        outfolderName = System.getProperty("user.dir");
        setDefaults();
    }

    /**
     * Copy Constructor
     * @param other
     * TODO: Make the initial values static global defaults
     */
    public RunConfiguration(RunConfiguration other){
        this();
        userName                = other.userName ;
        sessionName             = other.sessionName ;
        outfolderName           = other.outfolderName ;
        embedFileName           = other.embedFileName ;
        saveFormat              = other.saveFormat ;
        dimensions              = other.dimensions ;
        sampleIncrement         = other.sampleIncrement ;
        redGreenRatio           = other.redGreenRatio;
        redExploitRadius        = other.redExploitRadius;
        greenExploitRadius      = other.greenExploitRadius;
        attractionRadius        = other.attractionRadius;
        slewRateMean            = other.slewRateMean;
        slewRateVariance        = other.slewRateVariance;
        velocityMean            = other.velocityMean;
        velocityVariance        = other.velocityVariance;
        clusterPercent          = other.clusterPercent ;
        sourceScalar            = other.sourceScalar ;
        alignmentScalar         = other.alignmentScalar;
        stageLimit              = other.stageLimit ;
        forceOnsetPoint         = other.forceOnsetPoint;
        settlingTime            = other.settlingTime ;
        borderType              = other.borderType ;
        measureDataType         = other.measureDataType ;
        numAgents               = other.numAgents ;
        tailLength               = other.tailLength ;
        samplesToRecord         = other.samplesToRecord ;
        samplesRecorded         = other.samplesRecorded ;
        crossBiasInteraction    = other.crossBiasInteraction ;
        continouousClusterCalc  = other.continouousClusterCalc ;
        recordingState          = other.recordingState ;
        date                    = other.date ;
        particleType            = other.particleType;
        inverseHerding          = other.inverseHerding;
        herdingWeight           = other.herdingWeight;
        herdingType             = other.herdingType;
        debugLevel              = other.debugLevel;
    }

    /**
     * Set the default values
     */
    public void setDefaults(){
        outfolderName = System.getProperty("user.dir");
        sessionName = "flocking_"+ UUID.randomUUID().toString();
        userName = System.getProperty("user.name");
        embedFileName = null;
        settlingTime = 10;
        samplesToRecord = 400;
        loadSuccess = false;
        continouousClusterCalc = false;
        redGreenRatio = 0.1;
        greenExploitRadius = 0.2;
        redExploitRadius = 0.2;
        attractionRadius = 1.0;
        slewRateMean = 100.0;
        slewRateVariance = 50;
        velocityMean = 2.0;
        velocityVariance = 1.0;
        clusterPercent = 0.1;
        sourceScalar = 0.1;
        alignmentScalar = 0.0;
        stageLimit = STAGE_LIMIT;
        forceOnsetPoint = RunConfiguration.FORCE_ONSET_POINT;
        borderType = ParticleBelief.BORDER_TYPE.REFLECTIVE;
        numAgents = 100;
        tailLength = 1;
        dimensions = 2;
        sampleIncrement = 10;
        crossBiasInteraction = true;
        measureDataType = FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE;
        saveFormat = SAVE_FORMAT.EXCEL;
        elapsed = 0;
        recordingState = false;
        samplesRecorded = 0;
        debugLevel = 0;
        particleType = FlockingBeliefCA.PARTICLE_TYPE.Boidish;
        inverseHerding = false;
        herdingWeight = WeightWidget.MAX_WEIGHT;
        herdingType = FlockingAgentManager.HERDING_TYPE.NO_HERDING;
    }

    @Override
    protected void handleSpecial(Field f, String varName, String valStr) {
        valStr = valStr.toLowerCase();
        System.out.println(varName+" = "+valStr);

        if(varName.equals("borderType")){
            borderType = ParticleBelief.BORDER_TYPE.REFLECTIVE;
            for(ParticleBelief.BORDER_TYPE b : ParticleBelief.BORDER_TYPE.values()){
                if(b.toString().toLowerCase().contains(valStr)){
                    borderType = b;
                    break;
                }
            }
        }

        if(varName.equals("herdingType")){
            herdingType = FlockingAgentManager.HERDING_TYPE.NO_HERDING;
            for(FlockingAgentManager.HERDING_TYPE h : FlockingAgentManager.HERDING_TYPE.values()){
                if(h.toString().toLowerCase().contains(valStr)){
                    herdingType = h;
                    break;
                }
            }
        }

        if(varName.equals("saveFormat")) {
            saveFormat = SAVE_FORMAT.EXCEL_AND_WEKA;
            for(SAVE_FORMAT s : SAVE_FORMAT.values()){
                if(s.toString().toLowerCase().contains(valStr)){
                    saveFormat = s;
                    break;
                }
            }
        }
        if(varName.equals("measureDataType")) {
            measureDataType = FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE;
            for(FlockRecorder.DATA_TYPE d : FlockRecorder.DATA_TYPE.values()){
                if(d.toString().toLowerCase().contains(valStr)){
                    measureDataType = d;
                    break;
                }
            }
        }
    }

    @Override
    public boolean loadFromXML(Element root) {
        loadSuccess = super.loadFromXML(root);
        return loadSuccess;
    }

    /**
     * return the file name to save as. Will create the directory if it doesn't exist
     * @param postfix
     * @return
     */
    public String getOutFileName(String postfix){
        Path dir = Paths.get(outfolderName);
        if(!Files.exists(dir)){
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // first, verify that the folder exists. If not, create it
        return outfolderName+"/"+sessionName+"_"+sdf.format(date)+"."+postfix;
    }

    public String getOutfolderName() {
        File f = new File(outfolderName);
        if(!f.isDirectory()){
            outfolderName = System.getProperty("user.dir");
        }
        return outfolderName;
    }

    public void setOutfolderName(String outfolderName) {
        this.outfolderName = outfolderName;
    }

    /**
     * return the file name to save as. Will create the directory if it doesn't exist
     * @param postfix
     * @return
     */
    public String getOutFileName(String typeString, String postfix){
        Path dir = Paths.get(outfolderName);
        if(!Files.exists(dir)){
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // first, verify that the folder exists. If not, create it
        return outfolderName+"/"+sessionName+"_"+typeString+"_"+sdf.format(date)+"."+postfix;
    }

    /**
     * Save to an Excel worksheet
     * @param sheet the sheet (tab) to save to
     * @param rowIndex the starting row index
     * @return the next free row index
     */
    public int toExcel(Sheet sheet, int rowIndex){
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("User");
        row.createCell(1).setCellValue(userName);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Session");
        row.createCell(1).setCellValue(sessionName);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Date");
        row.createCell(1).setCellValue(date);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Sample Increment");
        row.createCell(1).setCellValue(sampleIncrement);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Dimensions");
        row.createCell(1).setCellValue(dimensions);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Red-Green ratio");
        row.createCell(1).setCellValue(redGreenRatio);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Allow bias (Red-Green) interaction");
        row.createCell(1).setCellValue(crossBiasInteraction);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Red initial influence radius");
        row.createCell(1).setCellValue(redExploitRadius);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Green initial influence radius");
        row.createCell(1).setCellValue(greenExploitRadius);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Attraction initial radius");
        row.createCell(1).setCellValue(attractionRadius);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Slew rate mean");
        row.createCell(1).setCellValue(slewRateMean);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Slew rate variance");
        row.createCell(1).setCellValue(slewRateVariance);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Velocity mean");
        row.createCell(1).setCellValue(velocityMean);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Velocity variance");
        row.createCell(1).setCellValue(velocityVariance);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Enable Herding");
        row.createCell(1).setCellValue(herdingType.toString());
        row = sheet.createRow(rowIndex++);
        if(herdingType != FlockingAgentManager.HERDING_TYPE.NO_HERDING) {
            row.createCell(0).setCellValue("Inverse Herding");
            row.createCell(1).setCellValue(Boolean.toString(inverseHerding));
            row = sheet.createRow(rowIndex++);

            row.createCell(0).setCellValue("Herding Weight");
            row.createCell(1).setCellValue(herdingWeight);
            row = sheet.createRow(rowIndex++);
        }
        row.createCell(0).setCellValue("Stage Limit");
        row.createCell(1).setCellValue(stageLimit);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Force Onset Point");
        row.createCell(1).setCellValue(forceOnsetPoint);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Border type");
        row.createCell(1).setCellValue(borderType.toString());
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Num Agents");
        row.createCell(1).setCellValue(numAgents);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Particle Type");
        row.createCell(1).setCellValue(particleType.toString());
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("EPS cluster initial percent");
        row.createCell(1).setCellValue(clusterPercent);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Settling time");
        row.createCell(1).setCellValue(settlingTime);
        row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue("Samples to record");
        row.createCell(1).setCellValue(samplesToRecord);

        return rowIndex;
    }

    @Override
    public String toString() {
        String s = "userName = "+ userName+"\n";
        s +=  "sessionName = "+sessionName+"\n";
        s +=  "outfolderName = "+outfolderName+"\n";
        s +=  "dimensions = "+dimensions+"\n";
        s +=  "sampleIncrement = "+sampleIncrement+"\n";
        s +=  "redGreenRatio = "+String.format("%.4f", redGreenRatio)+"\n";
        s +=  "redExploitRadius = "+String.format("%.4f", redExploitRadius)+"\n";
        s +=  "greenExploitRadius = "+String.format("%.4f", greenExploitRadius)+"\n";
        s +=  "attractionRadius = "+String.format("%.4f", attractionRadius)+"\n";
        s +=  "slewRateMean = "+String.format("%.4f", slewRateMean)+"\n";
        s +=  "slewRateVariance = "+String.format("%.4f", slewRateVariance)+"\n";
        s +=  "velocityMean = "+String.format("%.4f", velocityMean)+"\n";
        s +=  "velocityVariance = "+String.format("%.4f", velocityVariance)+"\n";
        s +=  "herdingType = "+herdingType.toString()+"\n";
        if(herdingType != FlockingAgentManager.HERDING_TYPE.NO_HERDING){
            s +=  "inverseHerding = "+String.format("%b", inverseHerding)+"\n";
            s +=  "herdingWeight = "+String.format("%.4f", herdingWeight)+"\n";
        }
        s +=  "clusterPercent = "+String.format("%.4f", clusterPercent)+"\n";
        s +=  "stageLimit = "+String.format("%.4f", stageLimit)+"\n";
        s +=  "borderType = "+borderType.toString()+"\n";
        s +=  "numAgents = "+numAgents+"\n";
        s +=  "particleType = "+particleType.toString()+"\n";
        s +=  "crossBiasInteraction = "+ crossBiasInteraction +"\n";
        s +=  "loadSuccess = "+loadSuccess+"\n";
        s +=  "settlingTime = "+settlingTime+"\n";
        s +=  "samplesToRecord = "+samplesToRecord+"\n";
        s +=  "date = "+date;
        return s;
    }

    public static void main(String[] args){
        RunConfiguration rc = new RunConfiguration();

        System.out.println(rc.toString());
        rc.outfolderName = "C:\\Development\\Sandboxes\\GroupPolarizationModel\\data\\foo";
        System.out.println(rc.getOutFileName(RunConfiguration.ARFF_POSTFIX));
    }
}
