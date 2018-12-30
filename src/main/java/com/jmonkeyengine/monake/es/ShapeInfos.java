package com.jmonkeyengine.monake.es;

import com.jmonkeyengine.monake.bullet.ShapeInfo;
import com.simsilica.es.EntityData;

/**
 * See ObjectType and ObjectTypes
 */
public class ShapeInfos {

    public static final String WORLD = "world";
    public static final String BOX = "box";

    public static ShapeInfo worldInfo(EntityData ed) {
        return ShapeInfo.create(WORLD, ed);
    }

    public static ShapeInfo boxInfo(EntityData ed) {
        return ShapeInfo.create(BOX, ed);
    }
}
