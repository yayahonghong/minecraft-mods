package com.ysh.smartplace;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class DirectionHelper {
    public static Direction fromLookAngle(Vec3 lookAngle) {
        double absX = Math.abs(lookAngle.x);
        double absY = Math.abs(lookAngle.y);
        double absZ = Math.abs(lookAngle.z);

        if (absY >= absX && absY >= absZ) {
            return lookAngle.y > 0 ? Direction.UP : Direction.DOWN;
        } else if (absX >= absZ) {
            return lookAngle.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return lookAngle.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}
