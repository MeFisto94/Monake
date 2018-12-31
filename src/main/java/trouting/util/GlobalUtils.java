/*
 * Wobblytrout
 *
 * Copyright (c) 2018 Wobblytrout
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
package trouting.util;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.simsilica.lemur.GuiGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wobblytrout
 *
 * global utilities function to cleanup some of my commonly used things I've
 * been copy pasting around.
 *
 */
public class GlobalUtils {

    static Logger log = LoggerFactory.getLogger(GlobalUtils.class);

    private static GlobalUtils instance;

    private AssetManager assets;
    private Application application;
    
    public static void initialize(Application app) {
        setInstance(new GlobalUtils(app));
    }

    public static void setInstance(GlobalUtils globals) {
        instance = globals;
        log.info("Initializing GlobalUtils with:" + globals);
    }

    public static GlobalUtils getInstance() {
        return instance;
    }

    protected GlobalUtils(Application app) {
        this.application = app;
        this.assets = app.getAssetManager();
    }

    protected AssetManager getAssetManager() {
        return assets;
    }

    public float getStandardScale() {
        int height = application.getCamera().getHeight();
        return height / 2160f;
    }

    public Geometry createQuad(String buttonTextureLocation, Vector3f startLocation) {
        Texture texture = assets.loadTexture(buttonTextureLocation);
        int textureSizeY = texture.getImage().getHeight();
        int textureSizeX = texture.getImage().getWidth();
        com.jme3.scene.shape.Quad quad = new com.jme3.scene.shape.Quad(textureSizeX, textureSizeY);
        Geometry texturedQuad = new Geometry("quad", quad);
        Material mat = GuiGlobals.getInstance().createMaterial(texture, false).getMaterial();
        mat.setTexture("ColorMap", texture);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setParam("AlphaDiscardThreshold", VarType.Float, 0.1f);
        texturedQuad.setMaterial(mat);
        texturedQuad.setLocalTranslation(startLocation);
        return texturedQuad;
    }

}
