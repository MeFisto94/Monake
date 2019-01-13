/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jmonkeyengine.monake;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.ExtrasLoader;
import com.jme3.scene.plugins.gltf.GltfLoader;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GltfConvertWithUserData extends SimpleApplication {

    static Logger log = LoggerFactory.getLogger(GltfConvertWithUserData.class);

    public static void main(String[] args) {
        GltfConvertWithUserData app = new GltfConvertWithUserData();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(0, -1, -1).normalizeLocal());
        rootNode.addLight(dl);

        convertGltf("Scenes/quakestartleveltextured_nosky.gltf", "assets/Models/level.j3o");
        //convertGltf("Scenes/single_shotgun.gltf", "assets/Models/single_shotgun.j3o");

    }

    private void convertGltf(String inputPath, String outputPath) {
        GltfModelKey modelKey = new GltfModelKey(inputPath);
        ExtrasLoader extras = new ExtrasLoadererer();
        modelKey.setExtrasLoader(extras);

        Spatial model = assetManager.loadModel(modelKey);

        model.breadthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry){
                Geometry geo = (Geometry)spatial;
                if (geo.getMaterial().getMaterialDef().getAssetName().equals("Common/MatDefs/Light/PBRLighting.j3md")){
                    geo.getMaterial().setBoolean("UseSpecGloss", false);
                }
            }
        });

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BinaryExporter exp = new BinaryExporter();
            exp.save(model, baos);

            try (OutputStream outputStream = new FileOutputStream(outputPath)) {
                baos.writeTo(outputStream);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private class ExtrasLoadererer implements ExtrasLoader {

        @Override
        public Object handleExtras(GltfLoader loader, String parentName, JsonElement parent, JsonElement extras, Object input) {
            // if its a geometry, we want to add all the geometry userdata 

            if (input instanceof Spatial) {
                if (extras.isJsonObject()) {
                    JsonObject ext = extras.getAsJsonObject();
                    for (Entry<String, JsonElement> element : ext.entrySet()) {
                        Spatial spatial = (Spatial) input;
                        spatial.setUserData(element.getKey(), element.getValue().getAsString());
                    }
                }
            }
            return input;
        }

    }

}
