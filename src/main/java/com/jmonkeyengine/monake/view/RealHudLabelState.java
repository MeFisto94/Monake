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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jmonkeyengine.monake.ConnectionState;
import com.jmonkeyengine.monake.GameSessionState;
import com.jmonkeyengine.monake.Main;
import com.jmonkeyengine.monake.es.components.ArmorComponent;
import com.jmonkeyengine.monake.es.components.HealthComponent;
import com.simsilica.es.*;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.style.ElementId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Displays a HUD for the current player
 *
 *  @author MeFisto94
 */
public class RealHudLabelState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(RealHudLabelState.class);
    private EntityData ed;
    private Node hudLabelRoot;

    // We need a WatchedEntity here, as ed.getEntity() would query the live-data every frame
    protected WatchedEntity playerStats;

    Container uiContainer;
    Label lblHealth;
    Label lblArmor;
    Label lblAmmo;

    public RealHudLabelState() {
    }

    @Override
    protected void initialize( Application app ) {
        hudLabelRoot = new Node("HUD labels");
        this.ed = getState(ConnectionState.class).getEntityData();

        uiContainer = new Container(new BorderLayout(), new ElementId("hud.container"));
        lblAmmo = new Label("1337", new ElementId("ammo.hud.label"));
        lblArmor = new Label("9000", new ElementId("armor.hud.label"));
        lblHealth = new Label("42", new ElementId("health.hud.label"));

        uiContainer.addChild(lblHealth, BorderLayout.Position.West);
        uiContainer.addChild(lblArmor, BorderLayout.Position.Center);
        uiContainer.addChild(lblAmmo, BorderLayout.Position.East);

        uiContainer.setPreferredSize(new Vector3f(app.getCamera().getWidth(), 0.25f * app.getCamera().getHeight(), 0f));
        uiContainer.setLocalTranslation(0f, 0.25f * app.getCamera().getHeight(), 0f);
        hudLabelRoot.attachChild(uiContainer);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
        ((Main)getApplication()).getGuiNode().attachChild(hudLabelRoot);
        playerStats = ed.watchEntity(getState(GameSessionState.class).getCharacterId(), HealthComponent.class, ArmorComponent.class);
    }

    @Override
    protected void onDisable() {
        hudLabelRoot.removeFromParent();
        
        playerStats.release();
        playerStats = null;
    }

    @Override
    public void update( float tpf ) {
        if (playerStats.applyChanges()) { // Update the UI
            lblHealth.setText(String.valueOf(playerStats.get(HealthComponent.class).getHealth()));
            lblArmor.setText(String.valueOf(playerStats.get(ArmorComponent.class).getArmor()));
            //lblAmmo.setText(String.valueOf(playerStats.get(HealthComponent.class).getHealth()));
            System.out.println("Update Changes");
        }
    }
}
