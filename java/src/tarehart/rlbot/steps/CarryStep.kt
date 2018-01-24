package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.awt.*
import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

/**
 * I don't think this was ever tested.
 */
class CarryStep : Step {

    override val situation: String
        get() = "Carrying"

    override fun getOutput(input: AgentInput): Optional<AgentOutput> {

        if (!canCarry(input, true)) {
            return Optional.empty()
        }

        val ballVelocityFlat = input.ballVelocity.flatten()
        val leadSeconds = .2

        val ballPath = ArenaModel.predictBallPath(input)

        val motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1)
        if (motionAfterWallBounce.isPresent && Duration.between(input.time, motionAfterWallBounce.get().time).seconds < 1) {
            return Optional.empty() // The dribble step is not in the business of wall reads.
        }

        val futureBallPosition: Vector2
        val (space) = ballPath.getMotionAt(input.time.plusSeconds(leadSeconds)).get()
        futureBallPosition = space.flatten()


        val scoreLocation = GoalUtil.getEnemyGoal(input.team).getNearestEntrance(input.ballPosition, 3.0).flatten()

        val ballToGoal = scoreLocation.minus(futureBallPosition)
        val pushDirection: Vector2
        val pressurePoint: Vector2
        val approachDistance = 1.0
        // TODO: vary the approachDistance based on whether the ball is forward / off to the side.

        val velocityCorrectionAngle = ballVelocityFlat.correctionAngle(ballToGoal)
        val angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * 2))
        pushDirection = VectorUtil.rotateVector(ballToGoal, angleTweak).normalized()
        pressurePoint = futureBallPosition.minus(pushDirection.scaled(approachDistance))


        val hurryUp = input.time.plusSeconds(leadSeconds)

        val dribble = SteerUtil.getThereOnTime(input.myCarData, SpaceTime(Vector3(pressurePoint.x, pressurePoint.y, 0.0), hurryUp))
        return Optional.of(dribble)
    }

    override fun canInterrupt(): Boolean {
        return true
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {
        private val MAX_X_DIFF = 1.3
        private val MAX_Y = 1.5
        private val MIN_Y = -0.9

        private fun positionInCarCoordinates(car: CarData, worldPosition: Vector3): Vector3 {
            // We will assume that the car is flat on the ground.

            // We will treat (0, 1) as the car's natural orientation.
            val carYaw = Vector2(0.0, 1.0).correctionAngle(car.orientation.noseVector.flatten())

            val carToPosition = worldPosition.minus(car.position).flatten()

            val (x, y) = VectorUtil.rotateVector(carToPosition, -carYaw)

            val zDiff = worldPosition.z - car.position.z
            return Vector3(x, y, zDiff)
        }

        private fun canCarry(input: AgentInput, log: Boolean): Boolean {

            val car = input.myCarData
            val (x, y, z) = positionInCarCoordinates(car, input.ballPosition)

            val xMag = Math.abs(x)
            if (xMag > MAX_X_DIFF) {
                if (log) {
                    println("Fell off the side", input.playerIndex)
                }
                return false
            }

            if (y > MAX_Y) {
                if (log) {
                    println("Fell off the front", input.playerIndex)
                }
                return false
            }

            if (y < MIN_Y) {
                if (log) {
                    println("Fell off the back", input.playerIndex)
                }
                return false
            }

            if (z > 3) {
                if (log) {
                    println("Ball too high to carry", input.playerIndex)
                }
                return false
            }

            if (z < 1) {
                if (log) {
                    println("Ball too low to carry", input.playerIndex)
                }
                return false
            }

            if (VectorUtil.flatDistance(car.velocity, input.ballVelocity) > 10) {
                if (log) {
                    println("Velocity too different to carry.", input.playerIndex)
                }
                return false
            }


            return true
        }
    }
}
