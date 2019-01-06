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
import com.jmonkeyengine.monake.es.ObjectTypes;
import com.jmonkeyengine.monake.es.ShapeInfos;
import com.jmonkeyengine.monake.es.components.AmmoShotgunComponent;
import com.jmonkeyengine.monake.es.components.HealthComponent;
import com.jmonkeyengine.monake.es.components.IsPickupComponent;
import com.jmonkeyengine.monake.util.server.ServerApplication;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
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
    EntitySet playerSet;

    public PickupSystem() {
    }

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class);
        if (ed == null) {
            throw new RuntimeException("ShootingSystem requires an EntityData object.");
        }

        getSystem(BulletSystem.class).addEntityCollisionListener(this);
    }

    @Override
    protected void terminate() {
        getSystem(BulletSystem.class).removeEntityCollisionListener(this);
    }

    @Override
    public void update(SimTime time) {
        super.update(time);
    }

    @Override
    public void collision(EntityPhysicsObject object1, EntityPhysicsObject object2, PhysicsCollisionEvent event) {

        // which objects do we have, is it a player on player, is it player on bullet? what happened
        // can directly access the entity system for this, because we're on the server and it will be fast
        // can't garuntee which components are going to be in the colliding objects so its hard to filter
        Entity objectEntity1 = ed.getEntity(object1.getId(), ObjectType.class);
        Entity objectEntity2 = ed.getEntity(object2.getId(), ObjectType.class);

        ObjectType objectTypeClass1 = objectEntity1.get(ObjectType.class);
        ObjectType objectTypeClass2 = objectEntity2.get(ObjectType.class);

        String objectType1 = objectTypeClass1.getTypeName(ed);
        String objectType2 = objectTypeClass2.getTypeName(ed);

        if (objectType1.equals(ObjectTypes.PLAYER) && objectType2.equals(ObjectTypes.PICKUP_HEALTH)) {
            processHealthPlayerCollision(objectEntity1, objectEntity2);
        } else if (objectType2.equals(ObjectTypes.PLAYER) && objectType1.equals(ObjectTypes.PICKUP_HEALTH)) {
            processHealthPlayerCollision(objectEntity2, objectEntity1);
        }

        if (objectType1.equals(ObjectTypes.PLAYER) && objectType2.equals(ObjectTypes.PICKUP_AMMO_SHOTGUN)) {
            processAmmoShotgunPlayerCollision(objectEntity1, objectEntity2);
        } else if (objectType2.equals(ObjectTypes.PLAYER) && objectType1.equals(ObjectTypes.PICKUP_AMMO_SHOTGUN)) {
            processAmmoShotgunPlayerCollision(objectEntity2, objectEntity1);
        }

    }

    private void processHealthPlayerCollision(Entity player, Entity health) {
        HealthComponent healthBoost = ed.getComponent(health.getId(), HealthComponent.class);
        HealthComponent playerHealth = ed.getComponent(player.getId(), HealthComponent.class);

        if (playerHealth.isDead()) {
            return;
        }

        int newHealth = Math.min(200, playerHealth.getHealth() + healthBoost.getHealth());
        System.out.println("Healing for " + newHealth);
        ed.setComponent(player.getId(), new HealthComponent(newHealth));
        ed.removeEntity(health.getId());
        // @TODO: Reschedule for Respawn Timer
    }

    private void processAmmoShotgunPlayerCollision(Entity player, Entity ammo) {
        AmmoShotgunComponent ammoBoost = ed.getComponent(ammo.getId(), AmmoShotgunComponent.class);
        AmmoShotgunComponent playerAmmo = ed.getComponent(player.getId(), AmmoShotgunComponent.class);

        int newAmmo = Math.min(200, playerAmmo.getAmmo() + ammoBoost.getAmmo());
        System.out.println("Shotgun Ammo for " + newAmmo);
        ed.setComponent(player.getId(), new AmmoShotgunComponent(newAmmo));
        ed.removeEntity(ammo.getId());
        // @TODO: Reschedule for Respawn Timer
    }

}
