/*
 * $Id$
 * 
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jmonkeyengine.monake.sim;

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jmonkeyengine.monake.bullet.Ghost;
import com.jmonkeyengine.monake.bullet.Mass;
import com.jmonkeyengine.monake.bullet.ShapeInfo;
import com.jmonkeyengine.monake.bullet.SpawnPosition;
import com.jmonkeyengine.monake.es.*;
import com.jmonkeyengine.monake.es.components.*;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.Name;
import com.simsilica.es.common.Decay;
import com.simsilica.mathd.Quatd;
import com.simsilica.mathd.Vec3d;
import com.simsilica.sim.SimTime;
import java.util.ArrayList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for creating the common game entities used by the simulation.
 * In cases where a game entity may have multiple specific components or
 * dependencies used to create it, it can be more convenient to have a
 * centralized factory method. Especially if those objects are widely used. For
 * entities with only a few components or that are created by one system and
 * only consumed by one other, then this is not necessarily true.
 *
 * @author Paul Speed
 */
public class GameEntities {

    static Logger log = LoggerFactory.getLogger(GameEntities.class);
    public static final Vector3f cameraOffset = new Vector3f(0f, 1.75f, 0f);
    public static final ArrayList<Transform> spawnLocations = new ArrayList<>();
    protected static Random random = new Random();

    public static Transform getRandomSpawnSpot() {
        return spawnLocations.get(random.nextInt(spawnLocations.size()));
    }

    public static EntityId createCharacter(EntityId parent, EntityData ed) {
        EntityId result = ed.createEntity();
        Name name = ed.getComponent(parent, Name.class);
        ed.setComponent(result, name);
        ed.setComponents(result, ObjectTypes.playerType(ed), new Mass(50f), new SpawnPosition(getRandomSpawnSpot()),
                ShapeInfos.playerInfo(ed));
        ed.setComponents(result, new HealthComponent(100), new ActiveWeaponComponent(WeaponTypes.SINGLESHOTGUN.ordinal()), new AmmoShotgunComponent(25), new AmmoNailgunComponent(0), new ArmorComponent(0, 0));
        return result;
    }

    public static EntityId createWeapon(EntityData ed) {
        // @TODO: Add WeaponType and a Weapon Component
        EntityId result = ed.createEntity();
        ed.setComponent(result, new AmmoShotgunComponent(64));
        return result;
    }

    public static EntityId createWorld(EntityData ed) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.worldType(ed), new SpawnPosition(0f, 0f, 0f),
                new Position(0f, 0f, 0f), new Mass(0), ShapeInfos.worldInfo(ed));
        return result;
    }

    public static EntityId createBox(EntityData ed, float xOffset) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.boxType(ed), new SpawnPosition(xOffset, 500f, 0f),
                new Mass(1), ShapeInfos.boxInfo(ed));
        return result;
    }

    public static EntityId createSphere(EntityData ed, Vector3f location) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.sphereType(ed), new SpawnPosition(location),
                new Mass(1), ShapeInfos.sphereInfo(ed));
        return result;
    }

    public static EntityId createWeaponShell(EntityData ed, Vector3f location, int damage, SimTime time) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.weaponShell(ed), new SpawnPosition(location),
                new DamageComponent(damage), ShapeInfos.sphereInfo(ed), new Ghost(Ghost.COLLIDE_DYNAMIC),
                Decay.duration(time.getTime(), time.toSimTime(0.3f)));
        return result;
    }

    public static EntityId createHealthPickup(EntityData ed, int amount, Vector3f position) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.pickupHealthType(ed), new SpawnPosition(position),
                new HealthComponent(amount), ShapeInfos.boxInfo(ed), new Ghost(Ghost.COLLIDE_DYNAMIC),
                new IsPickupComponent());
        log.info("CreateHealth: " + amount);
        return result;
    }

    public static EntityId createAmmoShotgunPickup(EntityData ed, int amount, Vector3f position) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.pickupAmmoShotgunType(ed), new SpawnPosition(position),
                new AmmoShotgunComponent(amount), ShapeInfos.boxInfo(ed), new Ghost(Ghost.COLLIDE_DYNAMIC),
                new IsPickupComponent());
        return result;
    }

    public static EntityId createAmmoNailgunPickup(EntityData ed, int amount, Vector3f position) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.pickupAmmoNailgunType(ed), new SpawnPosition(position),
                new AmmoNailgunComponent(amount), ShapeInfos.boxInfo(ed), new Ghost(Ghost.COLLIDE_DYNAMIC),
                new IsPickupComponent());
        return result;
    }

    public static EntityId createFlag(EntityData ed, Vector3f position, Team team) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ObjectTypes.flagType(ed), new Position(new Vec3d(position)), new TeamComponent(team));
        return result;
    }

}
