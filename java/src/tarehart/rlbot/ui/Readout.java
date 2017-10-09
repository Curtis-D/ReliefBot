package tarehart.rlbot.ui;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.tuning.BallPrediction;
import tarehart.rlbot.tuning.PredictionWarehouse;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class Readout {

    private static final int HEIGHT_BAR_MULTIPLIER = 10;

    private JLabel planPosture;
    private JProgressBar ballHeightActual;
    private JProgressBar ballHeightPredicted;
    private JSlider predictionTime;
    private JPanel rootPanel;
    private BallPredictionRadar ballPredictionReadout;
    private JProgressBar ballHeightActualMax;
    private JProgressBar ballHeightPredictedMax;
    private JTextPane situationText;
    private JLabel predictionTimeSeconds;
    private JTextArea logViewer;
    private JLabel blueCarPosX;
    private JLabel blueCarPosY;
    private JLabel blueCarPosZ;
    private JLabel orangeCarPosX;
    private JLabel orangeCarPosY;
    private JLabel orangeCarPosZ;

    private double maxCarSpeedVal;

    private LocalDateTime actualMaxTime = LocalDateTime.now();
    private LocalDateTime predictedMaxTime = LocalDateTime.now();

    private PredictionWarehouse warehouse = new PredictionWarehouse();

    private LocalDateTime previousTime = null;


    public Readout() {
        DefaultCaret caret = (DefaultCaret)logViewer.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void update(AgentInput input, Plan.Posture posture, String situation, String log, BallPath ballPath) {

        planPosture.setText(posture.name());
        situationText.setText(situation);
        predictionTimeSeconds.setText(String.format("%.2f", predictionTime.getValue() / 1000.0));
        // ballHeightPredicted.setValue(0); // Commented out to avoid flicker. Should always be fresh anyway.
        ballHeightActual.setValue((int) (input.ballPosition.z * HEIGHT_BAR_MULTIPLIER));
        logViewer.append(log);

        updateBallPredictionRadar(input, ballPath);
        updateBallHeightMaxes(input);
        updateCarPositionInfo(input);
    }

    private void updateBallHeightMaxes(AgentInput input) {
        // Calculate and display Ball Height Actual Max
        if (ballHeightActualMax.getValue() < ballHeightActual.getValue()) {
            ballHeightActualMax.setValue(ballHeightActual.getValue());
            actualMaxTime = input.time;
        } else if(Duration.between(input.time, actualMaxTime).abs().getSeconds() > 3) {
            ballHeightActualMax.setValue(0);
        }

        // Calculate and display Ball Height Predicted Max
        if (ballHeightPredictedMax.getValue() < ballHeightPredicted.getValue()) {
            ballHeightPredictedMax.setValue(ballHeightPredicted.getValue());
            predictedMaxTime = input.time;
        } else if(Duration.between(input.time, predictedMaxTime).abs().getSeconds() > 3) {
            ballHeightPredictedMax.setValue(0);
        }
    }

    private void updateBallPredictionRadar(AgentInput input, BallPath ballPath) {
        int predictionMillis = predictionTime.getValue();
        LocalDateTime predictionTime = input.time.plus(Duration.ofMillis(predictionMillis));

        if (previousTime == null || !previousTime.equals(input.time)) {
            if (ballPath != null) {
                Optional<SpaceTimeVelocity> predictionSpace = ballPath.getMotionAt(predictionTime);
                if (predictionSpace.isPresent()) {
                    BallPrediction prediction = new BallPrediction(predictionSpace.get().getSpace(), predictionTime);
                    warehouse.addPrediction(prediction);
                }
            }
        }

        Optional<BallPrediction> predictionOfNow = warehouse.getPredictionOfMoment(input.time);
        if (predictionOfNow.isPresent()) {
            Vector3 predictedLocation = predictionOfNow.get().predictedLocation;
            ballHeightPredicted.setValue((int) (predictedLocation.z * HEIGHT_BAR_MULTIPLIER));

            Vector3 predictionRelative = predictedLocation.minus(input.ballPosition);
            ballPredictionReadout.setPredictionCoordinates(new Vector2(predictionRelative.x, predictionRelative.y));
            ballPredictionReadout.setVelocity(new Vector2(input.ballVelocity.x, input.ballVelocity.y));
            ballPredictionReadout.repaint();
        }
    }

    private void updateCarPositionInfo(AgentInput input) {
        blueCarPosX.setText(String.format("%.2f", input.blueCar.position.x));
        blueCarPosY.setText(String.format("%.2f", input.blueCar.position.y));
        blueCarPosZ.setText(String.format("%.2f", input.blueCar.position.z));
        orangeCarPosX.setText(String.format("%.2f", input.orangeCar.position.x));
        orangeCarPosY.setText(String.format("%.2f", input.orangeCar.position.y));
        orangeCarPosZ.setText(String.format("%.2f", input.orangeCar.position.z));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
