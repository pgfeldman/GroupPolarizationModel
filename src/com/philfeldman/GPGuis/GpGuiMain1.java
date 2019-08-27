package com.philfeldman.GPGuis;

import com.philfeldman.GPMain.BeliefAgentManager;
import com.philfeldman.Graphics.ClusterCanvas;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.philfeldman.utils.jfxGUI.Popups.*;

/**
 * Created by philip.feldman on 7/28/2016.
 */
public class GpGuiMain1  extends Application implements Dprintable{
    //----------------------------- inner classes

    private class Renderer extends AnimationTimer  {
        int framecount;

        @Override
        public void handle(long now) {
            ClusterCanvas cc = guiVars.canvas;
            cc.behaveShapes(framecount);

            GraphicsContext gc = cc.getGraphicsContext2D();
            cc.identity();

            // background
            cc.clear(Paint.valueOf("white"), Paint.valueOf("#aaaaaa"));

            cc.pushMatrix();
                cc.centerOrigin();
                cc.mouseTransforms();
                cc.drawShapes();
            cc.popMatrix();
            cc.lshow(17);
            framecount++;
        }
    }

    private class GuiVars{
        final int PAD = 10;
        final int GAP = 5;
        final int MAX_WEIGHT = 10;
        Stage stage;
        Scene scene;
        GridPane grid;
        TextArea consoleText;
        TextField nameTextField;
        Button inFileButton;
        Slider attractionSlider;
        Slider repulsionSlider;
        Slider dragSlider;
        Slider antiBeliefSlider;
        Slider originSlider;
        Slider similaritySlider;
        Slider epsSlider;
        TextField inFileTextField;
        Button outFolderButton;
        TextField outFolderTextField;
        TextField outSessionTextField;
        Button saveButton;
        CheckBox randomAgentsCheck;

        ClusterCanvas canvas;
        Renderer renderer = new Renderer();


        public GuiVars(){
            renderer = new Renderer();
        }

        private void buildLeft(VBox leftBox) {
            grid = new GridPane();
            grid.setHgap(GAP);
            grid.setVgap(GAP);

            grid.add(new Label("Name: "), 0, 0);
            nameTextField = new TextField();
            nameTextField.setText(System.getProperty("user.name"));
            grid.add(nameTextField, 1, 0);
            inFileButton = new Button("Open files");
            inFileButton.setOnAction(
                    (final ActionEvent e) -> {
                        File file = chooseFile(System.getProperty("user.dir"), stage);

                        if (file != null) {
                            BeliefAgentManager.GuiVars gv = bam.loadConfig(file);
                            if(gv.success) {
                                dprint("successfully opened "+file.getName());
                                renderer.framecount = 0;

                                inFileTextField.setText(file.getName());
                                attractionSlider.adjustValue(gv.attractionPercent);
                                repulsionSlider.adjustValue(gv.repulsionPercent);
                                dragSlider.adjustValue(gv.dragCoef);
                                antiBeliefSlider.adjustValue(gv.antiBeliefScalar);
                                originSlider.adjustValue(gv.originScalar);
                                similaritySlider.adjustValue(gv.similarityPercent);
                                epsSlider.adjustValue(gv.similarityPercent);
                                randomAgentsCheck.setSelected(gv.useRandom);
                            }else{
                                dprint("error opening "+file.getName());
                            }
                        }
                    });
            grid.add(guiVars.inFileButton, 0,1);
            inFileTextField = new TextField();
            grid.add(inFileTextField, 1, 1);

            outFolderButton = new Button("Open folder");
            outFolderButton.setOnAction(
                    (final ActionEvent e) -> {
                        String filename  = chooseDirectory(System.getProperty("user.dir"), stage);
                        if (filename != null) {
                            outFolderTextField.setText(filename);
                            Tooltip tt = new Tooltip(filename);
                            outFolderTextField.setTooltip(tt);
                        }
                    });
            grid.add(guiVars.outFolderButton, 0,2);
            outFolderTextField = new TextField();
            grid.add(outFolderTextField, 1, 2);

            grid.add(new Label("Session Name:"), 0, 3);
            outSessionTextField = new TextField(System.getProperty("user.name")+"-GP");
            grid.add(outSessionTextField, 1, 3);

            String s = String.format("Attraction k %.1f", attractionPercent *100.0);
            Label attractionLabel = new Label(s);
            grid.add(attractionLabel, 0, 4);
            attractionSlider = addSlider(1, 4, attractionPercent *100.0);

            attractionSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                attractionPercent = (double)newValue/100.0;
                bam.setAttractionK(attractionPercent);
                String s2 = String.format("Attraction k %.1f", attractionPercent *100.0);
                attractionLabel.setText(s2);
            });

            s = String.format("Repusion k %.1f", repulsionPercent);
            Label repulsionLabel = new Label(s);
            repulsionLabel.setPrefWidth(100);
            grid.add(repulsionLabel, 0, 5);
            repulsionSlider = addSlider(1, 5, repulsionPercent);

            repulsionSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                repulsionPercent = (double)newValue;
                bam.setRepulsionK(repulsionPercent);
                String s2 = String.format("Repulsion k %.1f", repulsionPercent);
                repulsionLabel.setText(s2);
            });

            s = String.format("Drag %.1f", dragCoef *100.0);
            Label dragLabel = new Label(s);
            dragLabel.setPrefWidth(100);
            grid.add(dragLabel, 0, 6);
            dragSlider = addSlider(1, 6, dragCoef *100.0);

            dragSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                dragCoef = (double)newValue/100;
                bam.setDragCoef(dragCoef);
                String s2 = String.format("Drag %.1f", dragCoef *100.0);
                dragLabel.setText(s2);
            });
            s = String.format("AntiBelief %.1f", antiBeliefScalar*10);
            Label antiBeliefLabel = new Label(s);
            antiBeliefLabel.setPrefWidth(100);
            grid.add(antiBeliefLabel, 0, 7);
            antiBeliefSlider = addSlider(1, 7, antiBeliefScalar*10);

            antiBeliefSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                antiBeliefScalar = (double)newValue/10.0;
                bam.setAntiBeliefScalar(antiBeliefScalar);
                String s2 = String.format("AntiBelief %.1f", antiBeliefScalar*10);
                antiBeliefLabel.setText(s2);
            });

            s = String.format("Origin %.1f", originScalar*10);
            Label originLabel = new Label(s);
            originLabel.setPrefWidth(100);
            grid.add(originLabel, 0, 8);
            originSlider = addSlider(1, 8, originScalar*10);

            originSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                originScalar = (double)newValue/10.0;
                bam.setOriginScalar(originScalar);
                String s2 = String.format("Origin %.1f", originScalar*10);
                originLabel.setText(s2);
            });

            s = String.format("Cluster EPS %.1f", epsScalar*100);
            Label epsLabel = new Label(s);
            epsLabel.setPrefWidth(100);
            grid.add(epsLabel, 0, 9);
            epsSlider = addSlider(1, 9, epsScalar*100);

            epsSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                //dprint("Slider = "+newValue);
                epsScalar = newValue.floatValue()/100.0f;
                bam.setEpsScalar(epsScalar);
                String s2 = String.format("Cluster EPS %.1f", epsScalar*100);
                epsLabel.setText(s2);
            });

            s = String.format("Similarity %.1f", similarityPercent*100);
            Label similarityLabel = new Label(s);
            epsLabel.setPrefWidth(100);
            grid.add(similarityLabel, 0, 10);
            similaritySlider = addSlider(1, 10, similarityPercent*100);

            similaritySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                //dprint("Slider = "+newValue);
                similarityPercent = newValue.floatValue()/100.0f;
                bam.setSimilarityThresholds(similarityPercent);
                String s2 = String.format("Similarity %.1f", similarityPercent*100);
                similarityLabel.setText(s2);
            });

            //grid.setGridLinesVisible(true);
            leftBox.getChildren().add(grid);
            leftBox.getChildren().add(new Separator());
            randomAgentsCheck = new CheckBox("Random Agents");
            consoleText = new TextArea();
            consoleText.setPrefWidth(300);
            leftBox.getChildren().addAll(randomAgentsCheck, consoleText);
            VBox.setVgrow(consoleText, Priority.ALWAYS);
        }

        private Slider addSlider(int col, int row, double value){
            Slider s = new Slider();
            s.setMin(0);
            s.setMax(100);
            s.setValue(value);

            s.setShowTickLabels(true);
            s.setShowTickMarks(true);
            s.setPrefWidth(180);
            grid.add(s, col, row);
            return s;
        }

        private void buildCenter(VBox centerVbox){
            //centerVbox.setStyle("-fx-background-color: green");
            AnchorPane ap = new AnchorPane();
            //ap.setStyle("-fx-background-color: blue");

            centerVbox.getChildren().add(ap);
            VBox.setVgrow(ap, Priority.ALWAYS);

            canvas = new ClusterCanvas( 100, 100 );
            AnchorPane.setTopAnchor(canvas, 0.0);
            AnchorPane.setLeftAnchor(canvas, 0.0);
            AnchorPane.setRightAnchor(canvas, 0.0);
            AnchorPane.setBottomAnchor(canvas, 0.0);
            ap.getChildren().add(canvas);
        }

        private void buildBottom(HBox bottomHbox) {
            HBox rightBox = new HBox();
            rightBox.setSpacing(5);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            saveButton = new Button("Save");
            saveButton.setOnAction(event -> {
                saveFiles();
            });
            rightBox.getChildren().addAll(saveButton);

            HBox leftBox = new HBox();
            leftBox.setSpacing(5);
            leftBox.setAlignment(Pos.CENTER_LEFT);

            Button clearConsoleButton = new Button("Clear Console");
            clearConsoleButton.setOnAction(event -> {
                consoleText.setText("");
            });

            Button resetButton = new Button("Go!");
            resetButton.setOnAction(event -> {
                boolean rCheck = randomAgentsCheck.isSelected();
                canvas.clearShapes();
                bam.initializeAgents(rCheck);

                bam.setAttractionK(attractionPercent);
                bam.setRepulsionK(repulsionPercent);
                bam.setDragCoef(dragCoef);
                bam.setAntiBeliefScalar(antiBeliefScalar);
                bam.setOriginScalar(originScalar);
                bam.setEpsScalar(epsScalar);
                bam.setSimilarityThresholds(similarityPercent);

                renderer.framecount = 0;
            });
            Button startButton = new Button("Start GP");
            startButton.setOnAction(event -> {
                boolean state = !bam.isSimRunning();
                bam.setSimRunning(state);
                if(state){
                    startButton.setText("Stop GP");
                }else{
                    startButton.setText("Start GP");
                }
            });
            Button clusterButton = new Button("Calc Clusters");
            clusterButton.setOnAction(event -> {
                canvas.calcClusters();
            });

            leftBox.getChildren().addAll(clearConsoleButton, resetButton, startButton, clusterButton);
            bottomHbox.getChildren().addAll(leftBox, rightBox);
            bottomHbox.setAlignment(Pos.CENTER_RIGHT);

            HBox.setHgrow(leftBox, Priority.ALWAYS);
        }

        private void buildShapes(){
            bam.setCanvas(canvas);
            //fam.initializeAgents();
        }

        private void buildMainScreen(Stage stage) {
            this.stage = stage;
            BorderPane border = new BorderPane();
            Insets padding = new Insets(PAD, PAD, PAD, PAD);
            VBox leftBox = new VBox();
            leftBox.setPadding(padding);
            leftBox.setSpacing(GAP);
            border.setLeft(leftBox);
            buildLeft(leftBox);

            VBox centerVbox = new VBox();
            centerVbox.setPadding(padding);
            centerVbox.setSpacing(GAP);
            border.setCenter(centerVbox);
            buildCenter(centerVbox);

            HBox bottomHbox = new HBox();
            //bottomHbox.setPadding(padding);
            bottomHbox.setPadding(new Insets(0, PAD, PAD, PAD));
            bottomHbox.setSpacing(GAP);
            border.setBottom(bottomHbox);
            buildBottom(bottomHbox);

            buildShapes();

            scene = new Scene(border, 900, 700);
            stage.setScene(scene);

            renderer.start();
        }


    }

    //----------------------------------- Global variables
    protected GuiVars guiVars;
    protected double attractionPercent = .1;
    protected double repulsionPercent = 10;
    protected double antiBeliefScalar = 1;
    protected double originScalar = 1;
    protected float epsScalar = .1f;
    protected double similarityPercent = .5;
    protected double dragCoef = 0.1;
    protected BeliefAgentManager bam;

    //----------------------------------- Constructors
    public GpGuiMain1(){
        //cm = new CorpusManager();
        guiVars = new GuiVars();
        bam = new BeliefAgentManager();
        bam.setDprinter(this);
    }

    //----------------------------------- Methods

    public void dprint(String str){
        guiVars.consoleText.insertText(0, str+"\n");
    }



    protected void saveFiles(){
        String outfolderName = guiVars.outFolderTextField.getText();
        if(outfolderName.length() < 3){
            warnAlert("You need to set the output folder");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yy-kk_mm_ss");
        Date d = new Date();
        String outfile = outfolderName+"/"+guiVars.outSessionTextField.getText()+"_"+sdf.format(d)+".xlsx";
        dprint("Finished saving");
    }

    @Override
    public void start(Stage stage) throws Exception {
        guiVars.buildMainScreen(stage);

        guiVars.canvas.setDprinter(this);

        stage.setTitle("Physics-based Classifier");
        stage.setX(20);
        stage.setY(20);
        stage.show();

        dprint("Added similarity grouping based on beliefs");
    }

}
