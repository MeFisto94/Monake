package com.jmonkeyengine.monake.sim;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jmonkeyengine.monake.es.BodyPosition;
import com.jmonkeyengine.monake.es.ObjectType;
import com.jmonkeyengine.monake.es.ShapeInfos;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShootingSystem extends AbstractGameSystem {
    static Logger log = LoggerFactory.getLogger(ShootingSystem.class);
    EntityData ed;
    EntitySet movingSets;
    Spatial world;
    TimeSource timeSource;

    public ShootingSystem(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class);
        if( ed == null ) {
            throw new RuntimeException("ShootingSystem requires an EntityData object.");
        }

        movingSets = ed.getEntities(ObjectType.class, BodyPosition.class, HealthComponent.class);
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

        // We probably have to check all of this, because clients could still send something like that to us.
        if (eShooter.get(HealthComponent.class).isDead()) {
            log.error("Someone dead tried to shoot!");
            return;
        }

        if (world == null) {
            log.error("Got a shoot command but don't know the level geometry and thus can't make proper shot tracing");
            return;
        }

        Node raycastNode = new Node();
        raycastNode.attachChild(world);

        CapsuleCollisionShape capsuleShape = (CapsuleCollisionShape)CollisionShapeProvider.lookup(ShapeInfos.playerInfo(ed));

        BodyPosition bodyPos = eShooter.get(BodyPosition.class);
        // BodyPosition requires special management to make
        // sure all instances of BodyPosition are sharing the same
        // thread-safe history buffer.  Everywhere it's used, it should
        // be 'initialized'.
        bodyPos.initialize(shooter, 12);

        for (Entity e: movingSets) {
            if (e.getId().equals(shooter)) {
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

        if (cr.size() > 0) {
            CollisionResult res = cr.getClosestCollision();
            if (res.getGeometry().getUserData("player") == Boolean.valueOf(true)) {
                Long victimId = res.getGeometry().getUserData("entityId");
                if (victimId == null) {
                    throw new IllegalStateException("Shot a player which has an entityId User-Data attached to it, but isn't known to the entitySet. Impossible");
                }

                Entity eVictim = movingSets.getEntity(new EntityId(victimId));
                log.warn("Player " + eShooter.getId() + " shot " + eVictim.getId());

                ed.setComponents(eVictim.getId(), new HealthComponent(Math.max(0, eVictim.get(HealthComponent.class).getHealth() - 30)));
            }
        }
    }

    @Override
    public void update(SimTime time) {
        super.update(time);
        movingSets.applyChanges();

        if (world == null) {
            try {
                world = ServerApplication.self.getAssetManager().loadModel("Models/level.j3o");
            } catch (AssetNotFoundException anf) {
                anf.printStackTrace();
            }
        }
    }
}
