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
package com.jmonkeyengine.monake.es;

import com.simsilica.es.EntityData;

/**
 * Factory methods for the common object types. Because we run the string names
 * through the EntityData's string index we can't just have normal constants.
 *
 * @author Paul Speed
 */
public class ObjectTypes {

    public static final String SHIP = "ship";
    public static final String GRAV_SPHERE = "gravSphere";
    public static final String WORLD = "world";
    public static final String BOX = "box";
    public static final String SPHERE = "sphere"; // for testing
    public static final String PLAYER = "player";
    public static final String PICKUP_HEALTH = "pickupHealth";
    public static final String PICKUP_AMMO_SHOTGUN = "pickupAmmoShotgun";
    public static final String PICKUP_AMMO_NAILGUN = "pickupAmmoNailgun";
    public static final String WEAPON_SHELL = "weaponShell";
    public static final String FLAG = "flag";

    public static ObjectType shipType(EntityData ed) {
        return ObjectType.create(SHIP, ed);
    }

    public static ObjectType gravSphereType(EntityData ed) {
        return ObjectType.create(GRAV_SPHERE, ed);
    }

    public static ObjectType worldType(EntityData ed) {
        return ObjectType.create(WORLD, ed);
    }

    public static ObjectType boxType(EntityData ed) {
        return ObjectType.create(BOX, ed);
    }

    public static ObjectType sphereType(EntityData ed) {
        return ObjectType.create(SPHERE, ed);
    }

    public static ObjectType pickupHealthType(EntityData ed) {
        return ObjectType.create(PICKUP_HEALTH, ed);
    }

    public static ObjectType pickupAmmoShotgunType(EntityData ed) {
        return ObjectType.create(PICKUP_AMMO_SHOTGUN, ed);
    }

    public static ObjectType pickupAmmoNailgunType(EntityData ed) {
        return ObjectType.create(PICKUP_AMMO_NAILGUN, ed);
    }

    public static ObjectType weaponShell(EntityData ed) {
        return ObjectType.create(WEAPON_SHELL, ed);
    }

    public static ObjectType playerType(EntityData ed) {
        return ObjectType.create(PLAYER, ed);
    }

    public static ObjectType flagType(EntityData ed) {
        return ObjectType.create(FLAG, ed);
    }
}
