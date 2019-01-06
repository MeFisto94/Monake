/*
 * Monake
 *
 * Copyright (c) 2018 Monake Dev Team
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
package com.jmonkeyengine.monake.view.effects;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jmonkeyengine.monake.ConnectionState;
import com.jmonkeyengine.monake.Main;
import com.jmonkeyengine.monake.es.components.EffectComponent;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wobblytrout
 *
 * This state takes care of creation of effects, it will have an effects factory
 * for creating effects and there associated controls.
 *
 */
public class EffectsState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(EffectsState.class);

    private EntityData ed;
    private EntitySet effectEntities;
    private Node effectsNode;

    private HashMap<EntityId, Spatial> entityEffects = new HashMap<>();

    @Override
    protected void initialize(Application aplctn) {

        Main main = (Main) aplctn;

        this.ed = getState(ConnectionState.class).getEntityData();
        effectEntities = ed.getEntities(EffectComponent.class);

        effectsNode = new Node("Effects_Node");
        main.getRootNode().attachChild(effectsNode);

    }

    @Override
    protected void cleanup(Application aplctn) {
    }

    @Override
    public void update(float tpf) {
        super.update(tpf); //To change body of generated methods, choose Tools | Templates.

        if (effectEntities.applyChanges()) {
            for (Entity entity : effectEntities.getAddedEntities()) {
                EffectComponent effect = entity.get(EffectComponent.class);
                EffectType type = EffectType.values()[effect.getEffectType()];
                createEffect(type, effect);
            }

            Iterator it = entityEffects.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<EntityId, Node> entry = (Map.Entry) it.next();
                if (entry.getValue().getParent() == null) {
                    it.remove();
                }
            }
        }

    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    private void createEffect(EffectType type, EffectComponent effect) {
        log.info("CreateEffect");
        log.info("EffectType: " + type);
        log.info("Effect: " + effect);

        switch (type) {
            case EXPLOSION:
                break;
            case SHOTGUNPARTICLES:
                createShotgunParticle(type, effect);
                break;
            case TELEPORTEFFECT:
                break;
            case CANCELEFFECT:
                break;
        }
    }

    private void createShotgunParticle(EffectType type, EffectComponent effectInfo) {

        Sphere sphere = new Sphere(20, 20, 0.2f);
        Geometry sphereGeo = new Geometry("test", sphere);

        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        sphereGeo.setMaterial(mat);

        Node effectSpatial = new Node("Sphere: " + effectInfo.getEffectEntity());
        effectSpatial.attachChild(sphereGeo);
        effectSpatial.setUserData("entityId", effectInfo.getEffectEntity().getId());

        effectSpatial.setLocalTranslation(effectInfo.getEffectPosition());
        effectSpatial.addControl(new RemoveAfterDelayControl(250));
        
        entityEffects.put(effectInfo.getEffectEntity(), effectSpatial);
        effectsNode.attachChild(effectSpatial);

    }

}
