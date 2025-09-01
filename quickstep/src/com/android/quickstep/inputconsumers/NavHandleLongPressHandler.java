/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quickstep.inputconsumers;

import static android.os.VibrationEffect.createPredefined;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.launcher3.util.ResourceBasedOverride;
import com.android.launcher3.util.VibrationUtils;
import com.android.launcher3.util.VibratorWrapper;
import com.android.launcher3.Utilities;
import com.android.quickstep.NavHandle;

import android.os.Bundle;
import android.os.IBinder;

import com.android.systemui.shared.system.ActivityManagerWrapper;

import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;

/**
 * Class for extending nav handle long press behavior
 */
public class NavHandleLongPressHandler implements ResourceBasedOverride {

    private final String TAG = "NavHandleLongPressHandler";
    private final boolean DEBUG = false;

    private final Context mContext;

    /** Creates NavHandleLongPressHandler as specified by overrides */
    public NavHandleLongPressHandler(Context context) {
        mContext = context;
    }

    /**
     * Called when nav handle is long pressed to get the Runnable that should be executed by the
     * caller to invoke long press behavior. If null is returned that means long press couldn't be
     * handled.
     * <p>
     * A Runnable is returned here to ensure the InputConsumer can call
     * {@link android.view.InputMonitor#pilferPointers()} before invoking the long press behavior
     * since pilfering can break the long press behavior.
     *
     * @param navHandle to handle this long press
     */
    public @Nullable Runnable getLongPressRunnable(NavHandle navHandle) {
        if (Utilities.isGSAEnabled(mContext)) {
            VibrationUtils.triggerVibration(mContext, 2);
            navHandle.animateNavBarLongPress(
                /*isTouchDown*/ true, /*shrink*/true, /*durationMs*/200);
            if (DEBUG) Log.d(TAG, "getLongPressRunnable: CTS should start now");
            return () -> UI_HELPER_EXECUTOR.execute(() -> {
                startVoiceSession(mContext, 1);
            });
        }
        navHandle.animateNavBarLongPress(
            /*isTouchDown*/false, /*shrink*/ false, /*durationMs*/160);
        return null;
    }

    /**
     * Called when nav handle gesture starts.
     *
     * @param navHandle to handle the animation for this touch
     */
    public void onTouchStarted(NavHandle navHandle) {}

    /**
     * Starts the voice assistant session for Omni with given parameters.
     *
     * @param context Android context (for attribution tag)
     * @param entry_point Entry point code for the session
     * @return showVoiceSession boolean indicating success/failure
     */
    private boolean startVoiceSession(Context context, int entry_point) {
        Bundle bundle = new Bundle();
        bundle.putInt("omni.entry_point", entry_point);
        boolean showVoiceSession = ActivityManagerWrapper.getInstance().showVoiceSession((IBinder) null, bundle, 7, context.getAttributionTag());
        if (!showVoiceSession) {
            if (DEBUG) Log.d(TAG, "startVoiceSession: Omni invocation failed: invocation error");
        }
        return showVoiceSession;
    }

    /**
     * Called when nav handle gesture is finished by the user lifting their finger or the system
     * cancelling the touch for some other reason.
     *
     * @param navHandle to handle the animation for this touch
     * @param reason why the touch ended
     */
    final void onTouchFinished(NavHandle navHandle, String reason) {
        Log.i(TAG, "Contextual Search invocation: touch finished with reason: " + reason);
        navHandle.animateNavBarLongPress(
            /*isTouchDown*/false, /*shrink*/ true, /*durationMs*/200);
    }
}
