package com.jmonkeyengine.monake.es;

import com.jmonkeyengine.monake.bullet.ShapeInfo;
import com.simsilica.es.EntityData;

/**
 * See ObjectType and ObjectTypes
 */
public class ShapeInfos {

    public static final String WORLD = "world";
    public static final String BOX = "box";
    public static final String SPHERE = "sphere";
    public static final String PLAYER = "player";

    public static ShapeInfo worldInfo(EntityData ed) {
        return ShapeInfo.create(WORLD, ed);
    }

    public static ShapeInfo boxInfo(EntityData ed) {
        return ShapeInfo.create(BOX, ed);
    }

    public static ShapeInfo sphereInfo(EntityData ed) {
        return ShapeInfo.create(SPHERE, ed);
    }

    public static ShapeInfo playerInfo(EntityData ed) {
        return ShapeInfo.create(PLAYER, ed);
    }
}
