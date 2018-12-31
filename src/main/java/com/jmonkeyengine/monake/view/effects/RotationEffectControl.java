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

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.math.Vector3f;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author wobblytrout
 * 
 */
public class RotationEffectControl extends AbstractControl {

    Vector3f rotationAxis = new Vector3f(0, 1, 0);
    float speed = 100.0f;
    float rotationAngle = FastMath.DEG_TO_RAD * 5f;
   
    RotationEffectControl(Vector3f rotationAxis, float speed) {
        this.rotationAxis = rotationAxis.normalizeLocal();
        this.speed = speed;
    }

    @Override
    protected void controlUpdate(float tpf) {        
        float angle = rotationAngle * tpf * speed;
        Quaternion rotate = new Quaternion().fromAngleNormalAxis(angle, rotationAxis);                
        spatial.rotate(rotate);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
