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

import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jmonkeyengine.monake.bullet.CollisionShapes;
import com.jmonkeyengine.monake.es.Team;
import com.jmonkeyengine.monake.util.server.ServerApplication;
import com.simsilica.es.EntityData;
import com.simsilica.mathd.Vec3d;
import com.simsilica.sim.AbstractGameSystem;
import com.jmonkeyengine.monake.sim.CollisionShapeProvider;
import com.jmonkeyengine.monake.view.ModelViewState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a bunch of base entities in the environment.
 *
 * @author Paul Speed
 * @author Wobblytrout
 * @author DarkChaosGames
 *
 */
public class BasicEnvironment extends AbstractGameSystem {

    static Logger log = LoggerFactory.getLogger(BasicEnvironment.class);

    private EntityData ed;

    // we need to pull out teleporters
    // we need to pull out spawn locations
    // teleporter end points
    // pickups, for ammo, health and weapons
    @Override
    protected void initialize() {
        this.ed = getSystem(EntityData.class);
        if (ed == null) {
            throw new RuntimeException("Basic Environment system requires an EntityData object.");
        }
    }

    @Override
    protected void terminate() {
    }

    @Override
    public void start() {
        // Load the Map here:

        //Spatial spatial = ServerApplication.self.getAssetManager().loadModel("Models/level.j3o");
        Spatial spatial = ServerApplication.self.getAssetManager().loadModel("Scenes/ctf_arena1.j3o");
        spatial.setLocalScale(3f); // @TODO: Scale the map instead

        processWorld(spatial);

        // get all the special objects out of the world, and feed them into the places that need them from here.
        // after setting up the world, we need to set it so that collisions can be created.
        CollisionShapeProvider.setWorld(spatial);
        CollisionShapeProvider.registerShapes(getSystem(CollisionShapes.class));
        GameEntities.createWorld(ed);

        //GameEntities.createHealthPickup(ed, 10, new Vector3f(30f, 2f, -40f));
        GameEntities.createFlag(ed, new Vector3f(-2.2f, -7f, 2f), Team.BLUE);

//        GameEntities.createAmmoShotgunPickup(ed, 100, new Vector3f(10f, 2f, -40f));
    }

    private void processWorld(Spatial spatial) {

        spatial.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                super.visit(geom);
                processGameEntityFromMap(geom);
            }

            @Override
            public void visit(Node geom) {
                super.visit(geom);
                processGameEntityFromMap(geom);
            }

        });
    }

    protected void processGameEntityFromMap(Spatial spatial) {

        String value = spatial.getUserData("IsPickupComponent");
        if (value != null) {
            // pickup types
            //      - health
            //      - ammo shotgun 
            //      - ammo nailgun 
            //      - weapons nailgun
            if (spatial.getUserData("HealthComponent") != null) {
                String health = spatial.getUserData("HealthComponent");
                log.info("Health: " + health);
                GameEntities.createHealthPickup(ed, Integer.parseInt(health), spatial.getLocalTranslation());
            }
            if (spatial.getUserData("AmmoShotgunComponent") != null) {
                String ammo = spatial.getUserData("AmmoShotgunComponent");
                GameEntities.createAmmoShotgunPickup(ed, Integer.parseInt(ammo), spatial.getLocalTranslation());
            }
            if (spatial.getUserData("AmmoNailgunComponent") != null) {
                String ammo = spatial.getUserData("AmmoNailgunComponent");
                GameEntities.createAmmoNailgunPickup(ed, Integer.parseInt(ammo), spatial.getLocalTranslation());
            }
            if (spatial.getUserData("NailGun") != null) {
                String Nailgun = spatial.getUserData("NailGun");
                log.info("Weapon NailGun: " + Nailgun);
            }
            spatial.removeFromParent();
        }

        value = spatial.getUserData("Spawn");
        if (value != null) {
            log.info("Spawn: " + spatial.getLocalTranslation());
            GameEntities.spawnLocations.add(spatial.getWorldTransform().clone());
            spatial.removeFromParent();
        }

        value = spatial.getUserData("Teleport");
        if (value != null) {
            log.info("Teleporter: " + spatial.getLocalTranslation());
            spatial.removeFromParent();
        }

        value = spatial.getUserData("TeleportEndPoint");
        if (value != null) {
            log.info("TeleporterEndPoint: " + spatial.getLocalTranslation());
            spatial.removeFromParent();
        }

    }

    @Override
    public void stop() {
        // For now at least, we won't be reciprocal, ie: we won't remove
        // all of the stuff we added.
    }

}
