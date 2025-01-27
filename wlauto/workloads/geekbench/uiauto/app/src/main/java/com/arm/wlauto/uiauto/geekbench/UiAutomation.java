/*    Copyright 2013-2015 ARM Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/


package com.arm.wlauto.uiauto.geekbench;

import android.app.Activity;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.view.KeyEvent;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

// Import the uiautomator libraries

@RunWith(AndroidJUnit4.class)
public class UiAutomation extends UxPerfUiAutomation {

    public static String TAG = "geekbench";
    public static final long WAIT_TIMEOUT_5SEC = TimeUnit.SECONDS.toMillis(5);
    public static final long WAIT_TIMEOUT_20MIN = TimeUnit.SECONDS.toMillis(20 * 60);

@Test
public void runUiAutomation() throws Exception {
        initialize_instrumentation();
        Bundle params = getParams();
        String[] version = params.getString("version").split("\\.");
        int majorVersion = Integer.parseInt(version[0]);
        int minorVersion = Integer.parseInt(version[1]);
        boolean isCorporate = params.getBoolean("is_corporate");
        int times = params.getInt("times");

        if (!isCorporate)
            dismissEula();

        for (int i = 0; i < times; i++) {
            switch (majorVersion) {
                case 2:
                    // In version 2, we scroll through the results WebView to make sure
                    // all results appear on the screen, which causes them to be dumped into
                    // logcat by the Linaro hacks.
                    runBenchmarks();
                    waitForResultsv2();
                    scrollThroughResults();
                    break;
                case 3:
                    runBenchmarks();
                    waitForResultsv3onwards();
                    if (minorVersion < 4) {
                        // Attempting to share the results will generate the .gb3 file with
                        // results that can then be pulled from the device. This is not possible
                        // in verison 2 of Geekbench (Share option was added later).
                        // Sharing is not necessary from 3.4.1 onwards as the .gb3 files are always
                        // created.
                        shareResults();
                    }
                    break;
                case 4:
                    runCpuBenchmarks(isCorporate);
                    waitForResultsv3onwards();
                    break;
                default :
                    throw new RuntimeException("Invalid version of Geekbench requested");
            }

            if (i < (times - 1)) {
                mDevice.pressBack();
                if (majorVersion < 4)
                    mDevice.pressBack();  // twice
            }
        }

        Bundle status = new Bundle();
        mInstrumentation.sendStatus(Activity.RESULT_OK, status);
    }

    public void dismissEula() throws Exception {
        UiObject acceptButton =
            //mDevice.findObject(new UiSelector().textContains("Accept")
           mDevice.findObject(new UiSelector().resourceId("android:id/button1")
                                         .className("android.widget.Button"));
        if (!acceptButton.waitForExists(WAIT_TIMEOUT_5SEC)) {
            throw new UiObjectNotFoundException("Could not find Accept button");
        }
        acceptButton.click();
    }

    public void runBenchmarks() throws Exception {
        UiObject runButton =
           mDevice.findObject(new UiSelector().textContains("Run Benchmarks")
                                         .className("android.widget.Button"));
        if (!runButton.waitForExists(WAIT_TIMEOUT_5SEC)) {
            throw new UiObjectNotFoundException("Could not find Run button");
        }
        runButton.click();
    }

    public void runCpuBenchmarks(boolean isCorporate) throws Exception {
        // The run button is at the bottom of the view and may be off the screen so swipe to be sure
        uiDeviceSwipe(Direction.DOWN, 50);

        String packageName = isCorporate ? "com.primatelabs.geekbench4.corporate"
                                         : "com.primatelabs.geekbench";
        UiObject runButton =
           mDevice.findObject(new UiSelector().resourceId(packageName + ":id/runCpuBenchmarks")
                                         .className("android.widget.Button"));
        if (!runButton.waitForExists(WAIT_TIMEOUT_5SEC)) {
            throw new UiObjectNotFoundException("Could not find Run button");
        }
        runButton.click();
    }

    public void waitForResultsv2() throws Exception {
        UiSelector selector = new UiSelector();
        UiObject resultsWebview = mDevice.findObject(selector.className("android.webkit.WebView"));
        if (!resultsWebview.waitForExists(WAIT_TIMEOUT_20MIN)) {
            throw new UiObjectNotFoundException("Did not see Geekbench results screen.");
        }
    }

    public void waitForResultsv3onwards() throws Exception {
        UiSelector selector = new UiSelector();
        UiObject runningTextView = mDevice.findObject(selector.textContains("Running")
                                                        .className("android.widget.TextView"));

        if (!runningTextView.waitUntilGone(WAIT_TIMEOUT_20MIN)) {
            throw new UiObjectNotFoundException("Did not get to Geekbench results screen.");
        }
    }

    public void scrollThroughResults() throws Exception {
        UiSelector selector = new UiSelector();
        mDevice.pressKeyCode(KeyEvent.KEYCODE_PAGE_DOWN);
        sleep(1);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_PAGE_DOWN);
        sleep(1);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_PAGE_DOWN);
        sleep(1);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_PAGE_DOWN);
    }

    public void shareResults() throws Exception {
        sleep(2); // transition
        UiSelector selector = new UiSelector();
        mDevice.pressMenu();
        UiObject shareButton = mDevice.findObject(selector.text("Share")
                                                    .className("android.widget.TextView"));
        shareButton.waitForExists(WAIT_TIMEOUT_5SEC);
        shareButton.click();
    }
}
