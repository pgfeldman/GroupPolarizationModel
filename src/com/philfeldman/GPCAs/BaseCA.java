package com.philfeldman.GPCAs;

import com.philfeldman.GPGuis.LivePrintable;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by philip.feldman on 7/21/2016.
 */
public class BaseCA {
    public enum BIAS {CONTROL, SOURCE, CONFIRMER, AVOIDER, GREEN_BIAS, RED_BIAS};
    public enum STATE {ACTIVE, INACTIVE, DORMANT}

    protected BIAS bias;
    protected STATE state;
    protected Random rand;
    protected LivePrintable lprinter;
    protected ArrayList<CAInfluences> influencesList;

    public BaseCA(BIAS bias) {
        this.bias = bias;
        rand = new Random();
        state = STATE.ACTIVE;
        influencesList = null;
    }

    public BaseCA(BIAS bias, STATE state) {
        this(bias);
        this.state = state;
    }

    public BIAS getBias(){
        return bias;
    }

    public STATE getState() {
        return state;
    }

    public void setState(STATE state) {
        this.state = state;
    }

    public ArrayList<CAInfluences> getInfluencesList() {
        return influencesList;
    }

    public void setInfluencesList(ArrayList<CAInfluences> influencesList) {
        this.influencesList = influencesList;
    }

    @Override
    public String toString() {
        return ("State = "+state.toString()+", Bias = "+bias.toString());
    }

    public static void toExcelHeader(Sheet sheet, int row){
        Row exRow = sheet.createRow(row);
    }

    public void toExcel(Sheet sheet, int row){
        Row exRow = sheet.createRow(row);
    }

    public void setLprinter(LivePrintable lprinter) {
        this.lprinter = lprinter;
    }

    public void lprint(String s){
        if(lprinter != null){
            lprinter.lprint(s);
        } else {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {
        BaseCA bca = new BaseCA(BIAS.SOURCE);
    }
}
