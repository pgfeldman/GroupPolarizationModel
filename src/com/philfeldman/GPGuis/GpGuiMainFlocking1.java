package com.philfeldman.GPGuis;

import com.philfeldman.GPCAs.BaseCA;
import com.philfeldman.GPCAs.FlockingBeliefCA;
import com.philfeldman.GPMain.FlockRecorder;
import com.philfeldman.GPMain.FlockingAgentManager;
import com.philfeldman.Graphics.ClusterCanvas;
import com.philfeldman.Graphics.FlockingClusterCanvas;
import com.philfeldman.utils.jfxGUI.WeightWidget;
import com.philfeldman.Philosophy.ParticleBelief;
import com.philfeldman.Utils.RunConfiguration;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Date;

import static com.philfeldman.utils.jfxGUI.Popups.*;

/**
 * Created by philip.feldman on 7/28/2016.
 */
public class GpGuiMainFlocking1 extends Application implements Dprintable{
    //----------------------------- inner classes

    private class Renderer extends AnimationTimer  {
        double sampleCounter = 0;

        @Override
        public void handle(long now) {
            ClusterCanvas cc = guiVars.canvas;
            cc.lprint(fam.getRecorderState());
            if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INIT){
                guiVars.setFromConfig(fam.getCurrentConfig());
            }
            fam.behavior();
            if(fam.getCurrentConfig().continouousClusterCalc){
                cc.setEpsScalar(fam.getCurrentConfig().clusterPercent);
                cc.calcClusters();
            }

            GraphicsContext gc = cc.getGraphicsContext2D();
            cc.identity();

            // background
            cc.clear(Paint.valueOf("white"), Paint.valueOf("#aaaaaa"));
            //cc.drawBackground(fam.getStoRet(), Paint.valueOf("blue"));

            cc.pushMatrix();
                cc.centerOrigin();
                cc.mouseTransforms();
                fam.getStoRet().draw(cc);
                cc.drawShapes();
            cc.popMatrix();
            if(fam.getCurrentConfig().debugLevel > 0) {
                cc.lshow(17);
            }else{
                cc.lclear();
            }

            if(fam.isSimRunning()) {
                if(fam.getCurrentConfig().recordingState && sampleCounter == 0){
                    fam.recordSample();
                }
                if(++sampleCounter > fam.getCurrentConfig().sampleIncrement){
                    sampleCounter = 0;
                }
                fam.getCurrentConfig().elapsed += RunConfiguration.DELTA_TIME;
            }
        }
    }

    private class GuiVars{
        final int PAD = 5;
        final int GAP = 5;
        final int MAX_WEIGHT = 10;
        Stage stage;
        Scene scene;
        MenuBar menuBar;
        Menu fileMenu;
        GridPane grid;
        TextArea consoleText;
        TextField nameTextField;
        TextField sampleTextField;
        TextField numAgentsTextField;
        TextField tailLengthTextField;
        TextField slewRateTextField;
        TextField slewRateVarianceTextField;
        TextField velocityTextField;
        TextField velocityVarianceTextField;
        TextField herdingWeightTextField;
        Button inFileButton;
        TextField outFolderTextField;
        TextField outSessionTextField;
        Button saveButton;
        CheckBox recordCheck;
        CheckBox calcContinuousCheck;
        CheckBox inverseHerdingCheck;
        WeightWidget redtoGreenRatioWidget;
        WeightWidget redExploitRadiusWidget;
        WeightWidget greenExploitRadiusWidget;
        WeightWidget attractionRadiusWidget;
        WeightWidget clusterPercentWidget;
        WeightWidget sourceScalarWidget;
        WeightWidget stageLimitWidget;
        RadioMenuItem noBorderItem;
        RadioMenuItem reflectBorderItem;
        RadioMenuItem respawnBorderItem;
        CheckMenuItem biasInteractionCheckItem;
        CheckMenuItem ignoreMisalignedCheckItem;
        ComboBox<FlockingAgentManager.HERDING_TYPE> herdingComboBox;
        ComboBox<FlockingBeliefCA.PARTICLE_TYPE> particleComboBox;
        ComboBox<Integer> dimensionsCombo;
        ComboBox<FlockRecorder.DATA_TYPE> measureTypeComboBox;

        FlockingClusterCanvas canvas;
        Renderer renderer = new Renderer();


        public GuiVars(){
            renderer = new Renderer();
        }

        public void setFromConfig(RunConfiguration config){
            noBorderItem.setSelected(false);
            reflectBorderItem.setSelected(false);
            respawnBorderItem.setSelected(false);
            switch(fam.getCurrentConfig().borderType){
                case NONE: noBorderItem.setSelected(true); break;
                case REFLECTIVE: reflectBorderItem.setSelected(true); break;
                case RESPAWN: respawnBorderItem.setSelected(true); break;
            }
            outFolderTextField.setText(fam.getCurrentConfig().getOutfolderName());
            nameTextField.setText(fam.getCurrentConfig().userName);
            outSessionTextField.setText(fam.getCurrentConfig().sessionName);
            numAgentsTextField.setText(Integer.toString(fam.getCurrentConfig().numAgents));
            tailLengthTextField.setText(Integer.toString(fam.getCurrentConfig().tailLength));
            biasInteractionCheckItem.setSelected(fam.getCurrentConfig().crossBiasInteraction);
            ignoreMisalignedCheckItem.setSelected((fam.getCurrentConfig().alignmentScalar >0));
            redtoGreenRatioWidget.setCurWeight(fam.getCurrentConfig().redGreenRatio);
            redExploitRadiusWidget.setCurWeight(fam.getCurrentConfig().redExploitRadius);
            greenExploitRadiusWidget.setCurWeight(fam.getCurrentConfig().greenExploitRadius);
            clusterPercentWidget.setCurWeight(fam.getCurrentConfig().clusterPercent);
            sourceScalarWidget.setCurWeight(fam.getCurrentConfig().sourceScalar);
            stageLimitWidget.setCurWeight(fam.getCurrentConfig().stageLimit);
            dimensionsCombo.setValue(fam.getCurrentConfig().dimensions);
            calcContinuousCheck.setSelected(fam.getCurrentConfig().continouousClusterCalc);
            sampleTextField.setText(Integer.toString(fam.getCurrentConfig().sampleIncrement));
            measureTypeComboBox.setValue(fam.getCurrentConfig().measureDataType);
            recordCheck.setSelected(fam.getCurrentConfig().recordingState);
            //particleComboBox.setValue(fam.getCurrentConfig().particleType);
            slewRateTextField.setText(Double.toString(fam.getCurrentConfig().slewRateMean));
            slewRateVarianceTextField.setText(Double.toString(fam.getCurrentConfig().slewRateVariance));
            velocityTextField.setText(Double.toString(fam.getCurrentConfig().velocityMean));
            velocityVarianceTextField.setText(Double.toString(fam.getCurrentConfig().velocityVariance));
            herdingComboBox.setValue(fam.getCurrentConfig().herdingType);
            inverseHerdingCheck.setSelected(fam.getCurrentConfig().inverseHerding);
            herdingWeightTextField.setText(Double.toString(fam.getCurrentConfig().herdingWeight));
        }

        private void buildTop(BorderPane border){
            MenuBar menuBar = new MenuBar();
            Menu menuFile = new Menu("File");
            MenuItem outputFolderItem = new MenuItem("Set output folder");
            outputFolderItem.setOnAction(
                    (final ActionEvent e) -> {
                        String filename  = chooseDirectory(fam.getCurrentConfig().getOutfolderName(), stage);
                        if (filename != null) {
                            outFolderTextField.setText(filename);
                            Tooltip tt = new Tooltip(filename);
                            outFolderTextField.setTooltip(tt);
                            fam.getCurrentConfig().setOutfolderName(filename);
                            System.setProperty("user.dir", filename);
                        }
                    });

            MenuItem embedFileItem = new MenuItem("Load embedding XML");
            embedFileItem.setOnAction(
                    (final ActionEvent e) -> {
                        File file = chooseFile(fam.getCurrentConfig().getOutfolderName(), stage);

                        if (file != null) {
                            fam.loadEmbed(file);
                            if(fam.getCurrentEmbeddings().loadSuccess) {
                                dprint("successfully opened "+file.getName());
                                String p = file.getParent();
                                System.setProperty("user.dir", p);
                            }else{
                                dprint("error opening "+file.getName());
                            }
                        }
                    });

            MenuItem configFileItem = new MenuItem("Load config XML");
            configFileItem.setOnAction(
                    (final ActionEvent e) -> {
                        File file = chooseFile(fam.getCurrentConfig().getOutfolderName(), stage);

                        if (file != null) {
                            fam.loadConfig(file);
                            if(fam.getCurrentConfig().loadSuccess) {
                                dprint("successfully opened "+file.getName());
                                setFromConfig(fam.getCurrentConfig());
                                String p = file.getParent();
                                System.setProperty("user.dir", p);
                            }else{
                                dprint("error opening "+file.getName());
                            }
                        }
                    });

            MenuItem influenceSaveItem = new MenuItem("Save influence XML");
            influenceSaveItem.setOnAction(event -> {
                boolean result = saveInfluenceXML();
                if(result) {
                    infoAlert("Saved "+fam.getCurrentConfig().getOutFileName(RunConfiguration.XML_POSTFIX));
                }else{
                    infoAlert("Error saving file");
                }
            });

            MenuItem arffSaveItem = new MenuItem("Save .arff");
            arffSaveItem.setOnAction(event -> {
                boolean result = saveArff(measureTypeComboBox.getValue());
                if(result) {
                    infoAlert("Saved " + fam.getCurrentConfig().getOutFileName(RunConfiguration.ARFF_POSTFIX));
                }else{
                    infoAlert("Error saving file");
                }
            });

            MenuItem xlsxSaveItem = new MenuItem("Save .xlsx");
            xlsxSaveItem.setOnAction(event -> {
                boolean result = saveExcel();
                if(result) {
                    infoAlert("Saved "+fam.getCurrentConfig().getOutFileName(RunConfiguration.XLSX_POSTFIX));
                }else{
                    infoAlert("Error saving file");
                }
            });

            menuFile.getItems().addAll(outputFolderItem, configFileItem, embedFileItem, influenceSaveItem, xlsxSaveItem, arffSaveItem);


            Menu menuBorders = new Menu("Borders");
            ToggleGroup tGroup = new ToggleGroup();
            noBorderItem = new RadioMenuItem("None");
            noBorderItem.setToggleGroup(tGroup);
            noBorderItem.setOnAction(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().borderType = ParticleBelief.BORDER_TYPE.NONE;
                    fam.setBorderType(ParticleBelief.BORDER_TYPE.NONE);
                }
            });
            reflectBorderItem = new RadioMenuItem("Reflect");
            reflectBorderItem.setToggleGroup(tGroup);
            reflectBorderItem.setOnAction(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().borderType = ParticleBelief.BORDER_TYPE.REFLECTIVE;
                    fam.setBorderType(ParticleBelief.BORDER_TYPE.REFLECTIVE);
                }
            });
            respawnBorderItem = new RadioMenuItem("Respawn");
            respawnBorderItem.setToggleGroup(tGroup);
            respawnBorderItem.setOnAction(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().borderType = ParticleBelief.BORDER_TYPE.RESPAWN;
                    fam.setBorderType(ParticleBelief.BORDER_TYPE.RESPAWN);
                }
            });
            // lethalBorderItem = new CheckMenuItem("Lethal");
            // forceBorderItem = new CheckMenuItem("RestoringForce");
            // torusBorderItem = new CheckMenuItem("RestoringForce");
            reflectBorderItem.setSelected(true);
            menuBorders.getItems().addAll(noBorderItem, reflectBorderItem, respawnBorderItem);

            Menu menuPopulations = new Menu("Populations");

            biasInteractionCheckItem = new CheckMenuItem("Allow Interaction");
            biasInteractionCheckItem.setSelected(fam.getCurrentConfig().crossBiasInteraction);
            biasInteractionCheckItem.setOnAction(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().crossBiasInteraction = biasInteractionCheckItem.isSelected();
                }
            });

            ignoreMisalignedCheckItem = new CheckMenuItem("Ignore Misaligned");
            ignoreMisalignedCheckItem.setSelected(fam.getCurrentConfig().alignmentScalar > 0);
            ignoreMisalignedCheckItem.setOnAction(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    if(ignoreMisalignedCheckItem.isSelected()){
                        fam.getCurrentConfig().alignmentScalar = 1.0;
                    } else{
                        fam.getCurrentConfig().alignmentScalar = 0;
                    }
                }
            });

            menuPopulations.getItems().addAll(biasInteractionCheckItem, ignoreMisalignedCheckItem);

            Menu menuDebug = new Menu("Debug");
            ToggleGroup dGroup = new ToggleGroup();
            RadioMenuItem rItem = new RadioMenuItem("None");
            rItem.setOnAction(event -> {
                fam.getCurrentConfig().debugLevel = 0;
                canvas.setDebugLevel(0);
            });
            rItem.setToggleGroup(dGroup);
            menuDebug.getItems().add(rItem);

            for(int i = 1; i < 5; ++i){
                final int val = i;
                rItem = new RadioMenuItem("Level "+val);
                menuDebug.getItems().add(rItem);
                rItem.setToggleGroup(dGroup);
                rItem.setOnAction(event -> {
                    fam.getCurrentConfig().debugLevel = val;
                    canvas.setDebugLevel(val);
                });
            }

            menuBar.getMenus().addAll(menuFile, menuBorders, menuPopulations, menuDebug);
            border.setTop(menuBar);
        }

        private void buildLeft(VBox leftBox) {
            grid = new GridPane();
            grid.setHgap(GAP);
            grid.setVgap(GAP);
            int rowIndex = 0;

            grid.add(new Label("Name: "), 0, rowIndex);
            nameTextField = new TextField();
            nameTextField.setText(System.getProperty("user.name"));
            grid.add(nameTextField, 1, rowIndex++);

            grid.add(new Label("Output folder:"), 0,rowIndex);
            outFolderTextField = new TextField();
            outFolderTextField.setText(fam.getCurrentConfig().getOutfolderName());
            grid.add(outFolderTextField, 1, rowIndex++);

            grid.add(new Label("Session Name:"), 0, rowIndex);
            outSessionTextField = new TextField(System.getProperty("user.name")+"-GP");
            outSessionTextField.textProperty().addListener(event -> {
                fam.getCurrentConfig().sessionName = outSessionTextField.getText();
            });
            grid.add(outSessionTextField, 1, rowIndex++);

            grid.add(new Label("Num Agents:"), 0, rowIndex);
            numAgentsTextField = new TextField(Integer.toString(fam.getCurrentConfig().numAgents));
            numAgentsTextField.textProperty().addListener(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    try {
                        fam.getCurrentConfig().numAgents = Integer.parseInt(numAgentsTextField.getText());
                    } catch (NumberFormatException e) {}
                }
            });
            grid.add(numAgentsTextField, 1, rowIndex++);

            grid.add(new Label("Tail Length:"), 0, rowIndex);
            tailLengthTextField = new TextField(Integer.toString(fam.getCurrentConfig().tailLength));
            tailLengthTextField.textProperty().addListener(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    try {
                        fam.getCurrentConfig().tailLength = Integer.parseInt(tailLengthTextField.getText());
                    } catch (NumberFormatException e) {}
                }
            });
            grid.add(tailLengthTextField, 1, rowIndex++);

            HBox hBox = new HBox();
            hBox.setSpacing(GAP);
            hBox.setAlignment(Pos.CENTER_LEFT);
            slewRateTextField = new TextField(Double.toString(fam.getCurrentConfig().slewRateMean));
            slewRateVarianceTextField = new TextField(Double.toString(fam.getCurrentConfig().slewRateVariance));
            slewRateTextField.setPrefWidth(50);
            slewRateVarianceTextField.setPrefWidth(50);
            slewRateTextField.textProperty().addListener(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    try {
                        fam.getCurrentConfig().slewRateMean = Double.parseDouble(slewRateTextField.getText());
                    } catch (NumberFormatException e) {}
                }
            });
            slewRateVarianceTextField.textProperty().addListener(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    try {
                        fam.getCurrentConfig().slewRateVariance = Double.parseDouble(slewRateVarianceTextField.getText());
                    } catch (NumberFormatException e) {}
                }
            });
            hBox.getChildren().addAll(slewRateTextField, new Label("Mean"), slewRateVarianceTextField, new Label("Variance"));
            grid.add(new Label("Slew Rate (deg):"), 0, rowIndex);
            grid.add(hBox, 1, rowIndex++);

            hBox = new HBox();
            hBox.setSpacing(GAP);
            hBox.setAlignment(Pos.CENTER_LEFT);
            velocityTextField = new TextField(Double.toString(fam.getCurrentConfig().velocityMean));
            velocityVarianceTextField = new TextField(Double.toString(fam.getCurrentConfig().velocityVariance));
            velocityTextField.setPrefWidth(50);
            velocityVarianceTextField.setPrefWidth(50);
            velocityTextField.textProperty().addListener(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    try {
                        fam.getCurrentConfig().velocityMean = Double.parseDouble(velocityTextField.getText());
                    } catch (NumberFormatException e) {}
                }
            });
            velocityVarianceTextField.textProperty().addListener(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    try {
                        fam.getCurrentConfig().velocityVariance = Double.parseDouble(velocityVarianceTextField.getText());
                    } catch (NumberFormatException e) {}
                }
            });
            hBox.getChildren().addAll(velocityTextField, new Label("Mean"), velocityVarianceTextField, new Label("Variance"));
            grid.add(new Label("Velocity:"), 0, rowIndex);
            grid.add(hBox, 1, rowIndex++);

            hBox = new HBox();
            hBox.setSpacing(GAP);
            hBox.setAlignment(Pos.CENTER_LEFT);

            herdingWeightTextField = new TextField(Double.toString(fam.getCurrentConfig().herdingWeight));
            herdingWeightTextField.setPrefWidth(50);
            herdingWeightTextField.textProperty().addListener(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    try {
                        fam.getCurrentConfig().herdingWeight = Double.parseDouble(herdingWeightTextField.getText());
                    } catch (NumberFormatException e) {}
                }
            });

            inverseHerdingCheck = new CheckBox("Invert");
            inverseHerdingCheck.setSelected(fam.getCurrentConfig().inverseHerding);
            inverseHerdingCheck.setOnAction(event -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().inverseHerding = inverseHerdingCheck.isSelected();
                }
            });

            hBox.getChildren().addAll(inverseHerdingCheck, herdingWeightTextField, new Label("Weight"));

            ObservableList<FlockingAgentManager.HERDING_TYPE> herdTypeList = FXCollections.observableArrayList();
            herdTypeList.add(FlockingAgentManager.HERDING_TYPE.NO_HERDING);
            herdTypeList.add(FlockingAgentManager.HERDING_TYPE.AVERAGE_HEADING);
            herdTypeList.add(FlockingAgentManager.HERDING_TYPE.RANDOM_AGENT);
            herdTypeList.add(FlockingAgentManager.HERDING_TYPE.RANDOM_AGENTS);
            herdingComboBox = new ComboBox<>(herdTypeList);
            herdingComboBox.setValue(FlockingAgentManager.HERDING_TYPE.NO_HERDING);
            herdingComboBox.setOnAction(event -> {
                //warnAlert("Disabled - update code in FlockingBeliefCA.axisMove()");
                //currentfam.getCurrentConfig().borderType = herdingComboBox.getValue();
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().herdingType = herdingComboBox.getValue();
                }
            });
            //grid.add(new Label("Herding:"), 0, 7);
            grid.add(herdingComboBox, 0, rowIndex);
            grid.add(hBox, 1, rowIndex++);

            leftBox.getChildren().addAll(grid, new Separator());


            redtoGreenRatioWidget = new WeightWidget("Red to Green Ratio", 1.0, WeightWidget.MAPPING.LINEAR);
            redtoGreenRatioWidget.addActionListener(e -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().redGreenRatio = redtoGreenRatioWidget.getCurWeight();
                }
            });
            redtoGreenRatioWidget.setCurWeight(fam.getCurrentConfig().redGreenRatio);
            leftBox.getChildren().addAll(redtoGreenRatioWidget, new Separator());

            redExploitRadiusWidget = new WeightWidget("Red Exploit Radius");
            redExploitRadiusWidget.addActionListener(e -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().redExploitRadius = redExploitRadiusWidget.getCurWeight();
                }
                fam.setInfluenceRadius(fam.getCurrentConfig().redExploitRadius, BaseCA.BIAS.RED_BIAS);
            });
            redExploitRadiusWidget.setCurWeight(fam.getCurrentConfig().redExploitRadius);
            leftBox.getChildren().addAll(redExploitRadiusWidget, new Separator());

            greenExploitRadiusWidget = new WeightWidget("Green Exploit Radius");
            greenExploitRadiusWidget.addActionListener(e -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().greenExploitRadius = greenExploitRadiusWidget.getCurWeight();
                }
                fam.setInfluenceRadius(fam.getCurrentConfig().greenExploitRadius, BaseCA.BIAS.GREEN_BIAS);
            });
            greenExploitRadiusWidget.setCurWeight(fam.getCurrentConfig().greenExploitRadius);
            leftBox.getChildren().addAll(greenExploitRadiusWidget, new Separator());

            attractionRadiusWidget = new WeightWidget("Attraction Radius");
            attractionRadiusWidget.addActionListener(e -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().attractionRadius = attractionRadiusWidget.getCurWeight();
                }
            });
            attractionRadiusWidget.setCurWeight(fam.getCurrentConfig().attractionRadius);
            leftBox.getChildren().addAll(attractionRadiusWidget, new Separator());

            clusterPercentWidget = new WeightWidget("Cluster EPS", 1.0);
            clusterPercentWidget.addActionListener(e -> {
                //canvas.setEpsScalar(clusterPercentWidget.getCurWeight());
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().clusterPercent = clusterPercentWidget.getCurWeight();
                }
            });
            clusterPercentWidget.setCurWeight(fam.getCurrentConfig().clusterPercent);
            leftBox.getChildren().addAll(clusterPercentWidget, new Separator());

            sourceScalarWidget = new WeightWidget("Source Scalar");
            sourceScalarWidget.addActionListener(e -> {
                //canvas.setEpsScalar(sourceScalarWidget.getCurWeight());
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().sourceScalar = sourceScalarWidget.getCurWeight();
                }
            });
            sourceScalarWidget.setCurWeight(fam.getCurrentConfig().sourceScalar);
            leftBox.getChildren().addAll(sourceScalarWidget, new Separator());

            stageLimitWidget = new WeightWidget("Stage Limit");
            stageLimitWidget.addActionListener(e -> {
                if(fam.getBatchState() == FlockingAgentManager.BATCH_STATE.INTERACTIVE) {
                    fam.getCurrentConfig().stageLimit = stageLimitWidget.getCurWeight();
                }
            });
            stageLimitWidget.setCurWeight(fam.getCurrentConfig().stageLimit);
            leftBox.getChildren().addAll(stageLimitWidget, new Separator());

            consoleText = new TextArea();
            consoleText.setPrefWidth(300);
            leftBox.getChildren().addAll(consoleText);
            VBox.setVgrow(consoleText, Priority.ALWAYS);
        }

        private void buildCenter(VBox centerVbox){
            HBox sessionHBox = new HBox();
            //sessionHBox.setPadding(padding);
            sessionHBox.setPadding(new Insets(0, PAD, PAD, PAD));
            sessionHBox.setSpacing(GAP);
            buildSessionHbox(sessionHBox);
            centerVbox.getChildren().add(sessionHBox);
            //centerVbox.setStyle("-fx-background-color: green");
            AnchorPane ap = new AnchorPane();
            //ap.setStyle("-fx-background-color: blue");

            centerVbox.getChildren().add(ap);
            VBox.setVgrow(ap, Priority.ALWAYS);

            canvas = new FlockingClusterCanvas( 100, 100 );
            AnchorPane.setTopAnchor(canvas, 0.0);
            AnchorPane.setLeftAnchor(canvas, 0.0);
            AnchorPane.setRightAnchor(canvas, 0.0);
            AnchorPane.setBottomAnchor(canvas, 0.0);
            ap.getChildren().add(canvas);
        }

        private void buildSessionHbox(HBox bottomHbox) {
            HBox leftBox = new HBox();
            leftBox.setSpacing(GAP);
            leftBox.setAlignment(Pos.CENTER_LEFT);

            Button clearConsoleButton = new Button("Clear Console");
            clearConsoleButton.setOnAction(event -> {
                consoleText.setText("");
            });

            ObservableList<Integer> dimensionList = FXCollections.observableArrayList();
            for(int i = 2; i < 11; ++i){
                dimensionList.add(i);
            }

            dimensionsCombo = new ComboBox<Integer>(dimensionList);
            dimensionsCombo.setValue(2);

            Button resetButton = new Button("Init");
            resetButton.setOnAction(event -> {
                fam.getCurrentConfig().redGreenRatio = redtoGreenRatioWidget.getCurWeight();
                fam.getCurrentConfig().dimensions = dimensionsCombo.getValue();
                fam.getCurrentConfig().sessionName = outSessionTextField.getText();
                fam.getCurrentConfig().userName = nameTextField.getText();
                fam.getCurrentConfig().stageLimit = stageLimitWidget.getCurWeight();
                fam.getCurrentConfig().date = new Date();
                if(noBorderItem.isSelected()){
                    fam.getCurrentConfig().borderType = ParticleBelief.BORDER_TYPE.NONE;
                }else if(reflectBorderItem.isSelected()){
                    fam.getCurrentConfig().borderType = ParticleBelief.BORDER_TYPE.REFLECTIVE;
                }else{
                    fam.getCurrentConfig().borderType = ParticleBelief.BORDER_TYPE.RESPAWN;
                }



                fam.initializeAgents();
                fam.initializeStorage();
                greenExploitRadiusWidget.setCurWeight(fam.getCurrentConfig().greenExploitRadius);
                redExploitRadiusWidget.setCurWeight(fam.getCurrentConfig().redExploitRadius);
                stageLimitWidget.setCurWeight(fam.getCurrentConfig().stageLimit);

                if(recordCheck.isSelected()) {
                    System.out.println("Resetting recorder");
                    fam.configRecorder();
                }

                fam.getCurrentConfig().elapsed = 0;
            });
            Button startButton = new Button("Start");
            startButton.setOnAction(event -> {
                boolean state = !fam.isSimRunning();
                fam.setSimRunning(state);
                if(state){
                    startButton.setText("Stop");
                    infoAlert(fam.getCurrentConfig().toString());
                }else{
                    startButton.setText("Start");
                }
            });

            calcContinuousCheck = new CheckBox("Continuous");
            Button clusterButton = new Button("Calc Clusters");
            clusterButton.setOnAction(event -> {
                fam.getCurrentConfig().continouousClusterCalc = calcContinuousCheck.isSelected();

                if(fam.getCurrentConfig().continouousClusterCalc) {
                    canvas.calcClusters();
                }
            });

            leftBox.getChildren().addAll(clearConsoleButton, new Label("Dimensions:"), dimensionsCombo, resetButton, startButton, clusterButton, calcContinuousCheck);

            HBox rightBox = new HBox();
            rightBox.setSpacing(5);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            sampleTextField = new TextField(String.valueOf(fam.getCurrentConfig().sampleIncrement));
            sampleTextField.setPrefWidth(50);
            sampleTextField.setOnAction(event -> {
                try{
                    fam.getCurrentConfig().sampleIncrement = Integer.parseInt(sampleTextField.getText());
                }catch(NumberFormatException e){
                    infoAlert("Unable to parse '"+sampleTextField.getText()+"' as an Integer");
                }
            });
            recordCheck = new CheckBox("Record");
            recordCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if(recordCheck.isSelected() && fam.getCurrentConfig().getOutfolderName().length() < 3){
                    warnAlert("You need to set the output folder");
                    recordCheck.setSelected(false);
                    return;
                }
                fam.getCurrentConfig().recordingState = recordCheck.isSelected();
                fam.configRecorder();
            });
            ObservableList<FlockRecorder.DATA_TYPE> typeList = FXCollections.observableArrayList();
            for(FlockRecorder.DATA_TYPE type : FlockRecorder.DATA_TYPE.values()){
                typeList.add(type);
            }
            measureTypeComboBox = new ComboBox<>(typeList);
            measureTypeComboBox.setValue(fam.getCurrentConfig().measureDataType);
            measureTypeComboBox.setPrefWidth(150);

            rightBox.getChildren().addAll(new Label("Sampling:"), sampleTextField, recordCheck, measureTypeComboBox);

            bottomHbox.getChildren().addAll(leftBox, rightBox);
            bottomHbox.setAlignment(Pos.CENTER_RIGHT);

            HBox.setHgrow(leftBox, Priority.ALWAYS);
        }

        private void buildShapes(){
            fam.setCanvas(canvas);
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

            buildTop(border);

            buildShapes();

            scene = new Scene(border, 1250, 900);
            stage.setScene(scene);

            renderer.start();
        }


    }

    //----------------------------------- Global variables
    protected GuiVars guiVars;
    protected FlockingAgentManager fam;


    //----------------------------------- Constructors
    public GpGuiMainFlocking1(){
        //cm = new CorpusManager();

        guiVars = new GuiVars();
        fam = new FlockingAgentManager();
        fam.setDprinter(this);
    }

    //----------------------------------- Methods

    public void dprint(String str){
        guiVars.consoleText.insertText(0, str+"\n");
    }

    protected boolean saveArff(FlockRecorder.DATA_TYPE type){
        String outfolderName = guiVars.outFolderTextField.getText();
        if(outfolderName.length() < 3){
            warnAlert("You need to set the output folder");
            return false;
        }
        fam.getCurrentConfig().sessionName = guiVars.outSessionTextField.getText();
        fam.toArff(type);

        return true;
    }

    protected boolean saveInfluenceXML(){
        String outfolderName = guiVars.outFolderTextField.getText();
        if(outfolderName.length() < 3){
            warnAlert("You need to set the output folder");
            return false;
        }
        String filename = fam.getCurrentConfig().getOutFileName("INFLUENCE", RunConfiguration.XML_POSTFIX);
        fam.toInfluenceXML(filename);

        return true;
    }

    protected boolean saveExcel(){
        String outfolderName = guiVars.outFolderTextField.getText();
        if(outfolderName.length() < 3){
            warnAlert("You need to set the output folder");
            return false;
        }

//        SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yy-kk_mm_ss");
//        Date d = new Date();
//        String outfile = outfolderName+"/"+guiVars.outSessionTextField.getText()+"_"+sdf.format(d)+".xlsx";
        String outfile = fam.getCurrentConfig().getOutFileName(RunConfiguration.XLSX_POSTFIX);
        infoAlert(String.format("Saving spreadsheet %s", outfile));
        fam.toExcel(outfile);

        return true;
    }

    @Override
    public void start(Stage stage) throws Exception {
        guiVars.buildMainScreen(stage);

        guiVars.canvas.setDprinter(this);

        stage.setTitle("Group Polarization Model (5.13.19)");

        stage.setX(10);
        stage.setY(10);
        stage.show();

        dprint("12.26.16 - Started flocking GUI");
        dprint("12.27.16 - Multidimensional Boids model");
        dprint("12.28.16 - High-dimensional line intersection");
        dprint("12.29.16 - Interpolate heading and speed adjust");
        dprint("12.30.16 - Influence Distance");
        dprint("1.2.17 - Clustering");
        dprint("1.3.17, 1.5.17 - File loading and data output");
        dprint("1.13.17 - Initial data generation");
        dprint("1.15.17 - Double buffered interaction calculations");
        dprint("1.16.17 - Added 'samples' indicator");
        dprint("1.17.17 - Added CA state of ACTIVE, INACTIVE and DORMANT");
        dprint("1.18.17 - Wired RunConfig so that border behavior is supported");
        dprint("1.18.17 - Added mean angle distance");
        dprint("1.20.17 - Added load from file - one so far, working on multiple");
        dprint("1.24.17 - Added batch files");
        dprint("2.1.17 - Set agent number and tweaked flocking code");
        dprint("2.2.17 - Adding agent type (Boid/Particle)");
        dprint("2.9.17 - Finished particle behavior");
        dprint("2.10.17 - Adding configurable speed and heading, fixed ARFF time series output");
        dprint("2.13.17 - Integrated ARFF time formatting");
        dprint("2.14.17 - Added 'delta' output to ARFF and Excel. Fixed scientific notation bug for ARFF");
        dprint("2.16.17, 2.17,17 - Code cleanup");
        dprint("2.20.17 - Added more position deltas");
        dprint("2.21.17 - Added Dynamic Time Warping");
        dprint("6.15.17 - Enabled NONE border option");
        dprint("6.16.17 - Enabled RESPAWN border option");
        dprint("8.28.17, 9.5.17 - Added CellAccumulator storage, retrieval and drawing. Initial use is heatmap");
        dprint("9.13.17 - Started resistance boundaries");
        dprint("9.14.17 - Changed Social Horizon Radius to Exploit Radius");
        dprint("10.2.17, 10.3.17 - Added heading difference to average flock to recording");
        dprint("10.27.17 - Added adversarial herding");
        dprint("11.2.17 - Added inverse herding");
        dprint("11.3.17 - Added herding vars to Excel output");
        dprint("11.5.17 - Added multiple herding options (multi agent, single agent, average center");
        dprint("11.6.17 - Bug fixes to support batch recording of new features");
        dprint("11.7.17 - Set average heading agent weight to be the specified weight");
        dprint("11.16.17 - Added cell trajectories");
        dprint("12.12.17 - added speed influence scalar for herders");
        dprint("12.14.17 - added header to trajectory spreadsheet and named all the cells 'cell_'");
        dprint("12.19.17 - fixed heading recording calculation error");
        dprint("2.9.18 - Moved WeightWidget to JavaUtils");
        dprint("5.15.18, 5.17.18 - Adding menus, embedding space");
        dprint("6.18.18 - Fixed bug in cell recorder");
        dprint("6.21.18 - Added attraction value when agents are in sources, increased app size");
        dprint("6.22.18 - Added border actions to menu, started influence recording");
        dprint("6.25.18 - Adding influence recording, restructured GUI layout");
        dprint("6.26.18 - Fixed a bug that prevented population interactions, continued GUI tweaking");
        dprint("6.27.18 - Fixed an XML output bug. Unbalanced sample tag");
        dprint("7.18.18 - Added cell names to the XML influence output to aid in tagging");
        dprint("7.19.18 - Fixed a missing CR in the output of the <cell> tag, and fixed some file management issues");
        dprint("8.8.18 - Added borders to a toggle group");
        dprint("8.10.18 - Added live debugging support");
        dprint("8.20.18 - Added 'ignore misaligned' population option");
        dprint("3.2.19 - Added variable length 'tail' of cell names");
        dprint("3.18.19 - Added NONE filetype to config file reader for demos");
        dprint("5.13.19 - Maintained debug state across batches");

    }

}
