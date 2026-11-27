package dev.propulsionteam.propulsionsimulated.debug.routes;

import dev.propulsionteam.propulsionsimulated.debug.IDebugRoute;

public enum MainDebugRoute implements IDebugRoute {
    THRUSTER,
    LIDAR;

    private final IDebugRoute[] children;
    MainDebugRoute(IDebugRoute... children) { this.children = children; }
    MainDebugRoute() { this(new IDebugRoute[0]); }

    @Override public IDebugRoute[] getChildren() { return children; }
}
