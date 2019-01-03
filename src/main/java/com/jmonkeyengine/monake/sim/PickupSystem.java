package com.jmonkeyengine.monake.sim;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jmonkeyengine.monake.bullet.*;
import com.jmonkeyengine.monake.es.BodyPosition;
import com.jmonkeyengine.monake.es.ObjectType;
import com.jmonkeyengine.monake.es.ShapeInfos;
import com.jmonkeyengine.monake.es.components.HealthComponent;
import com.jmonkeyengine.monake.util.server.ServerApplication;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.ethereal.TimeSource;
import com.simsilica.mathd.trans.PositionTransition;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickupSystem extends AbstractGameSystem implements EntityCollisionListener {
    static Logger log = LoggerFactory.getLogger(PickupSystem.class);
    EntityData ed;
    EntitySet healthSet;

    public PickupSystem() {
    }

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class);
        if( ed == null ) {
            throw new RuntimeException("ShootingSystem requires an EntityData object.");
        }

        getSystem(BulletSystem.class).addEntityCollisionListener(this);

        healthSet = ed.getEntities(ObjectType.class, BodyPosition.class, Ghost.class, HealthComponent.class);
        //healthSet = ed.getEntities(ObjectType.class, BodyPosition.class, Ghost.class, HealthComponent.class);
        //healthSet = ed.getEntities(ObjectType.class, BodyPosition.class, Ghost.class, HealthComponent.class);
    }

    @Override
    protected void terminate() {
        healthSet.release();
    }

    @Override
    public void update(SimTime time) {
        super.update(time);
        healthSet.applyChanges();
    }

    @Override
    public void collision(EntityPhysicsObject object1, EntityPhysicsObject object2, PhysicsCollisionEvent event) {
        EntityGhostObject ghost;
        EntityRigidBody player;

        if (object1 instanceof EntityGhostObject && object2 instanceof EntityRigidBody) {
            ghost = (EntityGhostObject)object1;
            player = (EntityRigidBody)object2;
        } else if (object2 instanceof EntityGhostObject) {
            ghost = (EntityGhostObject)object2;
            player = (EntityRigidBody)object1;
        } else {
            return; // Skip event, not sure what happened
        }

        if (healthSet.containsId(ghost.getId())) {
            Entity eHealth = healthSet.getEntity(ghost.getId());
            HealthComponent health = eHealth.get(HealthComponent.class);
            System.out.println("Got Health: " + health.getHealth());
        }
    }
}
