package tarehart.rlbot.physics;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;


public class ArenaModel {

    public static final float SIDE_WALL = 74;
    public static final float BACK_WALL = 100;
    public static final float CEILING = 40;

    private static final int WALL_THICKNESS = 10;
    private static final int WALL_LENGTH = 200;

    private DynamicsWorld world;
    private RigidBody ball;


    public ArenaModel() {
        world = initPhysics();
        setupWalls();
        ball = initBallPhysics();
        world.addRigidBody(ball);
    }

    private void setupWalls() {
        addWallToWorld(new StaticPlaneShape(new Vector3f(0, 0, 1), 0));
        addWallToWorld(new StaticPlaneShape(new Vector3f(0, 1, 0), -BACK_WALL));
        addWallToWorld(new StaticPlaneShape(new Vector3f(0, -1, 0), BACK_WALL));
        addWallToWorld(new StaticPlaneShape(new Vector3f(1, 0, 0), -SIDE_WALL));
        addWallToWorld(new StaticPlaneShape(new Vector3f(-1, 0, 0), SIDE_WALL));
        addWallToWorld(new StaticPlaneShape(new Vector3f(0, 0, -1), CEILING));
    }

    private int convertNormal(float norm) {
        return norm == 0 ? WALL_LENGTH / 2 : WALL_THICKNESS / 2;
    }

    private void addWallToWorld(StaticPlaneShape plane) {

        Vector3f normal = new Vector3f();
        plane.getPlaneNormal(normal);

        CollisionShape boxGround = new BoxShape(new Vector3f(convertNormal(normal.x), convertNormal(normal.y), convertNormal(normal.z)));


        Transform wallTransform = new Transform();
        wallTransform.setIdentity();

        Vector3f origin = new Vector3f();
        origin.scale(-(plane.getPlaneConstant() + WALL_THICKNESS / 2), normal);
        wallTransform.origin.set(origin);

        DefaultMotionState myMotionState = new DefaultMotionState(wallTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
                0, myMotionState, boxGround, new Vector3f());
        RigidBody wall = new RigidBody(rbInfo);
        wall.setRestitution(1);
        world.addRigidBody(wall);
    }

    private Vector3f getBallPosition() {
        Transform trans = new Transform();
        ball.getMotionState().getWorldTransform(trans);
        return trans.origin;
    }

    public Vector3f simulateBall(Vector3f position, Vector3f velocity, float seconds) {
        ball.clearForces();
        ball.setLinearVelocity(velocity);
        Transform ballTransform = new Transform();
        ballTransform.setIdentity();
        ballTransform.origin.set(position);
        ball.setWorldTransform(ballTransform);
        ball.getMotionState().setWorldTransform(ballTransform);

        int stepsPerSecond = 10;

        // Do some simulation
        for (int i = 0; i < seconds * stepsPerSecond; i++) {
            world.stepSimulation(1.0f / stepsPerSecond, 10);
        }
        return getBallPosition();
    }

    private DynamicsWorld initPhysics() {
        // collision configuration contains default setup for memory, collision
        // setup. Advanced users can create their own configuration.
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();

        // use the default collision dispatcher. For parallel processing you
        // can use a diffent dispatcher (see Extras/BulletMultiThreaded)
        CollisionDispatcher dispatcher = new CollisionDispatcher(
                collisionConfiguration);

        // the maximum size of the collision world. Make sure objects stay
        // within these boundaries
        // Don't make the world AABB size too large, it will harm simulation
        // quality and performance
        Vector3f worldAabbMin = new Vector3f(-200, -200, -200);
        Vector3f worldAabbMax = new Vector3f(200, 200, 200);
        int maxProxies = 1024;
        AxisSweep3 overlappingPairCache =
                new AxisSweep3(worldAabbMin, worldAabbMax, maxProxies);

        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        DiscreteDynamicsWorld dynamicsWorld = new DiscreteDynamicsWorld(
                dispatcher, overlappingPairCache, solver,
                collisionConfiguration);

        dynamicsWorld.setGravity(new Vector3f(0, 0, -10));

        return dynamicsWorld;
    }

    private RigidBody initBallPhysics() {
        SphereShape collisionShape = new SphereShape(1.8555f);

        // Create Dynamic Objects
        Transform startTransform = new Transform();
        startTransform.setIdentity();

        float mass = 1f;

        Vector3f localInertia = new Vector3f(0, 0, 0);
        collisionShape.calculateLocalInertia(mass, localInertia);

        startTransform.origin.set(new Vector3f(0, 0, 0));

        // using motionstate is recommended, it provides
        // interpolation capabilities, and only synchronizes
        // 'active' objects
        DefaultMotionState myMotionState = new DefaultMotionState(startTransform);

        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
                mass, myMotionState, collisionShape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        body.setDamping(.07f, 0.5f);
        body.setRestitution(1);

        return body;
    }

}