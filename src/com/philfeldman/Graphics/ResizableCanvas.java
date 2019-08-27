package com.philfeldman.Graphics;

import com.philfeldman.GPGuis.Dprintable;
import com.philfeldman.GPGuis.LivePrintable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by philip.feldman on 7/29/2016.
 */
public class ResizableCanvas extends Canvas implements LivePrintable{
    private class MouseHandler implements EventHandler {
        protected double tx;
        protected double ty;
        protected double scalar;
        protected Point2D oldMouse;


        public MouseHandler(){
            oldMouse = new Point2D(-20,-20);
            tx = 0;
            ty = 0;
            scalar = 1;
        }

        private void mouseClicked(MouseEvent me){

            SmartShape selected = null;
            //dprint("clicked");
            for(int i = shapeList.size()-1; i >= 0; --i){ // get things in the revers order because we're using a painter's algorithm.
                SmartShape ss = shapeList.get(i);
                boolean hit = ss.testHit(me.getX(), me.getY());
                if (hit) {
                    //dprint("Hit!!!");
                    ss.setSelected(!ss.isSelected());
                    if(ss.isSelected()) {
                        selected = ss;
                        //dprint(ss.toString());
                    }
                    break;
                }
            }

            if(!me.isControlDown()) {
                for (SmartShape ss : shapeList) {
                    if (ss != selected) {
                        ss.setSelected(false);
                    }
                }
            }
        }

        private void mouseDragged(MouseEvent me) {
            //dprint("mouse dragged");
            double dist = oldMouse.distance(me.getX(), me.getY());
            if(dist > 20){
                oldMouse = new Point2D(me.getX(), me.getY());
            }

            Point2D newMouse = new Point2D(me.getX(), me.getY());
            Point2D deltaMouse = newMouse.subtract(oldMouse);
            tx += deltaMouse.getX();
            ty += deltaMouse.getY();
            oldMouse = newMouse;
        }

        private void mouseScrolled(ScrollEvent se) {
            double tz = se.getDeltaY()*0.0025;
            scalar *= (1.0+tz);

            //dprint ("mouse scrolled "+tz);

        }

        public double getScaledTx() {
            return tx/scalar;
        }

        public double getScaledTy() {
            return ty/scalar;
        }

        @Override
        public void handle(Event event) {

            if(event.getEventType() == MouseEvent.MOUSE_CLICKED) {
                mouseClicked((MouseEvent) event);
            }else if(event.getEventType() == MouseEvent.MOUSE_DRAGGED){
                mouseDragged((MouseEvent) event);
            }else if(event.getEventType() == ScrollEvent.SCROLL){
                mouseScrolled((ScrollEvent) event);
            }
        }
    }

    public static double DEFAULT_PIXEL_WORLD_SCALAR = 400.0;
    protected ArrayList<SmartShape> shapeList;
    protected ArrayList<String> lprintList;
    protected Stack<Affine> matStack;
    protected Affine curMat;
    protected GraphicsContext gc;
    protected Dprintable dprinter;
    protected MouseHandler mouseHandler;
    protected Font defaultText;
    protected Paint defaultTextColor;
    protected double pixelWorldScalar;
    protected int debugLevel;

    public ResizableCanvas(int w, int h) {
        super(w, h);
        pixelWorldScalar = DEFAULT_PIXEL_WORLD_SCALAR;
        dprinter = null;
        gc = getGraphicsContext2D();
        shapeList = new ArrayList<>();
        lprintList = new ArrayList<>();
        matStack = new Stack<>();
        curMat = new Affine();
        mouseHandler = new MouseHandler();
        defaultText = Font.font("Helvetica", 12);
        defaultTextColor = Paint.valueOf("black");
        setOnMouseClicked(mouseHandler);
        setOnMouseDragged(mouseHandler);
        setOnScroll(mouseHandler);
        debugLevel = 0;
        identity();
    }

    public void clear(Paint background, Paint border){
        pushMatrix();
            identity();
            gc.setFill(background);
            gc.fillRect(0,0,getWidth(), getHeight());
            if(border != null) {
                gc.setStroke(border);
                gc.setLineWidth(1);
                gc.strokeRect(0,0, getWidth(), getHeight());
            }


        popMatrix();
    }

    public void setDprinter(Dprintable dprinter) {
        this.dprinter = dprinter;
        if(shapeList != null) {
            for (SmartShape ss : shapeList) {
                ss.setDprinter(dprinter);
            }
        }
    }

    public void dprint(String str){
        if(dprinter == null){
            System.out.println(str);
        }else{
            dprinter.dprint(str);
        }
    }



    public void clearShapes(){
        shapeList.clear();
    }

    public ArrayList<SmartShape> getShapeList() {
        return shapeList;
    }

    public void drawShapes(){
        for(SmartShape ss : shapeList){
            ss.draw(this);
        }
    }

    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    public void lprint(String s){
        lprintList.add(s);
    }

    public void lprint(int level, String s){
        if(debugLevel >= level) {
            lprintList.add(s);
        }
    }

    public void lclear(){
        lprintList.clear();
    }

    public void lshow(double lineheight){
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(defaultTextColor);
        gc.setFont(defaultText);
        double lineval = 20;
        for(String s : lprintList) {
            String[] sa = s.split("\n");
            for(String ss : sa) {
                gc.fillText(ss, 10, lineval);
                lineval += lineheight;
            }
        }
        lclear();
    }

    public void addShape(SmartShape ss){
        shapeList.add(ss);
        ss.setAllShapesList(shapeList);
        ss.setLivePrinter(this);
    }

    public void behaveShapes(double elapsed){
        for(SmartShape ss : shapeList){
            ss.behavior(elapsed);
        }
    }

    public Affine getCurMat(){
        return curMat;
    }

    public void identity(){
        curMat.setToIdentity();
        gc.setTransform(curMat);
    }

    public void centerOrigin(){
        double x = getWidth()*0.5;
        double y = getHeight()*0.5;
        translate(x, y);
    }

    public void mouseTransforms(){
        scale(mouseHandler.scalar, mouseHandler.scalar);
        translate(mouseHandler.getScaledTx(), mouseHandler.getScaledTy());


        //translate(mouseHandler.tx, mouseHandler.ty);
        //scale(mouseHandler.scalar, mouseHandler.scalar);
    }

    public void pushMatrix(){
        Affine mat = new Affine(curMat);
        matStack.push(mat);
    }

    public void popMatrix(){
        curMat = matStack.pop();
        gc.setTransform(curMat);
    }

    public void translate(double x, double y){
        curMat.appendTranslation(x, y);
        gc.setTransform(curMat);
    }

    public void translate(Point2D pos){
        translate(pos.getX(), pos.getY());
    }

    public void rotate(double a){
        curMat.appendRotation(a);
        gc.setTransform(curMat);
    }

    public void scale(double sx, double sy){
        curMat.appendScale(sx, sy);
        gc.setTransform(curMat);
    }

    public double getPixelWorldScalar() {
        return pixelWorldScalar;
    }

    public void setPixelWorldScalar(double pixelWorldScalar) {
        this.pixelWorldScalar = pixelWorldScalar;
    }

    @Override
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
        //dprint("resize "+width+", "+height);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }
}
