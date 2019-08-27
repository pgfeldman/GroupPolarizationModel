package com.philfeldman.GPMain;

import com.philfeldman.GPCAs.*;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class SingleDistributionMain {
    protected static int NUM_CAS = 10;
    protected static int NUM_SAMPLES = 50;
    protected ArrayList<SingleDistributionCA> sourceList;
    protected ArrayList<SingleDistributionCA>caList;
    protected Random rand;
    protected NormalDistribution distribution;
    protected Workbook workbook;

    public SingleDistributionMain() {
        rand = new Random();
        distribution = new NormalDistribution(); // default to a mean of 0 and an SD of 1
        workbook = new XSSFWorkbook();
        sourceList = new ArrayList<SingleDistributionCA>();
        caList = new ArrayList<SingleDistributionCA>();
    }

    public void initSources(int numSources, int numSamples){
        for(int i = 0; i < numSources; ++i){
            SingleDistributionCA bca = new SingleDistributionCA(BaseCA.BIAS.SOURCE);
            for(int j = 0; j < numSamples; ++j){
                //bca.addValue(distribution.sample()*100);
                bca.addValue((rand.nextDouble()-0.5)*100);
            }
            sourceList.add(bca);
            System.out.println("CA["+i+"]: "+bca.toString());
        }
    }

    public void initPopulation(int popSize, int numSamples){
        // initialize the populations
        for(int i = 0; i < popSize; ++i){
            SingleDistributionCA ctrlca = new SingleDistributionCA(BaseCA.BIAS.CONTROL);
            ExplorerCASingleDist eca = new ExplorerCASingleDist();
            ConfirmerCASingleDist cca = new ConfirmerCASingleDist();
            AvoiderCASingleDist aca = new AvoiderCASingleDist();
            for(int j = 0; j < numSamples; ++j) {
                SingleDistributionCA sourceCa = sourceList.get(rand.nextInt(sourceList.size()));
                ctrlca.addValue(sourceCa.getMean());
                eca.addValue(sourceCa.getMean());
                cca.addValue(sourceCa.getMean());
                aca.addValue(sourceCa.getMean());
            }
            caList.add(ctrlca);
            caList.add(eca);
            caList.add(cca);
            caList.add(aca);
        }
    }

    public void stepSim(int numSamples){
        for(SingleDistributionCA bca : caList){
            bca.findValues(sourceList, numSamples);
        }
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(BaseCA bca : sourceList){
            sb.append(bca.toString());
            sb.append("\n");
        }
        for(BaseCA bca : caList){
            sb.append(bca.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public void toExcel(String sheetName){
        int rownum = 0;
        Sheet sheet = workbook.createSheet(sheetName);
        BaseCA.toExcelHeader(sheet, rownum);
        for(BaseCA bca : sourceList){
            ++rownum;
            bca.toExcel(sheet, rownum);
        }
        for(BaseCA bca : caList){
            ++rownum;
            bca.toExcel(sheet, rownum);
        }
    }

    public void saveWorkbook(String prefix){
        SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yy-kk_mm_ss");
        Date d = new Date();
        String filename =  prefix+"_"+sdf.format(d)+".xlsx";
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            workbook.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        System.out.println("Hello GroupPolarazationMain!");
        SingleDistributionMain gpm = new SingleDistributionMain();

        gpm.initSources(NUM_CAS, NUM_SAMPLES);
        gpm.toExcel("Init Sources");

        gpm.initPopulation(10, 3);
        gpm.toExcel("Init Population");

        gpm.stepSim(NUM_SAMPLES);
        gpm.toExcel("Simulation");


        gpm.saveWorkbook("./data/test");
    }
}
