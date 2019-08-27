package com.philfeldman.GPCAs;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;

/**
 * Created by philip.feldman on 7/26/2016.
 */
public class SingleDistributionCA extends BaseCA {
    protected ArrayList<Double> valueList;
    protected double sumVal;
    protected double meanVal;
    protected double sumSquares;
    protected double variance;

    public SingleDistributionCA(BIAS bias) {
        super(bias);
    }

    public void findValues(ArrayList<SingleDistributionCA> caList, int numSamples){
        for(int i = 0; i < numSamples; ++i){
            int index = rand.nextInt(caList.size());
            SingleDistributionCA bca = caList.get(index);
            addValue(bca.getMean());
        }
    }

    public void addValue(double val) {
        valueList.add(val);
        double count = valueList.size();
        sumVal += val;
        meanVal = sumVal / count;
        sumSquares = 0;
        for(double v : valueList){
            sumSquares += (v - meanVal) * (v - meanVal);
        }

        variance = sumSquares / count;
    }

    public double getMean() {
        return meanVal;
    }

    public double getVariance() {
        return variance;
    }

    public double getSD() {
        return Math.sqrt(variance);
    }

    @Override
    public String toString() {
        return String.format(bias+": Mean = %.2f, Variance = %.2f", getMean(), getVariance());
    }

    public static void toExcelHeader(Sheet sheet, int row){
        Row exRow = sheet.createRow(row);
        exRow.createCell(0).setCellValue("Type");
        exRow.createCell(1).setCellValue("Mean");
        exRow.createCell(2).setCellValue("StdDev");
        exRow.createCell(3).setCellValue("Variance");
    }

    public void toExcel(Sheet sheet, int row){
        Row exRow = sheet.createRow(row);
        exRow.createCell(0).setCellValue(bias.toString());
        exRow.createCell(1).setCellValue(getMean());
        exRow.createCell(2).setCellValue(getSD());
        exRow.createCell(3).setCellValue(getVariance());
    }

    public static void main(String[] args) {
        SingleDistributionCA bca = new SingleDistributionCA(BIAS.SOURCE);
        for(int i = 1; i < 14; ++i){
            bca.addValue(i);
        }
        System.out.println("bca mean = "+bca.getMean());
        System.out.println("bca variance = "+bca.getVariance());
    }
}
