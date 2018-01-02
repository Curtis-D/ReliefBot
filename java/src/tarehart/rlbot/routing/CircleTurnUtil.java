package tarehart.rlbot.routing;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Circle;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.time.GameTime;

import java.util.Optional;

public class CircleTurnUtil {
    public static SteerPlan planWithinCircle(CarData car, StrikePoint strikePoint, double currentSpeed) {

        Vector2 targetPosition = strikePoint.getPosition();
        Vector2 targetFacing = strikePoint.getFacing();
        Vector2 targetNose = targetPosition.plus(targetFacing);
        Vector2 targetTail = targetPosition.minus(targetFacing);
        Vector2 facing = car.getOrientation().getNoseVector().flatten();

        Vector2 flatPosition = car.getPosition().flatten();
        Circle idealCircle = Circle.Companion.getCircleFromPoints(targetTail, targetNose, flatPosition);

        boolean clockwise = Circle.Companion.isClockwise(idealCircle, targetPosition, targetFacing);

        Vector2 centerToMe = flatPosition.minus(idealCircle.getCenter());
        Vector2 idealDirection = VectorUtil.INSTANCE.orthogonal(centerToMe, v -> Circle.Companion.isClockwise(idealCircle, flatPosition, v) == clockwise).normalized();

//        if (facing.dotProduct(idealDirection) < .7) {
//            AgentOutput output = steerTowardGroundPosition(car, flatPosition.plus(idealDirection));
//            return new SteerPlan(output, targetPosition);
//        }

        Optional<Double> idealSpeedOption = getSpeedForRadius(idealCircle.getRadius());

        double idealSpeed = idealSpeedOption.orElse(10.0);

        double speedRatio = currentSpeed / idealSpeed; // Ideally should be 1

        double lookaheadRadians = Math.PI / 20;
        Vector2 centerToSteerTarget = VectorUtil.INSTANCE.rotateVector(flatPosition.minus(idealCircle.getCenter()), lookaheadRadians * (clockwise ? -1 : 1));
        Vector2 steerTarget = idealCircle.getCenter().plus(centerToSteerTarget);

        AgentOutput output = SteerUtil.steerTowardGroundPosition(car, steerTarget).withBoost(false).withSlide(false).withDeceleration(0).withAcceleration(1);

        if (speedRatio < 1) {
            output.withBoost(currentSpeed >= AccelerationModel.INSTANCE.getMEDIUM_SPEED() && speedRatio < .8 || speedRatio < .7);
        } else {
            int framesBetweenSlidePulses;
            if (speedRatio > 2) {
                framesBetweenSlidePulses = 3;
                output.withAcceleration(0);
            } else if (speedRatio > 1.5) {
                framesBetweenSlidePulses = 6;
            } else if (speedRatio > 1.2) {
                framesBetweenSlidePulses = 9;
            } else {
                framesBetweenSlidePulses = 12;
            }
            output.withSlide(car.getFrameCount() % (framesBetweenSlidePulses + 1) == 0);
        }

        return new SteerPlan(output, flatPosition, strikePoint, idealCircle, clockwise);
    }

    private static double getTurnRadius(double speed) {
        return SteerUtil.TURN_RADIUS_A * speed * speed + SteerUtil.TURN_RADIUS_B * speed + SteerUtil.TURN_RADIUS_C;
    }

    private static Optional<Double> getSpeedForRadius(double radius) {

        if (radius == SteerUtil.TURN_RADIUS_C) {
            return Optional.of(0d);
        }

        if (radius < SteerUtil.TURN_RADIUS_C) {
            return Optional.empty();
        }

        double a = SteerUtil.TURN_RADIUS_A;
        double b = SteerUtil.TURN_RADIUS_B;
        double c = SteerUtil.TURN_RADIUS_C - radius;

        double p = -b / (2 * a);
        double q = Math.sqrt(b * b - 4 * a * c) / (2 * a);
        return Optional.of(p + q);
    }

    public static double getFacingCorrectionSeconds(Vector2 approach, Vector2 targetFacing, double expectedSpeed) {

        double correction = approach.correctionAngle(targetFacing);
        return getTurnRadius(expectedSpeed) * Math.abs(correction) / expectedSpeed;
    }

    public static SteerPlan getPlanForCircleTurn(
            CarData car, DistancePlot distancePlot, StrikePoint strikePoint) {

        Vector2 targetPosition = strikePoint.getPosition();
        Vector2 targetFacing = strikePoint.getFacing();
        double distance = car.getPosition().flatten().distance(targetPosition);
        double maxSpeed = distancePlot.getMotionAfterDistance(distance)
                .map(DistanceTimeSpeed::getSpeed)
                .orElse(AccelerationModel.INSTANCE.getSUPERSONIC_SPEED());
        double idealSpeed = getIdealCircleSpeed(car, targetFacing);
        double currentSpeed = car.getVelocity().magnitude();

        return circleWaypoint(car, strikePoint, currentSpeed, Math.min(maxSpeed, idealSpeed));
    }

    private static double getIdealCircleSpeed(CarData car, Vector2 targetFacing) {
        double orientationCorrection = car.getOrientation().getNoseVector().flatten().correctionAngle(targetFacing);
        double angleAllowingFullSpeed = Math.PI / 6;
        double speedPenaltyPerRadian = 20;
        double rawPenalty = speedPenaltyPerRadian * (Math.abs(orientationCorrection) - angleAllowingFullSpeed);
        double correctionPenalty = Math.max(0, rawPenalty);
        return Math.max(15, AccelerationModel.INSTANCE.getSUPERSONIC_SPEED() - correctionPenalty);
    }

    private static SteerPlan circleWaypoint(CarData car, StrikePoint strikePoint, double currentSpeed, double expectedSpeed) {

        Vector2 targetPosition = strikePoint.getPosition();
        Vector2 targetFacing = strikePoint.getFacing();

        Vector2 flatPosition = car.getPosition().flatten();
        Vector2 toTarget = targetPosition.minus(flatPosition);

        boolean clockwise = toTarget.correctionAngle(targetFacing) < 0;

        double turnRadius = getTurnRadius(expectedSpeed);
        // Make sure the radius vector points from the target position to the center of the turn circle.
        Vector2 radiusVector = VectorUtil.INSTANCE.rotateVector(targetFacing, Math.PI / 2 * (clockwise ? -1 : 1)).scaled(turnRadius);

        Vector2 center = targetPosition.plus(radiusVector);
        double distanceFromCenter = flatPosition.distance(center);

        Vector2 centerToTangent = VectorUtil.INSTANCE.orthogonal(toTarget.scaledToMagnitude(turnRadius), v -> {
            Vector2 toCenter = center.minus(flatPosition);
            Vector2 toCandidate = center.plus(v).minus(flatPosition);
            return toCandidate.correctionAngle(toCenter) < 0 == clockwise;
        });

        Vector2 tangentPoint = center.plus(centerToTangent);

        if (distanceFromCenter < turnRadius) {

            if (currentSpeed < expectedSpeed) {
                return circleWaypoint(car, strikePoint, currentSpeed, currentSpeed);
            }

            return planWithinCircle(car, strikePoint, currentSpeed);
        }

        Vector2 toTangent = tangentPoint.minus(flatPosition);
        double facingCorrectionSeconds = getFacingCorrectionSeconds(toTangent, targetFacing, expectedSpeed);

        GameTime momentToStartTurning = strikePoint.getGameTime().minusSeconds(facingCorrectionSeconds);
        AgentOutput immediateSteer = SteerUtil.getThereOnTime(car, new SpaceTime(tangentPoint.toVector3(), momentToStartTurning));
        if (currentSpeed > expectedSpeed && toTangent.magnitude() < 20) {
            immediateSteer.withAcceleration(0).withDeceleration(1);
        }
        Circle circle = new Circle(center, turnRadius);
        return new SteerPlan(immediateSteer, tangentPoint, strikePoint, circle, Circle.Companion.isClockwise(circle, targetPosition, targetFacing));
    }

    public static SteerPlan getPlanForCircleTurn(CarData car, DistancePlot distancePlot, Vector2 flatten, Vector2 facing) {
        return getPlanForCircleTurn(car, distancePlot, new StrikePoint(flatten, facing, car.getTime()));
    }
}
