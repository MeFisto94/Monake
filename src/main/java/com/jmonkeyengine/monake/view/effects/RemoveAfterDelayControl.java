/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jmonkeyengine.monake.view.effects;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author wobblytrout
 */
public class RemoveAfterDelayControl extends AbstractControl {

    int delay = 500;
    long startTime = 0l;

    RemoveAfterDelayControl(int delay) {
        this.delay = delay;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (System.currentTimeMillis() - startTime > delay) {
            spatial.removeFromParent();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
