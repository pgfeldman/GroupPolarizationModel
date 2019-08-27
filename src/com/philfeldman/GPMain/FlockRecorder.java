package com.philfeldman.GPMain;

import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.CAInfluences;
import com.philfeldman.GPCAs.FlockingBeliefCA;
import com.philfeldman.Graphics.FlockingShape;
import com.philfeldman.MediatedInfo.StorageAndRetreival;
import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.ParticleStatement;
import com.philfeldman.Philosophy.Statement;
import com.philfeldman.Utils.RunConfiguration;
import com.philfeldman.Utils.SettingRecording;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.utils.math.Labeled2DMatrix;
import net.sf.javaml.distance.fastdtw.dtw.FastDTW;
import net.sf.javaml.distance.fastdtw.timeseries.TimeSeries;
import net.sf.javaml.distance.fastdtw.timeseries.TimeSeriesPoint;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.philfeldman.utils.jfxGUI.Popups.warnAlert;

/**
 * Created by SooperYooser on 1/3/2017.
 */
public class FlockRecorder {

    public static enum DATA_TYPE {ORIGIN_DISTANCE, MEAN_CENTER_DISTANCE, ANGLE_FROM_REFERENCE, ANGLE_FROM_MEAN, CLUSTER_ID, DYNAMIC_TIME_WARPING,
        ORIGIN_DISTANCE_STATS, MEAN_CENTER_DISTANCE_STATS, ANGLE_FROM_REFERENCE_STATS, ANGLE_FROM_FLOCK_MEAN_STATS, CLUSTER_ID_STATS,
        ORIGIN_POSITION_DELTA, ANGLE_DELTA, MEAN_CENTER_DELTA, MEAN_ANGLE_DELTA, ORIGIN_VELOCITY_DELTA, ANGLE_VELOCITY_DELTA,
        MEAN_CENTER_VELOCITY_DELTA, MEAN_ANGLE_VELOCITY_DELTA,
        ALL, ALL_DELTAS, ALL_MEASURES, ALL_MEASURES_STATS, TRAJECTORIES}
    public static enum STATS_TYPES {Mean, Fifth, Fiftieth, NintyFifth, Variance}

    //---------------------------------- Inner Classes
    class DimensionSample {
        double fifthPercentile = -1;
        double mean = -1;
        double nintyfifthPercentile = -1;
        double variance = -1;

        public DimensionSample(double[] dimensionData){
            if(dimensionData.length > 3) {
                Percentile p = new Percentile();
                p.setData(dimensionData);
                fifthPercentile = p.evaluate(5);
                mean = p.evaluate(50);
                nintyfifthPercentile = p.evaluate(95);
                variance = nintyfifthPercentile - fifthPercentile;
            }
        }
    }
    class DimensionSequence {
        BaseCA.BIAS bias;
        String dimensionName;
        ArrayList<DimensionSample> sequence;

        public DimensionSequence(BaseCA.BIAS bias, String dimensionName){
            this.bias = bias;
            this.dimensionName = dimensionName;
            sequence = new ArrayList<>();
        }

        public void addSample(){
            ArrayList<Double> values = new ArrayList<>();
            for(FlockingBeliefCA fbca : beliefList) {
                if (fbca.getBias() == bias) {
                    List<Statement> sList = fbca.getCurBelief().getSortedList(Belief.ORDERING.NAME);
                    for (Statement s : sList) {
                        ParticleStatement ps = (ParticleStatement)s;
                        if (s.getName().equals(dimensionName)) {
                            values.add(ps.getValue());
                            break;
                        }
                    }
                }
            }
            double[] varray = new double[values.size()];
            for(int i = 0; i < values.size(); ++i){
                varray[i] = values.get(i);
            }
            DimensionSample ds = new DimensionSample(varray);
            sequence.add(ds);
        }

        public int toExcel(int curRow, Sheet sheet){
            int rowNum = curRow;

            Row fifthRow = sheet.createRow(rowNum++);
            fifthRow.createCell(0).setCellValue(bias+":"+dimensionName+"_5%");
            Row meanRow = sheet.createRow(rowNum++);
            meanRow.createCell(0).setCellValue(bias+":"+dimensionName+"_50%");
            Row nintyFifthRow = sheet.createRow(rowNum++);
            nintyFifthRow.createCell(0).setCellValue(bias+":"+dimensionName+"_95%");

            for(int i = 0; i < sequence.size(); ++i) {
                DimensionSample sample = sequence.get(i);

                int cellNum = i + 1;
                fifthRow.createCell(cellNum).setCellValue(sample.fifthPercentile);
                meanRow.createCell(cellNum).setCellValue(sample.mean);
                nintyFifthRow.createCell(cellNum).setCellValue(sample.nintyfifthPercentile);
            }
            return rowNum;
        }

        public int normalizedToExcel(int curRow, Sheet sheet){
            int rowNum = curRow;

            Row fifthRow = sheet.createRow(rowNum++);
            fifthRow.createCell(0).setCellValue(bias+":"+dimensionName+"_5%");
            Row nintyFifthRow = sheet.createRow(rowNum++);
            nintyFifthRow.createCell(0).setCellValue(bias+":"+dimensionName+"_95%");

            for(int i = 0; i < sequence.size(); ++i) {
                DimensionSample sample = sequence.get(i);

                int cellNum = i + 1;
                fifthRow.createCell(cellNum).setCellValue(sample.fifthPercentile - sample.mean);
                nintyFifthRow.createCell(cellNum).setCellValue(sample.nintyfifthPercentile - sample.mean);
            }
            return rowNum;
        }

    }

    class AgentRecording{
        double timestamp = 0;
        double distanceFromOrigin = 0;
        double distanceFromAverageCenter = 0;
        double angleFromReference = 0;
        double angleFromFlockMean = 0;
        ArrayRealVector posVector;
        int clusterID = 0;
        String cellName = "unset";
        ArrayList<CAInfluences> influencesList;

        public AgentRecording(){

        }

        public AgentRecording(FlockingBeliefCA agent, double elapsed){
            setVals(agent, elapsed);
        }

        public void setVals(FlockingBeliefCA agent, double elapsed){
            ParticleBelief pb = agent.getCurBelief();
            cellName = agent.getCellName();
            ArrayRealVector pos = pb.calcPosVector();
            RealVector angle = pos.unitVector();
            timestamp = elapsed;
            clusterID = agent.getClusterId();
            distanceFromOrigin = pos.getNorm();
            posVector = pb.calcPosVector();
        }

        public void setInfluences(List<CAInfluences> source){
            influencesList = new ArrayList<>();
            for(CAInfluences sourceInf : source){
                CAInfluences sample = new CAInfluences(sourceInf);
                influencesList.add(sample);
            }
        }

        public boolean hasInfluences(){
            return (influencesList.size() > 0);
        }

        public String influenceToXML(){
            StringBuilder sb = new StringBuilder();
            for(CAInfluences ci : influencesList){
                if(ci.hasInfluences()) {
                    sb.append(ci.toXMLString());
                }
            }
            return sb.toString();
        }
    }

    class AgentSequence {
        FlockingShape fShape;
        FlockingBeliefCA agent;
        ArrayList<AgentRecording> sequence;

        public AgentSequence(FlockingShape fs){
            fShape = fs;
            agent = fs.getAgent();
            sequence = new ArrayList<>();
        }

        public FlockingShape getfShape() {
            return fShape;
        }

        public FlockingBeliefCA getAgent() {
            return agent;
        }

        public double getAngleBetween(RealVector angle, RealVector referenceAngle){
            double dotProduct = referenceAngle.dotProduct(angle);
            double cosTheta = Math.min(1.0, dotProduct);
            cosTheta = Math.max(-1.0, cosTheta);
            double radians = Math.acos(cosTheta);
            if(Double.isNaN(radians)){
                System.out.println(getfShape().getName()+" bad angle");
            }
            return Math.toDegrees(radians);
        }

        public DimensionSample comparePositions(AgentSequence other, DATA_TYPE type){
            double[] vals = new double[sequence.size()];

            TimeSeries timeSeries = new TimeSeries(sequence.get(0).posVector.getDimension());
            TimeSeries otherTimeSeries = new TimeSeries(other.sequence.get(0).posVector.getDimension());
            TimeSeriesPoint timeSeriesPoint;
            TimeSeriesPoint otherTimeSeriesPoint;

            for(int i = 0; i < sequence.size(); ++i){
                AgentRecording ar = sequence.get(i);
                AgentRecording otar = other.sequence.get(i);

                switch (type){
                    case ANGLE_FROM_MEAN:
                        vals[i] = Math.abs(ar.angleFromFlockMean -otar.angleFromFlockMean);
                        break;
                    case ANGLE_FROM_REFERENCE:
                        vals[i] = Math.abs(ar.angleFromReference-otar.angleFromReference);
                        break;
                    case MEAN_CENTER_DISTANCE:
                        vals[i] = Math.abs(ar.distanceFromAverageCenter-otar.distanceFromAverageCenter);
                        break;
                    case ORIGIN_DISTANCE:
                        vals[i] = Math.abs(ar.distanceFromOrigin-otar.distanceFromOrigin);
                        break;
                    case DYNAMIC_TIME_WARPING:
                        timeSeriesPoint = new TimeSeriesPoint(ar.posVector.toArray());
                        otherTimeSeriesPoint = new TimeSeriesPoint(otar.posVector.toArray());
                        timeSeries.addLast(ar.timestamp, timeSeriesPoint);
                        otherTimeSeries.addLast(otar.timestamp, otherTimeSeriesPoint);
                        break;
                    default: vals[i] = 0.0;
                }
            }

            DimensionSample ds = new DimensionSample(vals);
            if(type == DATA_TYPE.DYNAMIC_TIME_WARPING){
                // for the time being, just overwrite the mean
                ds.mean = FastDTW.getWarpDistBetween(timeSeries, otherTimeSeries);
            }
            return ds;
        }

        public DimensionSample compareVelocities(AgentSequence other, DATA_TYPE type){
            double[] vals = new double[sequence.size()-1];

            for(int i = 0; i < sequence.size()-1; ++i){
                AgentRecording ar1 = sequence.get(i);
                AgentRecording ar2 = sequence.get(i+1);
                AgentRecording otar1 = other.sequence.get(i);
                AgentRecording otar2 = other.sequence.get(i+1);

                double velA;
                double velO;
                switch (type){
                    case ANGLE_FROM_MEAN:
                        velA = ar2.angleFromFlockMean - ar1.angleFromFlockMean;
                        velO = otar2.angleFromFlockMean - otar1.angleFromFlockMean;
                        vals[i] = velA - velO;
                        break;
                    case ANGLE_FROM_REFERENCE:
                        velA = ar2.angleFromReference - ar1.angleFromReference;
                        velO = otar2.angleFromReference - otar1.angleFromReference;
                        vals[i] = velA - velO;
                        break;
                    case MEAN_CENTER_DISTANCE:
                        velA = ar2.distanceFromAverageCenter - ar1.distanceFromAverageCenter;
                        velO = otar2.distanceFromAverageCenter - otar1.distanceFromAverageCenter;
                        vals[i] = velA - velO;
                        break;
                    case ORIGIN_DISTANCE:
                        velA = ar2.distanceFromOrigin - ar1.distanceFromOrigin;
                        velO = otar2.distanceFromOrigin - otar1.distanceFromOrigin;
                        vals[i] = velA - velO;
                        break;
                    default: vals[i] = 0.0;
                }
            }

            DimensionSample ds = new DimensionSample(vals);
            return ds;
        }

        public DimensionSample getStats(DATA_TYPE type){
            double[] vals = new double[sequence.size()];
            for(int i = 0; i < sequence.size(); ++i){
                AgentRecording ar = sequence.get(i);
                vals[i] = 0;
                switch (type){
                    case ANGLE_FROM_FLOCK_MEAN_STATS:
                        vals[i] = ar.angleFromFlockMean;
                        break;
                    case ANGLE_FROM_REFERENCE_STATS:
                        vals[i] = ar.angleFromReference;
                        break;
                    case MEAN_CENTER_DISTANCE_STATS:
                        vals[i] = ar.distanceFromAverageCenter;
                        break;
                    case CLUSTER_ID_STATS:
                        vals[i] = ar.clusterID;
                        break;
                    case ORIGIN_DISTANCE_STATS:
                        vals[i] = ar.distanceFromOrigin;
                        break;
                }
            }
            return new DimensionSample(vals);
        }

        public void addSample(double elapsed, ArrayRealVector averageCenter, ArrayRealVector flockHeading){
            ArrayRealVector referenceAngle = new ArrayRealVector(flockHeading.getDimension());
            referenceAngle.set(0);
            referenceAngle.setEntry(0, 1.0);

            AgentRecording sample = new AgentRecording(agent, elapsed);
            ParticleBelief pb = agent.getCurBelief();
            ArrayRealVector pos = pb.calcPosVector();
            RealVector angle = pos.unitVector();
            RealVector heading = pb.calcOrientVector();
            sample.distanceFromAverageCenter = averageCenter.getDistance(pos);
            sample.angleFromReference = getAngleBetween(angle, referenceAngle);
            sample.angleFromFlockMean = getAngleBetween(heading, flockHeading);

            // add the influences as a seperate set of samples. These are too complicated to
            // be output to a spreadsheet
            sample.setInfluences(agent.getInfluencesList());

            sequence.add(sample);
        }

        public int samplesToExcel(int curRow, Sheet sheet, DATA_TYPE type){
            int rowNum = curRow;
            Row exRow = sheet.createRow(rowNum++);
            exRow.createCell(0).setCellValue(agent.getName());
            for(int i = 0; i < sequence.size(); ++i){
                AgentRecording sample = sequence.get(i);
                int cellNum = i+1;
                Cell cell = exRow.createCell(cellNum);
                switch(type){
                    case ORIGIN_DISTANCE: cell.setCellValue(sample.distanceFromOrigin);
                        break;
                    case MEAN_CENTER_DISTANCE: cell.setCellValue(sample.distanceFromAverageCenter);
                        break;
                    case ANGLE_FROM_REFERENCE: cell.setCellValue(sample.angleFromReference);
                        break;
                    case ANGLE_FROM_MEAN: cell.setCellValue(sample.angleFromFlockMean);
                        break;
                    case CLUSTER_ID: cell.setCellValue(sample.clusterID);
                        break;
                    case TRAJECTORIES: cell.setCellValue(sample.cellName);
                        break;
                }
            }
            return rowNum;
        }

        public int statsToExcel(int curRow, Sheet sheet, DATA_TYPE type){
            int rowNum = curRow;
            Row exRow = sheet.createRow(rowNum++);
            exRow.createCell(0).setCellValue(agent.getName());

            DimensionSample ds = getStats(type);

            int cellNum = 1;
            exRow.createCell(cellNum++).setCellValue(ds.fifthPercentile);
            exRow.createCell(cellNum++).setCellValue(ds.mean);
            exRow.createCell(cellNum++).setCellValue(ds.nintyfifthPercentile);
            exRow.createCell(cellNum++).setCellValue(ds.variance);

            return rowNum;
        }

        public String influenceToXML(BufferedWriter bw) throws IOException {
            StringBuilder sb = new StringBuilder();
            String str = "\t<agentRelationships agent = \""+agent.getName()+"\">\n";
            sb.append(str);
            for(AgentRecording ar : sequence){
                str = "\t\t<sampleData timestamp = \""+ar.timestamp+"\">\n";
                sb.append(str);
                str = "\t\t\t<cell name = \""+ar.cellName+"\"/>\n";
                sb.append(str);
                if(ar.hasInfluences()) {
                    str = ar.influenceToXML();
                    sb.append(str);
                }
                str = "\t\t</sampleData>\n";
                sb.append(str);
            }
            str = "\t</agentRelationships>\n";
            sb.append(str);
            bw.write(sb.toString());
            return sb.toString();
        }
    }



    //---------------------------- Global Variables
    protected TreeMap<String, AgentSequence> agentSequenceMap;
    protected TreeMap<String, DimensionSequence> dimensionSequenceMap;
    protected ArrayList<SettingRecording> settingsList;
    protected ArrayList<FlockingBeliefCA> beliefList;
    protected ArrayList<Double> elapsedList;
    protected StorageAndRetreival stoRet;
    protected RunConfiguration config;

    //---------------------------- Methods

    public FlockRecorder(RunConfiguration config){
        this.config = config;
        agentSequenceMap = new TreeMap<>();
        settingsList = new ArrayList<>();
        beliefList = new ArrayList<>();
        dimensionSequenceMap = new TreeMap<>();
        elapsedList = new ArrayList<>();
    }

    public void setStorageAndRetreival(StorageAndRetreival stoRet) {
        this.stoRet = stoRet;
    }

    public void add(FlockingShape fs){
        AgentSequence agentSequence = null;
        if(!agentSequenceMap.containsKey(fs.getName())){
            agentSequence = new AgentSequence(fs);
            agentSequenceMap.put(fs.getName(), agentSequence);
            beliefList.add(fs.getAgent());
        }

        FlockingBeliefCA fbca = fs.getAgent();

        List<Statement> sList = fbca.getCurBelief().getSortedList(Belief.ORDERING.NAME);
        for(Statement s : sList){
            String dimensionName = fbca.getBias().toString()+s.getName();
            if(!dimensionSequenceMap.containsKey(dimensionName)){
                DimensionSequence ds = new DimensionSequence(fbca.getBias(), s.getName());
                dimensionSequenceMap.put(dimensionName, ds);
            }
        }
    }

    public void recordSample(SettingRecording settings, Map<String, ArrayRealVector> flockHeadingMap){

        elapsedList.add(settings.timestamp);
        settingsList.add(settings);
        if(agentSequenceMap.size() == 0){
            System.out.println("FlockRecorder.recordSample: agentSequenceMap is empty");
            return; // nothing to do here, don't record
        }

        for(String key: dimensionSequenceMap.keySet()) {
            DimensionSequence ds = dimensionSequenceMap.get(key);
            ds.addSample();
        }

        // handle distance recordings by agent
        ArrayRealVector globalCenter = null;
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            FlockingBeliefCA fba = as.agent;
            ParticleBelief pb = fba.getCurBelief();
            ArrayRealVector arv = pb.calcPosVector();
            if(globalCenter == null){
                globalCenter = new ArrayRealVector(arv);
            }else{
                globalCenter = globalCenter.add(arv);
            }
        }
        double scalar = 1.0/ agentSequenceMap.size();

        // handle angle recordings by agent
        globalCenter.mapMultiplyToSelf(scalar);


        for(String name : agentSequenceMap.keySet()) {
            AgentSequence as = agentSequenceMap.get(name);
            ArrayRealVector angle = flockHeadingMap.get(as.getfShape().getFlockName());
            as.addSample(settings.timestamp, globalCenter, angle);
        }
    }

    public void constructMapFromTrajectories(){

    }

    public static void saveWorkbook(String filename, Workbook wb){
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            warnAlert(e.getMessage());
            e.printStackTrace();
        } catch (IOException e){
            warnAlert(e.getMessage());
            e.printStackTrace();
        }
    }

    protected int timeToExcel(int curRow, Sheet sheet){
        int rowNum = curRow;
        Row exRow = sheet.createRow(rowNum++);
        exRow.createCell(0).setCellValue("Time");
        for(int i = 0; i < elapsedList.size(); ++i){
            int cellNum = i+1;
            Cell cell = exRow.createCell(cellNum);
            cell.setCellValue(elapsedList.get(i));
        }
        return rowNum;
    }

    protected int sequenceHeaderToExcel(int curRow, Sheet sheet){
        int rowNum = curRow;
        Row exRow = sheet.createRow(rowNum++);
        //exRow.createCell(0).setCellValue("Sequence");
        for(int i = 0; i < elapsedList.size(); ++i){
            int cellNum = i+1;
            Cell cell = exRow.createCell(cellNum);
            cell.setCellValue(String.format("sequence_%05d", i));
        }
        return rowNum;
    }

    public int statsHeadersToExcel(int curRow, Sheet sheet){
        int rowNum = curRow;
        Row exRow = sheet.createRow(rowNum++);
        int cellNum = 1;
        exRow.createCell(cellNum++).setCellValue(STATS_TYPES.Fifth.toString());
        exRow.createCell(cellNum++).setCellValue(STATS_TYPES.Mean.toString());
        exRow.createCell(cellNum++).setCellValue(STATS_TYPES.NintyFifth.toString());
        exRow.createCell(cellNum++).setCellValue(STATS_TYPES.Variance.toString());
        return rowNum;
    }

    protected void settingsToExcelSheet(Workbook wb, String sheetName){
        int rowIndex = 0;
        int cellIndex = 0;
        Cell cell;
        // settings sheet
        Sheet sheet = wb.createSheet(sheetName);
        rowIndex = config.toExcel(sheet, rowIndex);

        rowIndex += 2;
        cellIndex = 1;
        Row timesampRow = sheet.createRow(rowIndex++);
        Row greenExploitRadiusRow = sheet.createRow(rowIndex++);
        Row redExploitRadiusRow = sheet.createRow(rowIndex++);
        Row clusterEpsPercentRow = sheet.createRow(rowIndex++);
        Row stageLimitRow = sheet.createRow(rowIndex++);
        timesampRow.createCell(0).setCellValue("timestamp");
        greenExploitRadiusRow.createCell(0).setCellValue("greenExploitRadius");
        redExploitRadiusRow.createCell(0).setCellValue("redExploitRadius");
        clusterEpsPercentRow.createCell(0).setCellValue("clusterEpsPercent");
        stageLimitRow.createCell(0).setCellValue("stageLimit");
        for(SettingRecording sr : settingsList){
            timesampRow.createCell(cellIndex).setCellValue(sr.timestamp);
            greenExploitRadiusRow.createCell(cellIndex).setCellValue(sr.greenExploitRadius);
            redExploitRadiusRow.createCell(cellIndex).setCellValue(sr.redExploitRadius);
            clusterEpsPercentRow.createCell(cellIndex).setCellValue(sr.clusterEpsPercent);
            stageLimitRow.createCell(cellIndex).setCellValue(sr.stageLimit);
            cellIndex++;
        }
    }

    protected void distanceFromOriginToExcelSheet(Workbook wb, String sheetName){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet(sheetName);

        rowIndex = timeToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.samplesToExcel(rowIndex, sheet, DATA_TYPE.ORIGIN_DISTANCE);
        }

        sheet = wb.createSheet(sheetName+"_stats");
        rowIndex = 0;
        rowIndex = statsHeadersToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.statsToExcel(rowIndex, sheet, DATA_TYPE.ORIGIN_DISTANCE_STATS);
        }
    }

    protected void distanceFromAverageCenterToExcelSheet(Workbook wb, String sheetName){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet(sheetName);

        rowIndex = timeToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);

            rowIndex = as.samplesToExcel(rowIndex, sheet, DATA_TYPE.MEAN_CENTER_DISTANCE);
        }

        sheet = wb.createSheet(sheetName+"_stats");
        rowIndex = 0;
        rowIndex = statsHeadersToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.statsToExcel(rowIndex, sheet, DATA_TYPE.MEAN_CENTER_DISTANCE_STATS);
        }
    }

    protected void angleFromReferenceToExcelSheet(Workbook wb, String sheetName){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet(sheetName);

        rowIndex = timeToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.samplesToExcel(rowIndex, sheet, DATA_TYPE.ANGLE_FROM_REFERENCE);
        }

        sheet = wb.createSheet(sheetName+"_stats");
        rowIndex = 0;
        rowIndex = statsHeadersToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.statsToExcel(rowIndex, sheet, DATA_TYPE.ANGLE_FROM_REFERENCE_STATS);
        }
    }

    protected void angleFromMeanToExcelSheet(Workbook wb, String sheetName){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet(sheetName);

        rowIndex = timeToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.samplesToExcel(rowIndex, sheet, DATA_TYPE.ANGLE_FROM_MEAN);
        }

        sheet = wb.createSheet(sheetName+"_stats");
        rowIndex = 0;
        rowIndex = statsHeadersToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.statsToExcel(rowIndex, sheet, DATA_TYPE.ANGLE_FROM_FLOCK_MEAN_STATS);
        }
    }

    protected void clusterIdToExcelSheet(Workbook wb, String sheetName){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet(sheetName);

        rowIndex = timeToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.samplesToExcel(rowIndex, sheet, DATA_TYPE.CLUSTER_ID);
        }

        sheet = wb.createSheet(sheetName+"_stats");
        rowIndex = 0;
        rowIndex = statsHeadersToExcel(rowIndex, sheet);
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.statsToExcel(rowIndex, sheet, DATA_TYPE.CLUSTER_ID_STATS);
        }
    }

    protected void trajectoriesToExcelSheet(Workbook wb, String sheetName){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet(sheetName);
        rowIndex = sequenceHeaderToExcel(rowIndex, sheet);
        rowIndex = timeToExcel(rowIndex, sheet);
        ArrayList<String> sequenceNames = new ArrayList<>(agentSequenceMap.keySet());
        for(String name : sequenceNames){
            AgentSequence as = agentSequenceMap.get(name);
            rowIndex = as.samplesToExcel(rowIndex, sheet, DATA_TYPE.TRAJECTORIES);
        }
    }

    protected void dimensionsToExcel(Workbook wb){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet("Dimensions");
        rowIndex = timeToExcel(rowIndex, sheet);
        for(String name : dimensionSequenceMap.keySet()){
            DimensionSequence ds = dimensionSequenceMap.get(name);
            rowIndex = ds.toExcel(rowIndex, sheet);
        }

        rowIndex = 0;
        sheet = wb.createSheet("Normalized Dimensions");
        rowIndex = timeToExcel(rowIndex, sheet);
        for(String name : dimensionSequenceMap.keySet()){
            DimensionSequence ds = dimensionSequenceMap.get(name);
            rowIndex = ds.normalizedToExcel(rowIndex, sheet);
        }
    }

    protected void storageAndRetreivalToExcel(Workbook wb){
        int rowIndex = 0;
        Sheet sheet = wb.createSheet("StorageAndRetreival");
        stoRet.xyToExcel(sheet, 0);
    }

    protected Labeled2DMatrix buildDeltaMatrix(DATA_TYPE type, boolean position){
        String[] names = new String[agentSequenceMap.size()];
        int i = 0;
        for(String s : agentSequenceMap.keySet()){
            names[i++] = s;
        }
        Labeled2DMatrix lm = new Labeled2DMatrix(names, names);
        DimensionSample ds;
        for(String rowName : names){
            AgentSequence rowSequence = agentSequenceMap.get(rowName);
            for(String columnName : names){
                AgentSequence colSequence = agentSequenceMap.get(columnName);
                if(position) {
                    ds = rowSequence.comparePositions(colSequence, type);
                }else{
                    ds = rowSequence.compareVelocities(colSequence, type);
                }
                lm.setEntry(rowName, columnName, ds.mean);
            }
        }
        return lm;
    }

    public void influenceToXML(String filename) throws IOException {
        Path p = Paths.get(filename);
        BufferedWriter bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8);
        bw.write("<xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\">\n");
        bw.write("<influenceDocument>\n");
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            as.influenceToXML(bw);
        }
        bw.write("</influenceDocument>\n</xml>");
        bw.flush();
        bw.close();
    }

    public void toExcel(String fileName){
        Workbook wb = new XSSFWorkbook();

        // settings
        settingsToExcelSheet( wb, "Settings");
        // position deltas
        Labeled2DMatrix lm;
        lm = buildDeltaMatrix(DATA_TYPE.DYNAMIC_TIME_WARPING, true);
        lm.toExcel(wb, DATA_TYPE.DYNAMIC_TIME_WARPING.toString());
        lm = buildDeltaMatrix(DATA_TYPE.ORIGIN_DISTANCE, true);
        lm.toExcel(wb, DATA_TYPE.ORIGIN_POSITION_DELTA.toString());
        lm = buildDeltaMatrix(DATA_TYPE.ANGLE_FROM_REFERENCE, true);
        lm.toExcel(wb, DATA_TYPE.ANGLE_DELTA.toString());
        lm = buildDeltaMatrix(DATA_TYPE.MEAN_CENTER_DISTANCE, true);
        lm.toExcel(wb, DATA_TYPE.MEAN_CENTER_DELTA.toString());
        lm = buildDeltaMatrix(DATA_TYPE.ANGLE_FROM_MEAN, true);
        lm.toExcel(wb, DATA_TYPE.MEAN_ANGLE_DELTA.toString());
        // position velocities
        lm = buildDeltaMatrix(DATA_TYPE.ORIGIN_DISTANCE, false);
        lm.toExcel(wb, DATA_TYPE.ORIGIN_VELOCITY_DELTA.toString());
        lm = buildDeltaMatrix(DATA_TYPE.ANGLE_FROM_REFERENCE, false);
        lm.toExcel(wb, DATA_TYPE.ANGLE_VELOCITY_DELTA.toString());
        lm = buildDeltaMatrix(DATA_TYPE.MEAN_CENTER_DISTANCE, false);
        lm.toExcel(wb, DATA_TYPE.MEAN_CENTER_VELOCITY_DELTA.toString());
        lm = buildDeltaMatrix(DATA_TYPE.ANGLE_FROM_MEAN, false);
        lm.toExcel(wb, DATA_TYPE.MEAN_ANGLE_VELOCITY_DELTA.toString());

        // positions
        distanceFromOriginToExcelSheet( wb, "Distance from origin");
        distanceFromAverageCenterToExcelSheet( wb, "Distance from mean center");
        angleFromReferenceToExcelSheet( wb, "Angle from reference");
        angleFromMeanToExcelSheet( wb, "Angle from flock avg");

        // cluster info
        clusterIdToExcelSheet( wb, "Cluster ID");
        trajectoriesToExcelSheet(wb, "Trajectories");

        // dimension stats
        dimensionsToExcel(wb);

        if(stoRet != null){
            storageAndRetreivalToExcel(wb);
        }

        saveWorkbook(config.getOutFileName(RunConfiguration.XLSX_POSTFIX), wb);
    }

    public void samplesToArff(String fileName, DATA_TYPE type){

        Labeled2DMatrix lm = new Labeled2DMatrix(elapsedList.size(), agentSequenceMap.size()+1);
        ArrayList<String>colHeaders = new ArrayList<>();
        colHeaders.add("time");
        for(String s : agentSequenceMap.keySet()) {
            colHeaders.add(s);
        }

        lm.setColumnHeaders(colHeaders);

        for(int rowNum = 0; rowNum < elapsedList.size(); ++rowNum) {
            int colNum = lm.getColIndex("time");
            //lm.setEntry(rowNum, colNum, elapsedList.get(rowNum));
            lm.setEntry(rowNum, colNum, TimeUnit.SECONDS.toMillis(rowNum));
            for (String name : agentSequenceMap.keySet()) {
                colNum = lm.getColIndex(name);
                AgentSequence as = agentSequenceMap.get(name);
                /**** TODO: Figure out how to do this as a row
                FlockingBeliefCA fbca = as.agent;
                lm.setStringValue(fbca.getName(), "AgentBias", fbca.getBias().toString());
                 /****/
                AgentRecording ar = as.sequence.get(rowNum);

                double val = ar.distanceFromOrigin;
                switch (type) {
                    case MEAN_CENTER_DISTANCE:
                        val = ar.distanceFromAverageCenter;
                        break;
                    case ORIGIN_DISTANCE:
                        val = ar.distanceFromAverageCenter;
                        break;
                    case ANGLE_FROM_REFERENCE:
                        val = ar.angleFromReference;
                        break;
                    case ANGLE_FROM_MEAN:
                        val = ar.angleFromReference;
                        break;
                    case CLUSTER_ID:
                        val = ar.clusterID;
                        break;
                }
                lm.setEntry(rowNum, colNum, val);
            }
        }

        lm.toArff(fileName, type.toString());
    }

    public void statsToArff(String fileName, DATA_TYPE type){
        Labeled2DMatrix lm = new Labeled2DMatrix(agentSequenceMap.size(), STATS_TYPES.values().length);
        ArrayList<String>rowNames = new ArrayList<>(agentSequenceMap.keySet());
        ArrayList<String> colHeaders = new ArrayList<>();
        for(STATS_TYPES st : STATS_TYPES.values()){
            colHeaders.add(st.toString());
        }
        lm.setRowNames(rowNames);
        lm.setColumnHeaders(colHeaders);

        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            FlockingBeliefCA fbca = as.agent;
            lm.setStringValue(fbca.getName(), "AgentBias", fbca.getBias().toString());

            DimensionSample ds = as.getStats(type);
            lm.setEntry(fbca.getName(), STATS_TYPES.Fifth.toString(), ds.fifthPercentile);
            lm.setEntry(fbca.getName(), STATS_TYPES.Mean.toString(), ds.mean);
            lm.setEntry(fbca.getName(), STATS_TYPES.NintyFifth.toString(), ds.nintyfifthPercentile);
            lm.setEntry(fbca.getName(), STATS_TYPES.Variance.toString(), ds.variance);
        }

        lm.toArff(fileName, type.toString());
    }

    public void deltasToArff(String fileName, DATA_TYPE relation, Labeled2DMatrix lm){
        for(String name : agentSequenceMap.keySet()){
            AgentSequence as = agentSequenceMap.get(name);
            FlockingBeliefCA fbca = as.agent;
            lm.setStringValue(fbca.getName(), "AgentBias", fbca.getBias().toString());
        }

        lm.toArff(fileName, relation.toString());
    }

    public static void main(String[] args){
        System.out.println("Hello FlockRecorder");

        TimeSeries tsI = new TimeSeries(1);
        TimeSeries tsJ = new TimeSeries(1);

        TimeSeriesPoint tspI;
        TimeSeriesPoint tspJ;

        double t = 0;
        double offset = 0.0;
        double amplitude = 2.0;
        double step = 0.1;
        while(t < 10) {
            double[] v1 = {Math.sin(t)};
            double[] v2 = {Math.sin(t+offset)*amplitude};
            tspI = new TimeSeriesPoint(v1);
            tspJ = new TimeSeriesPoint(v2);
            tsI.addLast(t, tspI);
            tsJ.addLast(t, tspJ);

            t += step;
        }

        System.out.println("FastDTW.getWarpDistBetween(tsI, tsJ) = "+FastDTW.getWarpDistBetween(tsI, tsJ));
    }
}
