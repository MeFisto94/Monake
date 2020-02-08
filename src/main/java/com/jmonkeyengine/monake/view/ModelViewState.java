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
package com.jmonkeyengine.monake.view;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.TextureKey;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jmonkeyengine.monake.ConnectionState;
import com.jmonkeyengine.monake.GameSessionState;
import com.jmonkeyengine.monake.Main;
import com.jmonkeyengine.monake.TimeState;
import com.jmonkeyengine.monake.es.*;
import com.jmonkeyengine.monake.es.components.TeamComponent;
import com.jmonkeyengine.monake.sim.CollisionShapeProvider;
import com.simsilica.es.*;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.mathd.trans.PositionTransition;
import com.simsilica.mathd.trans.TransitionBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Displays the models for the various physics objects.
 *
 * @author Paul Speed
 */
public class ModelViewState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(ModelViewState.class);

    private EntityData ed;
    private TimeState timeState;

    private Node modelRoot;

    private Map<EntityId, Spatial> modelIndex = new HashMap<>();

    private Map<String, Spatial> mapModels = new HashMap<>();

    private MobContainer mobs;
    private ModelContainer models;

    public ModelViewState() {
    }

    public Spatial getModel(EntityId id) {
        return modelIndex.get(id);
    }

    @Override
    protected void initialize(Application app) {
        modelRoot = new Node();

        // Retrieve the time source from the network connection
        // The time source will give us a time in recent history that we should be
        // viewing.  This currently defaults to -100 ms but could vary (someday) depending
        // on network connectivity.
        // For more information on this interpolation approach, see the Valve networking
        // articles at:
        // https://developer.valvesoftware.com/wiki/Source_Multiplayer_Networking
        // https://developer.valvesoftware.com/wiki/Latency_Compensating_Methods_in_Client/Server_In-game_Protocol_Design_and_Optimization
        //this.timeSource = getState(ConnectionState.class).getRemoteTimeSource();
        // 
        // We now grab time from the TimeState which wraps the TimeSource to give
        // consistent timings over the whole frame
        this.timeState = getState(TimeState.class);

        this.ed = getState(ConnectionState.class).getEntityData();

        // get the world loaded and the models loaded before any entities are assigned
        // this prevents nullpointers later on.
        initializeWorld();
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {

        mobs = new MobContainer(ed);
        models = new ModelContainer(ed);
        mobs.start();
        models.start();

        ((Main) getApplication()).getRootNode().attachChild(modelRoot);
    }

    @Override
    protected void onDisable() {
        modelRoot.removeFromParent();

        models.stop();
        mobs.stop();
        mobs = null;
        models = null;
    }

    @Override
    public void update(float tpf) {

        // Grab a consistent time for this frame
        long time = timeState.getTime();

        // Update all of the models
        models.update();
        mobs.update();
        for (Mob mob : mobs.getArray()) {
            mob.updateSpatial(time);
        }
    }

    private void initializeWorld() {
        Spatial world;

        try {
            // @TODO: use the same model as in BasicEnvironment#start()
            world = getApplication().getAssetManager().loadModel("Scenes/ctf_arena1.j3o");
            world.setLocalScale(3f); // @TODO: Increase the map size
            world.depthFirstTraversal(new SceneGraphVisitorAdapter() {
                @Override
                public void visit(Geometry geom) {
                    super.visit(geom);
                    // Not needed anymore, Blender 2.81 GLTF Exporter has that fixed long time ago, I guess
                    /*if (geom.getMaterial().getMaterialDef().getAssetName().equals("Common/MatDefs/Light/PBRLighting.j3md")) {
                        //geom.getMaterial().setBoolean("UseMetallicFirstPacking", true);
                        geom.getMaterial().setBoolean("UseBrokenGLTFExporter", true);
                    }*/

                    /*for (Light light : geom.getLocalLightList()) {
                        if (light instanceof PointLight) {
                            light.setColor(light.getColor().mult(5));
                        }
                    }

                    geom.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);*/
                    processGameEntityFromMap(geom);

                }

                @Override
                public void visit(Node geom) {
                    super.visit(geom); //To change body of generated methods, choose Tools | Templates.
                    processGameEntityFromMap(geom);
                }

            });

        } catch (AssetNotFoundException anf) {
            // Show programmers art if assets not found
            world = new Geometry("World", new Box(64f, 0.5f, 64f));
            Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Red);
            world.setMaterial(mat);
        }

        mapModels.put("World", world);

    }

    protected Spatial createShip(Entity entity) {

        AssetManager assetManager = getApplication().getAssetManager();

        Spatial ship = assetManager.loadModel("Models/fighter.j3o");
        ship.center();
        Texture texture = assetManager.loadTexture("Textures/ship1.png");
        //Material mat = GuiGlobals.getInstance().createMaterial(texture, false).getMaterial();
        Material mat = new Material(getApplication().getAssetManager(), "MatDefs/FogUnshaded.j3md");
        mat.setTexture("ColorMap", texture);
        mat.setColor("FogColor", new ColorRGBA(0, 0, 0.1f, 1));
        mat.setFloat("FogDepth", 64);
        ship.setMaterial(mat);

        Node result = new Node("ship:" + entity.getId());
        result.attachChild(ship);

        result.setUserData("entityId", entity.getId().getId());

        return result;
    }

    protected Spatial createWorld(Entity entity) {
        Spatial world = mapModels.get("World");

        Node result = new Node("World: " + entity.getId());
        result.attachChild(world);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    // so, we need to remove the spatial from the scene, and add it to the models list
    // to be added back in by the entity system
    protected void processGameEntityFromMap(Spatial spatial) {

        String value = spatial.getUserData("IsPickupComponent");
        if (value != null) {
            // pickup types
            //      - health
            //      - ammo shotgun 
            //      - ammo nailgun 
            //      - weapons nailgun
            spatial.removeFromParent();
            if (spatial.getUserData("HealthComponent") != null) {
                String health = spatial.getUserData("HealthComponent");
                Spatial healthSpatial = spatial.deepClone();
                healthSpatial.setLocalTranslation(0, 0, 0);
                mapModels.put("Health" + health, healthSpatial);
                log.info("Loaded health");
            }
            if (spatial.getUserData("AmmoShotgunComponent") != null) {
                String ammo = spatial.getUserData("AmmoShotgunComponent");
                Spatial ammoSpatial = spatial.deepClone();
                ammoSpatial.setLocalTranslation(0, 0, 0);
                mapModels.put("AmmoShotgun" + ammo, ammoSpatial);
            }
            if (spatial.getUserData("AmmoNailgunComponent") != null) {
                String ammo = spatial.getUserData("AmmoNailgunComponent");
                Spatial ammoSpatial = spatial.deepClone();
                ammoSpatial.setLocalTranslation(0, 0, 0);
                mapModels.put("AmmoNailgun" + ammo, ammoSpatial);
            }
            if (spatial.getUserData("NailGun") != null) {
                String Nailgun = spatial.getUserData("NailGun");
                Spatial nailgunSpatial = spatial.deepClone();
                nailgunSpatial.setLocalTranslation(0, 0, 0);
                mapModels.put("Nailgun", nailgunSpatial);
                log.info("Weapon NailGun: " + Nailgun);
            }
        }

        value = spatial.getUserData("Spawn");
        if (value != null) {
            log.info("Spawn: " + spatial.getWorldTranslation());
            spatial.removeFromParent();
        }

        value = spatial.getUserData("Teleport");
        if (value != null) {
            log.info("Teleporter: " + spatial.getWorldTranslation());
            spatial.removeFromParent();
        }

        value = spatial.getUserData("TeleportEndPoint");
        if (value != null) {
            log.info("TeleporterEndPoint: " + spatial.getWorldTranslation());
            spatial.removeFromParent();
        }

    }

    protected Spatial createGravSphere(Entity entity) {

        SphereShape shape = ed.getComponent(entity.getId(), SphereShape.class);
        float radius = shape == null ? 1 : (float) shape.getRadius();

        GuiGlobals globals = GuiGlobals.getInstance();
        Sphere sphere = new Sphere(40, 40, radius);
        sphere.setTextureMode(Sphere.TextureMode.Projected);
        sphere.scaleTextureCoordinates(new Vector2f(60, 40));
        Geometry geom = new Geometry("test", sphere);
        Texture texture = globals.loadTexture("Textures/gravsphere.png", true, true);
        //Material mat = globals.createMaterial(texture, false).getMaterial();
        Material mat = new Material(getApplication().getAssetManager(), "MatDefs/FogUnshaded.j3md");
        mat.setTexture("ColorMap", texture);
        mat.setColor("FogColor", new ColorRGBA(0, 0, 0.1f, 1));
        mat.setFloat("FogDepth", 256);
        geom.setMaterial(mat);

        geom.setLocalTranslation(16, 16, 16);
        geom.rotate(-FastMath.HALF_PI, 0, 0);

        return geom;
    }

    protected Spatial createBox(Entity entity) {
        Spatial box = new Geometry("Box", new Box(1f, 1f, 1f));
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        box.setMaterial(mat);

        Node result = new Node("Box: " + entity.getId());
        result.attachChild(box);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createSphere(Entity entity) {
        Sphere sphere = new Sphere(20, 20, 0.2f);
        Geometry sphereGeo = new Geometry("test", sphere);

        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.randomColor());
        sphereGeo.setMaterial(mat);

        Node result = new Node("Sphere: " + entity.getId());
        result.attachChild(sphereGeo);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createWeaponShell(Entity entity) {
        Sphere sphere = new Sphere(20, 20, 0.2f);
        Geometry sphereGeo = new Geometry("test", sphere);

        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.LightGray);
        sphereGeo.setMaterial(mat);

        Node result = new Node("Sphere: " + entity.getId());
        result.attachChild(sphereGeo);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createAmmoShotgun(Entity entity) {
        Spatial spatial = mapModels.get("AmmoShotgun20").deepClone();
        Node result = new Node("AmmoShotgun: " + entity.getId());
        result.attachChild(spatial);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createAmmoNailgun(Entity entity) {
        Spatial spatial = mapModels.get("AmmoNailgun20").deepClone();
        Node result = new Node("AmmoNailgun: " + entity.getId());
        result.attachChild(spatial);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createHealth(Entity entity) {
        Spatial spatial = mapModels.get("Health10").deepClone();
        Node result = new Node("Health: " + entity.getId());
        result.attachChild(spatial);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createPlayer(Entity entity) {
        Spatial player = getApplication().getAssetManager().loadModel("Models/Jaime.j3o");
        player.move(0f, -1.2f, 0f); // Prevent "flying"
        player.setLocalScale(3f);

        Node result = new Node("Player: " + entity.getId());
        result.attachChild(player);
        result.attachChild(CollisionShapeProvider.getPlayerCollisionShapeAsGeometry(getApplication(), ed, -1.2f));
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createFlag(Entity entity) {
        Team team = ed.getComponent(entity.getId(), TeamComponent.class).getTeam();
        Node flag = (Node)getApplication().getAssetManager().loadModel("Models/Flag.j3o");
        flag.setLocalScale(3f); // Temporary
        Geometry flagGeom = (Geometry)flag.getChild("FlagMesh");
        Material mat = flagGeom.getMaterial();
        // Currently the texture was baked into the j3o.
        mat.setTexture("BaseColorMap", getApplication().getAssetManager().loadTexture(new TextureKey("Textures/Flag/FlagBase.png", false)));
        ColorRGBA teamColor = team.equals(Team.RED) ? ColorRGBA.Red : ColorRGBA.Cyan;
        mat.setColor("BaseColor", teamColor);
        Node result = new Node("Flag: " + entity.getId());
        result.attachChild(flag);
        result.setUserData("entityId", entity.getId().getId());
        return result;
    }

    protected Spatial createModel(Entity entity) {
        // Check to see if one already exists
        Spatial result = modelIndex.get(entity.getId());
        if (result != null) {
            return result;

        }

        // Else figure out what type to create... 
        ObjectType type = entity.get(ObjectType.class);
        String typeName = type.getTypeName(ed);
        switch (typeName) {
            case ObjectTypes.SHIP:
                result = createShip(entity);
                break;
            case ObjectTypes.GRAV_SPHERE:
                result = createGravSphere(entity);
                break;
            case ObjectTypes.WORLD:
                result = createWorld(entity);
                break;
            case ObjectTypes.BOX:
                result = createBox(entity);
                break;
            case ObjectTypes.SPHERE:
                result = createSphere(entity);
                break;
            case ObjectTypes.WEAPON_SHELL:
                result = createWeaponShell(entity);
                break;
            case ObjectTypes.PICKUP_AMMO_SHOTGUN:
                result = createAmmoShotgun(entity);
                break;
            case ObjectTypes.PICKUP_AMMO_NAILGUN:
                result = createAmmoNailgun(entity);
                break;
            case ObjectTypes.PICKUP_HEALTH:
                result = createHealth(entity);
                break;
            case ObjectTypes.PLAYER:
                result = createPlayer(entity);
                break;
            case ObjectTypes.FLAG:
                result = createFlag(entity);
                break;
            default:
                throw new RuntimeException("Unknown spatial type:" + typeName);
        }

        // Add it to the index
        modelIndex.put(entity.getId(), result);

        modelRoot.attachChild(result);

        return result;

    }

    protected void updateModel(Spatial spatial, Entity entity, boolean updatePosition) {
        if (updatePosition) {
            Position pos = entity.get(Position.class);

            // I like to move it... move it...
            spatial.setLocalTranslation(pos.getLocation().toVector3f());
            spatial.setLocalRotation(pos.getFacing().toQuaternion());
        }
    }

    protected void removeModel(Spatial spatial, Entity entity) {
        modelIndex.remove(entity.getId());
        spatial.removeFromParent();

    }

    private class Mob {

        Entity entity;
        Spatial spatial;
        boolean visible;
        boolean localPlayerShip;

        TransitionBuffer<PositionTransition> buffer;

        public Mob(Entity entity) {
            this.entity = entity;

            this.spatial = createModel(entity); //createCharacter(entity);
            //modelRoot.attachChild(spatial);

            BodyPosition bodyPos = entity.get(BodyPosition.class);
            // BodyPosition requires special management to make
            // sure all instances of BodyPosition are sharing the same
            // thread-safe history buffer.  Everywhere it's used, it should
            // be 'initialized'.            
            bodyPos.initialize(entity.getId(), 12);
            buffer = bodyPos.getBuffer();

            // If this is the player's ship then we don't want the model
            // shown else it looks bad.  A) it's ugly.  B) the model will
            // always lag the player's turning.
            if (entity.getId().getId() == getState(GameSessionState.class).getCharacterId().getId()) {
                this.localPlayerShip = true;
            }

            // Starts invisible until we know otherwise           
            resetVisibility();
        }

        public void updateSpatial(long time) {

            // Look back in the brief history that we've kept and
            // pull an interpolated value.  To do this, we grab the
            // span of time that contains the time we want.  PositionTransition
            // represents a starting and an ending pos+rot over a span of time.
            PositionTransition trans = buffer.getTransition(time);
            if (trans != null) {
                spatial.setLocalTranslation(trans.getPosition(time, true));
                spatial.setLocalRotation(trans.getRotation(time, true));
                setVisible(trans.getVisibility(time));
            }
        }

        protected void updateComponents() {
            updateModel(spatial, entity, false);
        }

        protected void setVisible(boolean f) {
            if (this.visible == f) {
                return;
            }
            this.visible = f;
            resetVisibility();
        }

        protected void resetVisibility() {
            if (visible && !localPlayerShip) {
                spatial.setCullHint(Spatial.CullHint.Inherit);
            } else {
                spatial.setCullHint(Spatial.CullHint.Always);
            }
        }

        public void dispose() {
            if (models.getObject(entity.getId()) == null) {
                removeModel(spatial, entity);
            }
        }
    }

    private class MobContainer extends EntityContainer<Mob> {

        public MobContainer(EntityData ed) {
            super(ed, ObjectType.class, BodyPosition.class);
        }

        @Override
        protected Mob[] getArray() {
            return super.getArray();
        }

        @Override
        protected Mob addObject(Entity e) {
            System.out.println("MobContainer.addObject(" + e + ")");
            return new Mob(e);
        }

        @Override
        protected void updateObject(Mob object, Entity e) {
            object.updateComponents();
        }

        @Override
        protected void removeObject(Mob object, Entity e) {
            object.dispose();
        }
    }

    /**
     * Contains the static objects... care needs to be taken that if an object
     * exists in both the MobContainer and this one that the MobContainer takes
     * precedence.
     */
    private class ModelContainer extends EntityContainer<Spatial> {

        public ModelContainer(EntityData ed) {
            super(ed, ObjectType.class, Position.class);
        }

        @Override
        protected Spatial addObject(Entity e) {
            System.out.println("ModelContainer.addObject(" + e + ")");
            Spatial result = createModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            System.out.println("MobContainer.updateObject(" + e + ")");
            updateModel(object, e, true);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            if (mobs.getObject(e.getId()) == null) {
                removeModel(object, e);
            }
        }

    }
}
