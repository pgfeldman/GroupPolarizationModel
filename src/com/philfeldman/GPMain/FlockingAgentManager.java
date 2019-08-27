package com.philfeldman.GPMain;

import com.philfeldman.GPCAs.FlockingBeliefCA;
import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.SourceBeliefCA;
import com.philfeldman.GPGuis.Dprintable;
import com.philfeldman.GPGuis.LivePrintable;
import com.philfeldman.Graphics.*;
import com.philfeldman.MediatedInfo.CellAccumulator;
import com.philfeldman.MediatedInfo.StorageAndRetreival;
import com.philfeldman.Philosophy.Belief;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.Philosophy.ParticleStatement;
import com.philfeldman.Utils.*;
import com.philfeldman.utils.collections.LongArrayList;
import com.philfeldman.utils.math.Labeled2DMatrix;
import com.philfeldman.utils.math.LabeledTensor;
import com.philfeldman.utils.xmlParsing.Dom4jUtils;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static com.philfeldman.Utils.RunConfiguration.ARFF_POSTFIX;
import static com.philfeldman.Utils.RunConfiguration.XLSX_POSTFIX;
import static com.philfeldman.utils.jfxGUI.Popups.warnAlert;

/**
 * Created by philip.feldman on 8/2/2016.
 */
public class FlockingAgentManager implements Dprintable {
    protected class HeadingHelper{
        String flockName;
        ArrayRealVector flockAverageHeading;
        ArrayRealVector inverseFlockAverageHeading;
        FlockingShape leader;
        FlockingShape randLeader;

        public HeadingHelper(String name){
            flockName = name;
            leader = null;
        }

        public void setLeaders(List<FlockingShape> flock){
            int index = rand.nextInt(flock.size());
            randLeader = flock.get(index);
            if(leader == null){
                leader = randLeader;
            }
        }

        public void calcInvert(){
            inverseFlockAverageHeading = (ArrayRealVector) flockAverageHeading.mapMultiply(-1.0);
        }
    }

    //------------------------------------------- Global Variables
    public static enum BATCH_STATE {
        INTERACTIVE, INIT, START, SETTLE, RUN, STOP, SAVE, NEXT, S1, S2, S3, S4, S5
    }

    public static enum HERDING_TYPE {
        NO_HERDING, AVERAGE_HEADING, RANDOM_AGENT, RANDOM_AGENTS
    }

    public static int RED_FLOCK = 0;
    public static int GREEN_FLOCK = 1;
    public static String[] flockColors = {"red", "green"};
    public static String[] flockNames = {"RedFlock", "GreenFlock"};

    protected BATCH_STATE batchState;
    protected RunConfiguration currentConfig;
    protected WordEmbeddings currentEmbeddings;
    protected Stack<RunConfiguration> configStack;
    protected FlockRecorder recorder;
    protected ResizableCanvas canvas;
    protected LivePrintable lprinter;
    protected Dprintable dprinter;
    protected Random rand;
    protected boolean simRunning;
    protected ArrayList<FlockingShape> allBoidsList;
    protected ArrayList<SourceShape> allSourceList;
    protected TreeMap<String, List<FlockingShape>> flockListsMap;
    protected HashMap<String, HeadingHelper> flockHeadingMap;
    protected StorageAndRetreival stoRet;


    //--------------------------------------- Methods
    public FlockingAgentManager() {
        rand = new Random();
        allBoidsList = new ArrayList<>();
        allSourceList = new ArrayList<>();
        flockListsMap = new TreeMap<>();
        flockHeadingMap = new HashMap<>();
        stoRet = new StorageAndRetreival();
        configStack = new Stack<>();
        batchState = BATCH_STATE.INTERACTIVE;
        currentConfig = new RunConfiguration();
        currentConfig.setDefaults();
        currentEmbeddings = new WordEmbeddings();
    }

    public void setCanvas(ResizableCanvas canvas) {
        this.canvas = canvas;
        setLivePrinter(canvas);
    }

    public RunConfiguration getCurrentConfig() {
        return currentConfig;
    }

    public void configRecorder() {
        recorder = new FlockRecorder(currentConfig);
        for (FlockingShape fs : allBoidsList) {
            recorder.add(fs);
        }
    }

    public void setInfluenceRadius(double influenceRadius, BaseCA.BIAS bias) {
        for (String flockName : flockListsMap.keySet()) {
            List<FlockingShape> flock = flockListsMap.get(flockName);
            for (FlockingShape fs : flock) {
                FlockingBeliefCA fbca = fs.getAgent();
                if (fbca.getBias() == bias) {
                    fs.setGroupRadius(influenceRadius);
                }
            }
        }
    }

    public void setBorderType(ParticleBelief.BORDER_TYPE borderType) {
        for (String flockName : flockListsMap.keySet()) {
            List<FlockingShape> flock = flockListsMap.get(flockName);
            for (FlockingShape fs : flock) {
                FlockingBeliefCA fbca = fs.getAgent();
                fbca.setBorderType(borderType);
            }
        }
    }

    public BATCH_STATE getBatchState() {
        return batchState;
    }

    public String getRecorderState() {
        if (recorder == null) {
            return String.format("%s: State = %s, Elapsed = %.2f, Recorder not set", currentConfig.sessionName, batchState.toString(), currentConfig.elapsed);
        }
        int samples = recorder.elapsedList.size();
        return String.format("%s: State = %s, Elapsed = %.2f, Samples recorded = %d", currentConfig.sessionName, batchState.toString(), currentConfig.elapsed, samples);
    }

    public boolean isSimRunning() {
        return simRunning;
    }

    public void setSimRunning(boolean simRunning) {
        this.simRunning = simRunning;
    }

    protected FlockingShape initAgent(String name, String flockName, String color, int size) {
        return initAgent(name, flockName, Paint.valueOf(color), size);
    }

    protected FlockingShape initAgent(String name, String flockName, Paint paint, int size) {
        FlockingShape ss = new FlockingShape(name, flockName, paint, Paint.valueOf("black"));
        ss.setSize(size * 2.0, size);
        ss.setAngle(0);
        ss.setType(SmartShape.SHAPE_TYPE.TRIANGLE);

        return ss;
    }

    public void batchRunStateMachine() {
        switch (batchState) {
            case INTERACTIVE:
                return;
            case INIT:
                initializeAgents();
                initializeStorage();
                batchState = BATCH_STATE.START;
                break;
            case START:
                currentConfig.elapsed = 0;
                currentConfig.recordingState = false;
                setSimRunning(true);
                batchState = BATCH_STATE.SETTLE;
                break;
            case SETTLE:
                if (currentConfig.elapsed > currentConfig.settlingTime) {
                    currentConfig.recordingState = true;
                    configRecorder();
                    batchState = BATCH_STATE.RUN;
                }
                break;
            case RUN:
                if (currentConfig.samplesRecorded >= currentConfig.samplesToRecord) {
                    currentConfig.recordingState = false;
                    batchState = BATCH_STATE.SAVE;
                }
                break;
            case SAVE:
                String filename;
                switch (currentConfig.saveFormat) {
                    case EXCEL:
                        filename = currentConfig.getOutFileName(XLSX_POSTFIX);
                        toExcel(filename);
                        break;
                    case WEKA:
                        toArff(currentConfig.measureDataType);
                        break;
                    case EXCEL_AND_WEKA:
                        filename = currentConfig.getOutFileName(XLSX_POSTFIX);
                        toExcel(filename);
                        toArff(currentConfig.measureDataType);
                        break;
                }
                batchState = BATCH_STATE.NEXT;
                break;
            case NEXT:
                System.out.println("Ready for next");
                if (configStack.size() > 0) {
                    int debugLevel = currentConfig.debugLevel;
                    currentConfig = configStack.pop();
                    currentConfig.debugLevel = debugLevel;
                    batchState = BATCH_STATE.INIT;
                } else {
                    setSimRunning(false);
                    initializeAgents();
                    initializeStorage();
                    currentConfig.elapsed = 0;
                    batchState = BATCH_STATE.INTERACTIVE;
                }
                break;
        }
    }

    public void behavior() {

        // if we're not interactive, then we're in some batch state
        if (batchState != BATCH_STATE.INTERACTIVE) {
            batchRunStateMachine();
        }

        for( SourceShape ss : allSourceList){
            ss.behavior(currentConfig.elapsed);
        }

        lprint(String.format("Alignment scalar = %.2f", getCurrentConfig().alignmentScalar));

        ArrayRealVector headingVec = null;
        for (String flockName : flockListsMap.keySet()) {
            List<FlockingShape> flock = flockListsMap.get(flockName);
            // We're going to have averages by flock so
            // create a set of empty vectors for each flock
            if(flock.size() > 0) {
                if (!flockHeadingMap.containsKey(flockName)) {
                    HeadingHelper hh = new HeadingHelper(flockName);
                    FlockingBeliefCA agent = flock.get(0).getAgent();
                    hh.flockAverageHeading = agent.getCurBelief().calcOrientVector();
                    hh.flockAverageHeading.set(0);
                    flockHeadingMap.put(flockName, hh);
                }
            }

            // update all the agent's previous states so there are no ordering effects
            for (FlockingShape fs : flock) {
                FlockingBeliefCA agent = fs.getAgent();
                agent.updatePrevState();
            }
        }

        // calculate average heading, based on the entire flock
        // calculate local behavior, based on neighbors' previous positions
        for (String flockName : flockListsMap.keySet()) {
            List<FlockingShape> flock = flockListsMap.get(flockName);
            if(flockHeadingMap.containsKey(flockName)) {
                HeadingHelper hh = flockHeadingMap.get(flockName);
                for (FlockingShape fs : flock) {
                    fs.behavior(currentConfig.elapsed);
                    FlockingBeliefCA agent = fs.getAgent();
                    headingVec = agent.getPrevBelief().calcOrientVector();
                    hh.flockAverageHeading = hh.flockAverageHeading.add(headingVec);
                }
                hh.flockAverageHeading.unitize();
                hh.calcInvert();
                hh.setLeaders(flock);
            }
        }


        HashMap<String, HashMap<FlockingShape, Double>> alignedFlockMap = new HashMap<>();
        for (String flockName : flockListsMap.keySet()) {
            String largeFlockName = flockListsMap.firstKey();
            List<FlockingShape> flock = flockListsMap.get(flockName);
            if(flock.size() == 0){
                continue;
            }
            HashMap<FlockingShape, Double> alignedShapeMap;
            if(flock.size() > 0 && !alignedFlockMap.containsKey(flockName)){
                alignedShapeMap = new HashMap<>();
                alignedFlockMap.put(flockName, alignedShapeMap);
            }else{
                alignedShapeMap = alignedFlockMap.get(flockName);
            }
            HeadingHelper hh = flockHeadingMap.get(flockName);
            HeadingHelper largeHh = flockHeadingMap.get(largeFlockName);
            if(hh == null || largeHh == null){
                System.out.println("FlockingAgentManager.behavior(): A HeadingHelper is null");
            }else {
                //canvas.lprint(flockName + " vec = " + hh.flockAverageHeading);
                //canvas.lprint(largeFlockName + " ivec = " + largeHh.flockAverageHeading);
            }
            double alignedWeight = 0;
            for (FlockingShape fs : flock) {
                // accumulate agent data into the environment. Currently, this builds a heat map
                stoRet.addData(fs);

                // this is the rough equivalent of a troll farm. Beliefs are amplified that are
                // 'most aligned'. Since the bots don't have belief intertia, they 'teleport' to
                // the most suseptable area in belief space
                FlockingBeliefCA agent = fs.getAgent();
                ParticleBelief cpb = agent.getCurBelief();
                if(currentConfig.herdingType != HERDING_TYPE.NO_HERDING) {
                    // calculate the difference of the agent's heading to the flock average
                    // weight the closest to the average the highest
                    ArrayRealVector dirVec = hh.flockAverageHeading;
                    if(currentConfig.inverseHerding && !flockName.equals(largeFlockName)) {
                        dirVec = largeHh.inverseFlockAverageHeading;
                    }
                    double dotProduct = cpb.calcOrientVector().dotProduct(dirVec);
                    if (Double.isNaN(dotProduct)) {
                        System.out.println("FlockingAgentManager.behavior() NaN problem: " + toString());
                    }
                    double length = dirVec.getNorm();
                    double cosTheta = Math.min(1.0, dotProduct / length);
                    double beliefAlignment = Math.toDegrees(Math.acos(cosTheta));
                    double weight = (1.0 - beliefAlignment/180.0)*currentConfig.herdingWeight + 1.0;
                    if(weight > alignedWeight){
                        alignedShapeMap.clear();
                        alignedShapeMap.put(fs, weight);
                        alignedWeight = weight;
                    }
                }
                // reset sizes and weights from the last time around
                if(currentConfig.herdingType != HERDING_TYPE.NO_HERDING) {
                    cpb.setWeight(Belief.INITIAL_WEIGHT);
                    cpb.setHerding(false);
                    fs.setSizeScalar(1.0);
                }
            }
        }

        if(currentConfig.herdingType != HERDING_TYPE.NO_HERDING){
            for (String flockName : alignedFlockMap.keySet()) {
                HeadingHelper hh = flockHeadingMap.get(flockName);
                if(currentConfig.herdingType == HERDING_TYPE.AVERAGE_HEADING) {
                    HashMap<FlockingShape, Double> alignedShapeMap = alignedFlockMap.get(flockName);
                    for (FlockingShape mostAligned : alignedShapeMap.keySet()) {
                        double alignedWeight = alignedShapeMap.get(mostAligned);
                        FlockingBeliefCA agent = mostAligned.getAgent();
                        ParticleBelief cpb = agent.getCurBelief();
                        //cpb.setWeight(alignedWeight);
                        cpb.setWeight(currentConfig.herdingWeight);
                        cpb.setHerding(true);
                        mostAligned.setSizeScalar(Math.log(alignedWeight) + 1.0);
                        //canvas.lprint(mostAligned.getName());
                    }
                }else if(currentConfig.herdingType == HERDING_TYPE.RANDOM_AGENT){
                    FlockingShape mostAligned = hh.leader;
                    FlockingBeliefCA agent = mostAligned.getAgent();
                    ParticleBelief cpb = agent.getCurBelief();
                    cpb.setWeight(currentConfig.herdingWeight);
                    cpb.setHerding(true);
                    mostAligned.setSizeScalar(Math.log(currentConfig.herdingWeight) + 1.0);
                }else if(currentConfig.herdingType == HERDING_TYPE.RANDOM_AGENTS){
                    FlockingShape mostAligned = hh.randLeader;
                    FlockingBeliefCA agent = mostAligned.getAgent();
                    ParticleBelief cpb = agent.getCurBelief();
                    cpb.setWeight(currentConfig.herdingWeight);
                    cpb.setHerding(true);
                    mostAligned.setSizeScalar(Math.log(currentConfig.herdingWeight) + 1.0);
                }
            }
        }
    }

    public void recordSample() {
        if (recorder != null) {
            currentConfig.samplesRecorded++;
            SettingRecording sr = new SettingRecording(currentConfig.elapsed, currentConfig);
            HashMap<String, ArrayRealVector> map = new HashMap<>();
            for(String flockName : flockHeadingMap.keySet()){
                HeadingHelper hh = flockHeadingMap.get(flockName);
                map.put(flockName, hh.flockAverageHeading);
            }
            recorder.recordSample(sr, map);
        }
    }

    public boolean initializeAgents() {
        if (canvas == null || currentConfig == null) {
            return false;
        }
        canvas.clearShapes();
        allBoidsList.clear();
        flockListsMap.clear();
        flockHeadingMap.clear();
        allSourceList.clear();


        Random rand = new Random();
        String shapeName;

        for (String s : flockNames) {
            ArrayList<FlockingShape> al = new ArrayList<>();
            flockListsMap.put(s, al);
        }
        currentConfig.slewRateVariance = Math.min(currentConfig.slewRateMean, currentConfig.slewRateVariance);
        currentConfig.velocityVariance = Math.min(currentConfig.velocityMean, currentConfig.velocityVariance);

        int numRedFlock = (int) (currentConfig.numAgents * currentConfig.redGreenRatio);
        for (int i = 0; i < currentConfig.numAgents; ++i) {
            String flockColor = flockColors[GREEN_FLOCK];
            String flockName = flockNames[GREEN_FLOCK];

            shapeName = "Sh_" + i;
            FlockingShape fs;
            FlockingBeliefCA cbca;

            if (i < numRedFlock) {
                flockName = flockNames[RED_FLOCK];
                flockColor = flockColors[RED_FLOCK];
                fs = initAgent(flockName + shapeName, flockName, flockColor, 16);
                cbca = fs.setAgent(BaseCA.BIAS.RED_BIAS, currentConfig);
                fs.setGroupRadius(currentConfig.redExploitRadius);
            } else {
                fs = initAgent(flockName + shapeName, flockName, flockColor, 16);
                cbca = fs.setAgent(BaseCA.BIAS.GREEN_BIAS, currentConfig);
                fs.setGroupRadius(currentConfig.greenExploitRadius);
            }
            cbca.setLprinter(canvas);

            // Handle currentConfig.numDimensions here
            for (int d = 0; d < currentConfig.dimensions; ++d) {
                String label = "s_" + d;
                double sl = currentConfig.stageLimit;
                double sl2 = sl / 2.0;
                cbca.addCoordinate(new ParticleStatement(label, rand.nextDouble() * sl - sl2, rand.nextDouble() * sl - sl2), Belief.INITIAL_WEIGHT);
            }
            fs.setDimensionNames("s_0", "s_1");

            // add to the global list
            allBoidsList.add(fs);

            // add a pointer to the global list to each shape
            fs.setFlockingShapeList(allBoidsList);

            // Add to the flock so that we can get flock headings
            List<FlockingShape> flock = flockListsMap.get(flockName);
            flock.add(fs);

            // do the initial calculation
            fs.calcPosFromBelief();

            // Add the shape to the canvas
            canvas.addShape(fs);
        }

        if(getCurrentEmbeddings().loadSuccess){
            double posScalar = ResizableCanvas.DEFAULT_PIXEL_WORLD_SCALAR /2.0;
            List<WordEmbedding> weList = currentEmbeddings.getEmbeddings();
            ArrayList<SourceBeliefCA> sbcaList = new ArrayList<>();
            for (WordEmbedding we : weList){
                double size = 10.0 * we.getCount();
                SourceShape ss = new SourceShape(we.getEntry(), new Color(1, 1, 1, 0.5), Color.BLACK);
                SourceBeliefCA sbca = new SourceBeliefCA(we);
                sbca.setConfig(currentConfig);
                ss.setAgent(sbca);
                allSourceList.add(ss);
                sbcaList.add(sbca);
                canvas.addShape(ss);
                // if we want dynamic behavion, make a list of these shapes and process them in the behavior() method
            }
            for(FlockingShape fs : allBoidsList){
                FlockingBeliefCA fbca = fs.getAgent();
                fbca.setSourceBeliefCAList(sbcaList);
            }
        }

        if (recorder != null) {
            for (SmartShape ss : canvas.getShapeList()) {
                if(ss instanceof FlockingShape) {
                    FlockingShape fs = (FlockingShape) ss;
                    recorder.add(fs);
                }
            }
        }

        return true;
    }


    public boolean initializeStorage() {
        System.out.println("Setting up storage");
        if (canvas == null || currentConfig == null) {
            return false;
        }
        stoRet.clear();

        // Handle currentConfig.numDimensions here
        for (int d = 0; d < currentConfig.dimensions; ++d) {
            String label = "s_" + d;
            double sl = currentConfig.stageLimit;
            double sl2 = sl / 2.0;
            stoRet.addDimension(label, StorageAndRetreival.DEFAULT_SIZE, -sl, sl);
        }
        stoRet.setDimensionNames("s_0", "s_1");
        LabeledTensor lt = stoRet.getLabeledTensor();
        int[] indicies = new int[currentConfig.dimensions];
        int[] maxIndicies = new int[currentConfig.dimensions];
        for (int d = 0; d < currentConfig.dimensions; ++d) {
            String label = "s_" + d;
            int size = lt.getDimensionSize(label);
            maxIndicies[d] = size;
        }
        stoRet.initialize("unset");

        stoRet.fillTensor(indicies, maxIndicies, 0, new TensorCellEvaluator() {
            @Override
            public String evaluate(int[] indicies, LabeledTensor lt) {
                int sum = IntStream.of(indicies).sum()%2;
                String flockName = flockNames[sum];
                //String name = flockName+Arrays.toString(indicies);
                String name = "cell_"+Arrays.toString(indicies);
                lt.set(indicies, new CellAccumulator(name));
                return flockName;
            }
        });


        LongArrayList<CellAccumulator> lal = lt.getRawList();
        for(long i = 0; i < lal.size(); ++i){
            CellAccumulator ca = lal.get(i);
            if(ca == null) {
                System.out.println("FlockingAgentManager.initializeStorage(): CellAccumulator["+i+"] is null!");
            }
        }

        System.out.println(stoRet.toString());

        return true;
    }

    public StorageAndRetreival getStoRet() {
        return stoRet;
    }

    public void toInfluenceXML(String filename){
        if (recorder != null) {
            try {
                recorder.influenceToXML(filename);
            } catch (IOException e) {
                warnAlert("FlockingAgentManager.toInfluenceXML(): Error: "+e.getMessage());
            }
        } else {
            warnAlert("FlockingAgentManager.toInfluenceXML(): recorder not set");
        }
    }

    public void toExcel(String filename) {
        if (recorder != null) {
            recorder.setStorageAndRetreival(stoRet);
            recorder.toExcel(filename);
        } else {
            warnAlert("FlockingAgentManager.samplesToExcel(): recorder not set");
        }
    }

    public void toArff(FlockRecorder.DATA_TYPE type) {
        String filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
        if (recorder != null) {
            Labeled2DMatrix lm;
            switch (type) {
                case ANGLE_FROM_MEAN:
                case ANGLE_FROM_REFERENCE:
                case CLUSTER_ID:
                case MEAN_CENTER_DISTANCE:
                case ORIGIN_DISTANCE:
                    recorder.samplesToArff(filename, type);
                    break;
                case ANGLE_FROM_FLOCK_MEAN_STATS:
                case ANGLE_FROM_REFERENCE_STATS:
                case CLUSTER_ID_STATS:
                case MEAN_CENTER_DISTANCE_STATS:
                case ORIGIN_DISTANCE_STATS: // all the STATS types
                    recorder.statsToArff(filename, type);
                    break;
                case DYNAMIC_TIME_WARPING:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE, true);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case ORIGIN_POSITION_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE, true);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case ANGLE_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE, true);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case MEAN_CENTER_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE, true);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case MEAN_ANGLE_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN, true);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case ORIGIN_VELOCITY_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE, false);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case ANGLE_VELOCITY_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE, false);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case MEAN_CENTER_VELOCITY_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE, false);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case MEAN_ANGLE_VELOCITY_DELTA:
                    filename = currentConfig.getOutFileName(type.toString(), ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN, false);
                    recorder.deltasToArff(filename, type, lm);
                    break;
                case ALL_MEASURES:
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.CLUSTER_ID.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.CLUSTER_ID);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE);
                    break;
                case ALL_MEASURES_STATS:
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_FLOCK_MEAN_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_FLOCK_MEAN_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.CLUSTER_ID_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.CLUSTER_ID_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE_STATS);
                    break;
                case ALL_DELTAS:
                    // positions
                    filename = currentConfig.getOutFileName(type.toString() + "_DYNAMIC_TIME_WARPING", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.DYNAMIC_TIME_WARPING, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.DYNAMIC_TIME_WARPING, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_ORIGIN_POSITION", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_POSITION_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_ANGLE", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_CENTER_POSITION", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_CENTER_ANGLE", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_ANGLE_DELTA, lm);
                    //velocities
                    filename = currentConfig.getOutFileName(type.toString() + "_ORIGIN_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_POSITION_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_ANGLE_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_VELOCITY_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_CENTER_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_VELOCITY_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_ANGLE_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_ANGLE_VELOCITY_DELTA, lm);
                    break;
                case ALL:
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.CLUSTER_ID.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.CLUSTER_ID);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE.toString(), ARFF_POSTFIX);
                    recorder.samplesToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_FLOCK_MEAN_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_FLOCK_MEAN_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.CLUSTER_ID_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.CLUSTER_ID_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE_STATS);
                    filename = currentConfig.getOutFileName(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE_STATS.toString(), ARFF_POSTFIX);
                    recorder.statsToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE_STATS);
                    // positions
                    filename = currentConfig.getOutFileName(type.toString() + "_DYNAMIC_TIME_WARPING", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.DYNAMIC_TIME_WARPING, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.DYNAMIC_TIME_WARPING, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_ORIGIN_POSITION", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_POSITION_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_ANGLE", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_CENTER_POSITION", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_CENTER_ANGLE", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN, true);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_ANGLE_DELTA, lm);
                    //velocities
                    filename = currentConfig.getOutFileName(type.toString() + "_ORIGIN_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ORIGIN_DISTANCE, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ORIGIN_POSITION_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_ANGLE_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_REFERENCE, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.ANGLE_VELOCITY_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_CENTER_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.MEAN_CENTER_DISTANCE, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_CENTER_VELOCITY_DELTA, lm);
                    filename = currentConfig.getOutFileName(type.toString() + "_MEAN_ANGLE_VELOCITY", ARFF_POSTFIX);
                    lm = recorder.buildDeltaMatrix(FlockRecorder.DATA_TYPE.ANGLE_FROM_MEAN, false);
                    recorder.deltasToArff(filename, FlockRecorder.DATA_TYPE.MEAN_ANGLE_VELOCITY_DELTA, lm);
                    break;
            }
        } else {
            warnAlert("FlockingAgentManager.samplesToArff(): recorder not set");
        }
    }

    public WordEmbeddings getCurrentEmbeddings(){
        return currentEmbeddings;
    }

    public void loadEmbed(File file){

        currentEmbeddings.clear();

        if (file.isFile()) {
            if (file.getName().endsWith(".xml")) {
                currentEmbeddings.loadFromXML(file);
            }
        }
    }

    public void saveConfig(File file) {
        //TODO: write out currentConfig as xml file that could be read in below
    }

    public RunConfiguration loadConfig(File file) {
        boolean goodload = false;
        List<Element> eList = new ArrayList<>();
        Dom4jUtils d4ju = new Dom4jUtils();
        configStack.clear();

        if (file.isFile()) {
            if (file.getName().endsWith(".xml")) {
                try {
                    d4ju.create(file);
                    eList = d4ju.findNodesByFullName("config.simulationRun");
                    for (Element e : eList) {
                        RunConfiguration rc = new RunConfiguration();
                        rc.loadFromXML(e);
                        configStack.push(rc);
                    }
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
            }

        }
        if (configStack.size() > 0) {
            Collections.reverse(configStack);
            currentConfig = configStack.pop();
            batchState = BATCH_STATE.INIT;
            return currentConfig;
        }
        return null;
    }

    public void setDprinter(Dprintable dprinter) {
        this.dprinter = dprinter;
    }

    public void setLivePrinter(LivePrintable lp) {
        this.lprinter = lp;
    }

    public void lprint(String str) {
        if (lprinter != null) {
            lprinter.lprint(str);
        }
    }

    public void dprint(String str) {
        if (dprinter == null) {
            System.out.println(str);
        } else {
            dprinter.dprint(str);
        }
    }

}
