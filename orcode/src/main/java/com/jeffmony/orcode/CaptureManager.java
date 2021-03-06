package com.jeffmony.orcode;

import com.jeffmony.orcode.camera.CameraManager;

public interface CaptureManager {

    /**
     * Get {@link CameraManager}
     * @return {@link CameraManager}
     */
    CameraManager getCameraManager();

    /**
     * Get {@link BeepManager}
     * @return {@link BeepManager}
     */
    BeepManager getBeepManager();

    /**
     * Get {@link AmbientLightManager}
     * @return {@link AmbientLightManager}
     */
    AmbientLightManager getAmbientLightManager();

    /**
     * Get {@link InactivityTimer}
     * @return {@link InactivityTimer}
     */
    InactivityTimer getInactivityTimer();
}
