package com.jmonkeyengine.monake.sim;

import com.jme3.app.Application;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jmonkeyengine.monake.bullet.CollisionShapes;
import com.jmonkeyengine.monake.bullet.ShapeInfo;
import com.jmonkeyengine.monake.es.ShapeInfos;
import com.jmonkeyengine.monake.util.server.ServerApplication;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;

/**
 * This class is responsible for passing/creating the right CollisionShapes based on some strings.
 * The reason for this complication is, that the Server needs the CollisionShapes but only the client loads the
 * Meshes/Spatials usually.
 */
public class CollisionShapeProvider {
    static EntityData entityData;

    public static void registerDefaults(EntityData ed, CollisionShapes shapes) {
        if (entityData == null) {
            entityData = ed;
        }
        shapes.register(ShapeInfos.worldInfo(ed), lookup(ShapeInfos.worldInfo(ed)));
    }

    public static CollisionShape lookup(ShapeInfo shapeInfo) {
        if (entityData == null) {
            throw new IllegalStateException("Can't be!");
        }

        // @TODO: Is there a better way?
        if (shapeInfo.getShapeId() == ShapeInfos.worldInfo(entityData).getShapeId()) {
            // Taken from RigidBodyControl code :D
            try {
                Spatial spatial = ServerApplication.self.getAssetManager().loadModel("Models/level.j3o");
                return CollisionShapeFactory.createMeshShape(spatial);
            } catch (AssetNotFoundException anf) {
                return new BoxCollisionShape(new Vector3f(64f, 0.5f, 64f)); // See ModelViewState
            }
        } else if (shapeInfo.getShapeId() == ShapeInfos.boxInfo(entityData).getShapeId()) {
            return new BoxCollisionShape(new Vector3f(1f, 1f, 1f));
        } else if (shapeInfo.getShapeId() == ShapeInfos.playerInfo(entityData).getShapeId()) {
            return new CapsuleCollisionShape(0.5f, 4f);
        }

        return null;
    }

    public static Geometry getPlayerCollisionShapeAsGeometry(Application app, EntityData ed, float playerYOffset) {
        Geometry physicsCapsule = new Geometry("", new Cylinder(32, 32, 0.5f, 4f));
        physicsCapsule.setLocalTranslation(0f, 4f / 2f + playerYOffset, 0f); // height / 2 -> origin = center of capsule, -1.2f because Jaime
        physicsCapsule.setLocalRotation(new Quaternion(new float[] { FastMath.HALF_PI, 0f, 0f}));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Pink);
        mat.getAdditionalRenderState().setWireframe(true);
        physicsCapsule.setMaterial(mat);

        return physicsCapsule;
    }
}
