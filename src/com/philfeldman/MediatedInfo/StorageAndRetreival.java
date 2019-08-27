package com.philfeldman.MediatedInfo;

import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.FlockingBeliefCA;
import com.philfeldman.Graphics.FlockingShape;
import com.philfeldman.Graphics.ResizableCanvas;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.Philosophy.ParticleStatement;
import com.philfeldman.Utils.TensorCellEvaluator;
import com.philfeldman.utils.collections.LongArrayList;
import com.philfeldman.utils.math.LabeledTensor;
import com.philfeldman.utils.math.TensorDimensionInfo;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.TreeSet;

public class StorageAndRetreival {

    public static int DEFAULT_SIZE = 10;
    protected LabeledTensor<CellAccumulator> lt;
    protected String xDimensionName;
    protected String yDimensionName;
    protected int[] maxIndicies;
    protected Paint fillColor;
    protected Paint fillColor2;
    protected Paint selectedColor;
    protected Paint borderColor;
    protected TreeSet<String> agentNameSet;
    protected TreeSet<BaseCA.BIAS> agentBiasSet;

    public StorageAndRetreival() {
        lt = new LabeledTensor();
        xDimensionName = "X";
        yDimensionName = "Y";
        borderColor = Color.BLUE;
        selectedColor = Color.ORANGE;
        fillColor = Color.GRAY;
        fillColor2 = Color.LIGHTGRAY;
        agentBiasSet = new TreeSet<>();
        agentBiasSet = new TreeSet<>();
    }

    public void addDimension(String name, int size, double mappingLow, double mappingHigh) {
        lt.addDimension(name, size, mappingLow, mappingHigh);
    }

    public void initialize(String val) {
        lt.initialize(new CellAccumulator("unset"));
        maxIndicies = lt.getDimensionSizes();
        agentNameSet = new TreeSet<>();
        agentBiasSet = new TreeSet<>();
    }

    // these are the axis we'll sample for drawing
    public void setDimensionNames(String xName, String yName) {
        xDimensionName = xName;
        yDimensionName = yName;
    }

    public LabeledTensor<CellAccumulator> getLabeledTensor() {
        return lt;
    }

    public void clear() {
        if (lt != null) {
            lt.clear();
        }
        xDimensionName = "X";
        yDimensionName = "Y";
    }

    public TensorDimensionInfo getInfo(String name) {
        ArrayList<TensorDimensionInfo> tdiList = lt.getInfoList();
        for (TensorDimensionInfo tdi : tdiList) {
            if (name.equals(tdi.getName())) {
                return tdi;
            }
        }
        return null;
    }

    public boolean fillTensor(int[] curIndices, int[] maxIndices, int curIndex, TensorCellEvaluator tce) {
        if (curIndex > curIndices.length - 1) {
            // if we get here, we're done
            return true;
        }
        for (int i = 0; i < maxIndices[curIndex]; ++i) {
            curIndices[curIndex] = i;
            boolean done = fillTensor(curIndices, maxIndices, curIndex + 1, tce);
            if (done) {
                tce.evaluate(curIndices, lt);
            }
        }
        // if we get here, we're not done, keep going
        return false;
    }

    public void addData(FlockingShape fs) {
        FlockingBeliefCA ca = fs.getAgent();
        FlockingBeliefCA fbca = fs.getAgent();
        agentNameSet.add(fbca.getName());
        agentBiasSet.add(fbca.getBias());
        ParticleBelief pb = ca.getCurBelief();

        String[] dimNames = lt.getDimensionNames();
        double[] posArray = new double[lt.getDegree()];
        for(int i = 0; i < dimNames.length; ++i) {
            String dimName = dimNames[i];
            ParticleStatement ps = pb.getStatement(dimName);
            TensorDimensionInfo tdi = getInfo(dimName);
            double pos = ps.getValue() - (tdi.getMappingStep() * 0.5);
            posArray[i] = pos;
        }
        int[] iArray = lt.indexFromLocation(posArray);
        //System.out.printf("%s[%d][%d] = (%.2f, %.2f), (%d, %d)\n", ca.getName(), xIndex, yIndex, posArray[xIndex], posArray[yIndex], iArray[xIndex], iArray[yIndex]);
        try {
            CellAccumulator cellAccumulator = lt.get(iArray);
            cellAccumulator.add(fs);
        } catch (IndexOutOfBoundsException e) {
            //System.out.println("StorageAndRetreival.addData("+fs.getName()+") out of bounds: " + lt.toString());
        }

        /***
        ParticleStatement xStatement = pb.getStatement(xDimensionName);
        ParticleStatement yStatement = pb.getStatement(yDimensionName);

        ArrayList<TensorDimensionInfo> tdiList = lt.getInfoList();
        TensorDimensionInfo xInfo = getInfo(xDimensionName);
        TensorDimensionInfo yInfo = getInfo(yDimensionName);
        double[] posArray = new double[lt.getDegree()];
        if (xInfo != null && yInfo != null) {
            int xIndex = tdiList.indexOf(xInfo);
            int yIndex = tdiList.indexOf(yInfo);
            posArray[xIndex] = xStatement.getValue() - (xInfo.getMappingStep() * .5);
            posArray[yIndex] = yStatement.getValue() - (yInfo.getMappingStep() * .5);
            int[] iArray = lt.indexFromLocation(posArray);
            //System.out.printf("%s[%d][%d] = (%.2f, %.2f), (%d, %d)\n", ca.getName(), xIndex, yIndex, posArray[xIndex], posArray[yIndex], iArray[xIndex], iArray[yIndex]);
            try {
                CellAccumulator cellAccumulator = lt.get(iArray);
                cellAccumulator.add(fs);
            } catch (IndexOutOfBoundsException e) {
            }
        }
         ****/
    }

    public void draw(ResizableCanvas rc) {
        GraphicsContext gc = rc.getGraphicsContext2D();
        ArrayList<TensorDimensionInfo> tdiList = lt.getInfoList();
        TensorDimensionInfo xInfo = getInfo(xDimensionName);
        TensorDimensionInfo yInfo = getInfo(yDimensionName);

        if (xInfo != null && yInfo != null) {
            LongArrayList<CellAccumulator> lal = lt.getRawList();
            double redMax = 0;
            double greenMax = 0;
            for (long i = 0; i < lal.size(); ++i) {
                CellAccumulator ca = lal.get(i);
                ca.decay(CellAccumulator.DEFAULT_DECAY_FACTOR);
                if (ca != null) {
                    redMax = Math.max(redMax, ca.getBiasValue(BaseCA.BIAS.RED_BIAS));
                    greenMax = Math.max(greenMax, ca.getBiasValue(BaseCA.BIAS.GREEN_BIAS));
                } else {
                    //System.out.println("StorageAndRetreival.draw(): CellAccumulator["+i+"] is null!");
                }
            }
            int xIndex = tdiList.indexOf(xInfo);
            int yIndex = tdiList.indexOf(yInfo);
            //rc.lprint(String.format("coord: (%.2f, %.2f), size:(%.2f, %.2f)", xInfo.getMappingLow(), yInfo.getMappingLow(), xInfo.getMappingSize(), yInfo.getMappingSize()));

            gc.setStroke(borderColor);
            gc.setLineWidth(2);
            double ps = rc.getPixelWorldScalar() * 0.5;
            gc.strokeRect(xInfo.getMappingLow() * ps, yInfo.getMappingLow() * ps, xInfo.getMappingSize() * ps, yInfo.getMappingSize() * ps);

            double[] posArray = new double[lt.getDegree()];
            for (double xPos = xInfo.getMappingLow(); xPos < xInfo.getMappingHigh(); xPos += xInfo.getMappingStep()) {
                double x = xPos * ps;
                double xs = xInfo.getMappingStep() * ps;
                //rc.lprint(String.format("[%d] = %.2f", xIndex, xPos));
                posArray[xIndex] = xPos;
                for (double yPos = yInfo.getMappingLow(); yPos < yInfo.getMappingHigh(); yPos += yInfo.getMappingStep()) {
                    double y = yPos * ps;
                    double ys = yInfo.getMappingStep() * ps;
                    posArray[yIndex] = yPos + yInfo.getMappingStep() * 0.1;

                    try {
                        int[] iArray = lt.indexFromLocation(posArray);
                        CellAccumulator cellAccumulator = lt.get(iArray);
                        if (cellAccumulator == null) {
                            gc.setFill(Color.BLACK);
                        } else {
                            // set colors based on the ratio of current cell to max
                            double redVal = 0.0;
                            if (redMax > 0) {
                                redVal = Math.max(0.0, cellAccumulator.getBiasValue(BaseCA.BIAS.RED_BIAS) / redMax);
                            }

                            double greenVal = 0.0;
                            if (greenMax > 0) {
                                greenVal = Math.max(0.0, cellAccumulator.getBiasValue(BaseCA.BIAS.GREEN_BIAS) / greenMax);
                            }
                            double blueVal = 0.0;
                            Color c = new Color(redVal, greenVal, blueVal, (redVal + greenVal) * 0.5);
                            gc.setFill(c);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        gc.setFill(Color.DARKRED);
                    }
                    gc.fillRect(x, y, xs, ys);
                    //rc.lprint(String.format("%.2f, %.2f), (%.2f, %.2f)", x, y, xs, ys));
                }
            }
        }
    }

    @Override
    public String toString() {
        return lt.toString();
    }

    public void xyToExcel(Sheet sheet, int rowIndex) {
        TensorDimensionInfo xInfo = getInfo(xDimensionName);
        TensorDimensionInfo yInfo = getInfo(yDimensionName);

        ArrayList<TensorDimensionInfo> tdiList = lt.getInfoList();
        int xIndex = tdiList.indexOf(xInfo);
        int yIndex = tdiList.indexOf(yInfo);

        int[] iArray = new int[lt.getDegree()];

        Row row;
        for(BaseCA.BIAS b : agentBiasSet) {
            rowIndex++;
            row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue("bias");
            row.createCell(1).setCellValue(b.toString());
            for (int y = 0; y < lt.getDimensionSize(yDimensionName); ++y) {
                row = sheet.createRow(rowIndex++);
                iArray[yIndex] = y;
                for (int x = 0; x < lt.getDimensionSize(xDimensionName); ++x) {
                    iArray[xIndex] = x;
                    CellAccumulator ca = lt.get(iArray);
                    double val = 0;
                    try{
                        val = ca.getBiasCountMap().get(b);
                    }catch (NullPointerException e){}
                    row.createCell(x+1).setCellValue(val);
                    // put on the row
                }
            }
        }

        for(String name : agentNameSet){
            rowIndex++;
            row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue("agent");
            row.createCell(1).setCellValue(name);
            for (int y = 0; y < lt.getDimensionSize(yDimensionName); ++y) {
                row = sheet.createRow(rowIndex++);
                iArray[yIndex] = y;
                for (int x = 0; x < lt.getDimensionSize(xDimensionName); ++x) {
                    iArray[xIndex] = x;
                    CellAccumulator ca = lt.get(iArray);
                    double val = 0;
                    try{
                        val = ca.getAgentCountMap().get(name);
                    }catch (NullPointerException e){}
                    row.createCell(x+1).setCellValue(val);
                }
            }
        }
    }

    public static void main(String[] args) {
        LabeledTensor<Integer> lt = new LabeledTensor<>();
        lt.addDimension("X", 3, 0, 1);
        lt.addDimension("Y", 4, 0, 1);
        lt.addDimension("Z", 5, 0, 1);
        lt.initialize(0);

        for (int x = 0; x < lt.getDimensionSize("X"); ++x) {
            for (int y = 0; y < lt.getDimensionSize("Y"); ++y) {
                for (int z = 0; z < lt.getDimensionSize("Z"); ++z) {
                    int val = x * 100 + y * 10 + z;
                    lt.set(new int[]{x, y, z}, val);
                }
            }
        }

        System.out.println(lt.toString());
    }
}
