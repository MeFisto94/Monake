package com.jmonkeyengine.monake.sim;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jmonkeyengine.monake.es.BodyPosition;
import com.jmonkeyengine.monake.es.ObjectType;
import com.jmonkeyengine.monake.es.ShapeInfos;
import com.jmonkeyengine.monake.es.components.ActiveWeaponComponent;
import com.jmonkeyengine.monake.es.components.AmmoShotgunComponent;
import com.jmonkeyengine.monake.es.components.HealthComponent;
import com.jmonkeyengine.monake.util.server.ServerApplication;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.ethereal.EtherealHost;
import com.simsilica.ethereal.TimeSource;
import com.simsilica.mathd.trans.PositionTransition;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import java.util.HashMap;
import static java.util.Objects.isNull;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShootingSystem extends AbstractGameSystem {

    static Logger log = LoggerFactory.getLogger(ShootingSystem.class);
    EntityData ed;
    EntitySet movingSets;
    Spatial world;
    TimeSource timeSource;
    Random rand = new Random();

    HashMap<EntityId, Long> singleShotgunLastShots = new HashMap<>();
    HashMap<EntityId, Long> nailgunLastShots = new HashMap<>();

    public ShootingSystem(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class);
        if (ed == null) {
            throw new RuntimeException("ShootingSystem requires an EntityData object.");
        }

        movingSets = ed.getEntities(ObjectType.class, BodyPosition.class, HealthComponent.class, ActiveWeaponComponent.class);
    }

    @Override
    protected void terminate() {
        movingSets.release();
    }

    public void shoot(EntityId shooter) {
        shoot(shooter, 0f);
    }

    public void shoot(EntityId shooter, float roundtriptime) {
        Entity eShooter = null;

        if (!movingSets.containsId(shooter)) {
            log.error("Someone unknown tried to shoot!");
            return;
        } else {
            eShooter = movingSets.getEntity(shooter);
        }

        if (isNull(eShooter)) {
            // when you get entities from entity lists, occasionally they will be null
            // discard the shooting attempt if the shooter is null
            return;
        }

        // We probably have to check all of this, because clients could still send something like that to us.
        if (eShooter.get(HealthComponent.class).isDead()) {
            log.error("Someone dead tried to shoot!");
            return;
        }

        if (world == null) {
            log.error("Got a shoot command but don't know the level geometry and thus can't make proper shot tracing");
            return;
        }

        // we also need to know what active weapon they are using is, so that we know what kind of behavior to have
        // shotguns are 'instant' hits
        // all other guns are moving object projectile systems.
        ActiveWeaponComponent active = eShooter.get(ActiveWeaponComponent.class);
        Long lastTimeFired;

        switch (WeaponTypes.values()[active.getWeaponNumber()]) {
            case SINGLESHOTGUN:
                // check ammo
                lastTimeFired = singleShotgunLastShots.get(eShooter.getId());
                if (lastTimeFired == null || (System.currentTimeMillis() - lastTimeFired) > 500) {
                    // generate cloud of bullets at the correct place and radius
                    handleShotgunShot(eShooter);
                    singleShotgunLastShots.put(eShooter.getId(), System.currentTimeMillis());
                }
                break;
            case NAILGUN:
                // check ammo
                lastTimeFired = nailgunLastShots.get(eShooter.getId());
                if (lastTimeFired == null || (System.currentTimeMillis() - lastTimeFired) > 100) {
                    // generate bullets and set their direction/velocity in the physics system
                    handleNailgunShot(eShooter);
                    nailgunLastShots.put(eShooter.getId(), System.currentTimeMillis());
                }

                break;
            default:
                return;
        }

    }

    // 
    private void handleShotgunShot(Entity eShooter) {

        // up here we need to figure out if we even have ammo, if we don't, FAILURE!!
        AmmoShotgunComponent ammo = ed.getComponent(eShooter.getId(), AmmoShotgunComponent.class);

        if (ammo == null || ammo.getAmmo() <= 0) {
            log.info("You have no AMMO for your shotgun!!!!!");
            return;
        }

        CollisionResults cr = castRayFromPlayer(eShooter);
        if (cr.size() > 0) {
            CollisionResult res = cr.getClosestCollision();

            // needs distance for shotgun shell radius calc
            float shotgunConeRadius = res.getDistance() / 22f;
            Vector3f contactLocation = res.getContactPoint();

            log.info("Contact Location: " + res.getContactPoint());
            log.info("Contact Normal: " + res.getContactNormal());

            // rotate the contact point towards the normal, so that when we generate shotgun pellets, normals are used.
            Quaternion quat = new Quaternion();
            quat.lookAt(res.getContactNormal(), Vector3f.UNIT_Y);

            // generate 6 shots at 'random' locations
            for (int i = 0; i <= 5; i++) {
                float randomX = rand.nextFloat() * shotgunConeRadius * 2;
                float randomY = rand.nextFloat() * shotgunConeRadius * 2;
                randomX = randomX - shotgunConeRadius;
                randomY = randomY - shotgunConeRadius;
                Vector3f contactLocation1 = contactLocation.add(quat.mult(new Vector3f(randomX, randomY, 0.1f)));
                GameEntities.createWeaponShell(ed, contactLocation1, 4, getManager().getStepTime());
            }

        }
    }

    private void handleNailgunShot(Entity eShooter) {
        // to be implemented
    }

    private CollisionResults castRayFromPlayer(Entity eShooter) {
        Node raycastNode = new Node();
        raycastNode.attachChild(world);
        CapsuleCollisionShape capsuleShape = (CapsuleCollisionShape) CollisionShapeProvider.lookup(ShapeInfos.playerInfo(ed));
        BodyPosition bodyPos = eShooter.get(BodyPosition.class);
        // BodyPosition requires special management to make
        // sure all instances of BodyPosition are sharing the same
        // thread-safe history buffer.  Everywhere it's used, it should
        // be 'initialized'.
        bodyPos.initialize(eShooter.getId(), 12);
        for (Entity e : movingSets) {
            if (e.getId().equals(eShooter.getId())) {
                continue;
            }

            BodyPosition bPos = e.get(BodyPosition.class);
            bPos.initialize(e.getId(), 12);

            //Geometry capsule = new Geometry(e.getId().toString(), new Cylinder(32, 32, capsuleShape.getRadius(), capsuleShape.getHeight()));
            Geometry capsule = CollisionShapeProvider.getPlayerCollisionShapeAsGeometry(ServerApplication.self, ed, -1.2f);
            capsule.setName(e.getId().toString());
            capsule.setUserData("entityId", e.getId().getId());
            capsule.setUserData("player", true);

            PositionTransition posTrans = bPos.getFrame(timeSource.getTime());

            // Hoping this is accurate
            capsule.setLocalTranslation(capsule.getLocalTranslation().add(posTrans.getPosition(timeSource.getTime())));
            capsule.setLocalRotation(posTrans.getRotation(timeSource.getTime()).mult(capsule.getLocalRotation()));
            raycastNode.attachChild(capsule);
        }
        PositionTransition posTrans = bodyPos.getFrame(timeSource.getTime());
        Ray shootingRay = new Ray(posTrans.getPosition(timeSource.getTime()).add(GameEntities.cameraOffset), posTrans.getRotation(timeSource.getTime()).mult(Vector3f.UNIT_Z));
        // @TODO: Weapon Range? shootingRay.setLimit(50);
        CollisionResults cr = new CollisionResults();
        raycastNode.collideWith(shootingRay, cr);
        log.warn("Raycast: " + shootingRay.getOrigin() + " => " + shootingRay.getDirection());
        log.warn("Numbers of Collisions: " + cr.size());
        return cr;
    }

    @Override
    public void update(SimTime time) {
        super.update(time);
        movingSets.applyChanges();

        if (world == null) {
            try {
                world = CollisionShapeProvider.world;
            } catch (AssetNotFoundException anf) {
                anf.printStackTrace();
            }
        }
    }
}
