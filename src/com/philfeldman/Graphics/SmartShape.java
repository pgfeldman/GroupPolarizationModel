package com.philfeldman.Graphics;

import com.philfeldman.GPGuis.Dprintable;
import com.philfeldman.GPGuis.LivePrintable;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import org.apache.commons.math3.ml.clustering.Clusterable;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by philip.feldman on 7/29/2016.
 */
public class SmartShape implements Clusterable{


    public static enum SHAPE_TYPE{RECT, ROUNDED_RECT, OVAL, TRIANGLE};
    public static final double DEFAULT_SCALE = 1.0;

    protected SHAPE_TYPE type;
    protected String name;
    protected double angle;
    protected Point2D posVec;
    protected BoundingBox bbox;
    protected Affine mat;
    protected Affine invMat;
    protected Paint fillColor;
    protected Paint selectedColor;
    protected Paint borderColor;
    protected double elapsed;
    protected double oldElapsed;
    protected double dClock;
    protected double timeScalar;
    protected double sizeScalar;
    protected double pixelWorldScalar;
    protected boolean isSelected;
    protected boolean wasSelected;
    protected ArrayList<SmartShape> allShapesList;
    protected Random rand;
    protected double randVal;
    protected Dprintable dprinter;
    protected LivePrintable lprinter;

    public SmartShape(String n, Paint c1, Paint c2){
        type = SHAPE_TYPE.RECT;
        name = n;
        fillColor = c1;
        borderColor =c2;
        selectedColor = Paint.valueOf("yellow");
        elapsed = 0;
        oldElapsed = 0;
        timeScalar = 0.1;
        isSelected = false;
        wasSelected = false;
        rand = new Random();
        randVal = rand.nextDouble();
        dprinter = null;
        lprinter = null;
        allShapesList = null;
        sizeScalar = DEFAULT_SCALE;
        pixelWorldScalar = ResizableCanvas.DEFAULT_PIXEL_WORLD_SCALAR /2.0;
    }

    public SHAPE_TYPE getType() {
        return type;
    }

    public void setType(SHAPE_TYPE type) {
        this.type = type;
    }

    public void setFillColor(Paint fillColor) {
        this.fillColor = fillColor;
    }

    public void setPos(double x, double y){
        posVec = new Point2D(x, y);
    }

    public void setPixelWorldScalar(double pixelWorldScalar) {
        this.pixelWorldScalar = pixelWorldScalar;
    }

    public Point2D getPos() {
        return posVec;
    }

    public double[] getPoint(){
        double[] point = {posVec.getX(), posVec.getY()};
        return point;
    }

    public void setAngle(double a){
        angle = a;
    }

    public void setSize(double w, double h){
        bbox = new BoundingBox(-w*0.5, -h*0.5, w, h);
    }

    public void setAllShapesList(ArrayList<SmartShape> allShapesList) {
        this.allShapesList = allShapesList;
    }

    public boolean testHit(double x, double y){
        if(invMat == null){
            dprint("SmartShape.testHit ERROR - invMat is null");
        }
        //dprint("Testing "+x+", "+y);

        Point2D xform = invMat.transform(x, y);
        //Point2D xform = mat.inverseTransform(x, y);
        //dprint("Transformed "+xform.getX()+", "+xform.getY());

        return bbox.contains(xform);
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

    public void setLivePrinter(LivePrintable lp) {
        this.lprinter = lp;
    }

    public void lprint(String str){
        if(lprinter != null){
            lprinter.lprint(str);
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    protected void calcTime(double e){
        dClock = elapsed - oldElapsed;
        oldElapsed = elapsed;
        elapsed = e;
    }


    public void selectedBehavior(){
        if(isSelected){
            lprint(toString());
            if(!wasSelected){
                dprint(toString());
            }
        }
        wasSelected = isSelected;
    }

    public void behavior(double e){
        calcTime(e);
        selectedBehavior();
    }

    public String getName() {
        return name;
    }

    public double getRandVal() {
        return randVal;
    }

    public void setRandVal(double randVal) {
        this.randVal = randVal;
    }

    public double getSizeScalar() {
        return sizeScalar;
    }

    public void setSizeScalar(double sizeScalar) {
        this.sizeScalar = sizeScalar;
    }

    public void customDrawStep(GraphicsContext gc){

    }

    public void draw(ResizableCanvas rc){

        GraphicsContext gc = rc.getGraphicsContext2D();

        if(isSelected) {
            gc.setFill(selectedColor);
        }else {
            gc.setFill(fillColor);
        }
        gc.setStroke(borderColor);
        gc.setLineWidth(1);

        rc.pushMatrix();
            rc.translate(posVec);
            rc.rotate(angle);
            rc.scale(sizeScalar, sizeScalar);
            switch(type) {
                case RECT:
                gc.fillRect(bbox.getMinX(), bbox.getMinY(), bbox.getWidth(), bbox.getHeight());
                gc.strokeRect(bbox.getMinX(), bbox.getMinY(), bbox.getWidth(), bbox.getHeight());
                    break;
                case ROUNDED_RECT:
                    double arcWidth = bbox.getWidth()*0.2;
                    double arcHeight = bbox.getHeight()*0.2;
                    gc.fillRoundRect(bbox.getMinX(), bbox.getMinY(), bbox.getWidth(), bbox.getHeight(), arcWidth, arcHeight);
                    gc.strokeRoundRect(bbox.getMinX(), bbox.getMinY(), bbox.getWidth(), bbox.getHeight(), arcWidth, arcHeight);
                    break;
                case OVAL:
                    gc.fillOval(bbox.getMinX(), bbox.getMinY(), bbox.getWidth(), bbox.getHeight());
                    gc.strokeOval(bbox.getMinX(), bbox.getMinY(), bbox.getWidth(), bbox.getHeight());
                    break;
                case TRIANGLE:
                    double[] xArray = {bbox.getMinX(), bbox.getMaxX(), bbox.getMinX()};
                    double[] yArray = {bbox.getMinY(), 0, bbox.getMaxY()};
                    gc.fillPolygon(xArray, yArray, 3);
                    gc.strokePolygon(xArray, yArray, 3);
                    break;
            }
            customDrawStep(gc);
            mat = new Affine(rc.getCurMat());
        rc.popMatrix();

        invMat = null;
        try {
            invMat = mat.createInverse();
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return String.format("%s pos = (%.2f, %.2f), angle = %.2f", name, posVec.getX(), posVec.getY(), angle);
    }
}
