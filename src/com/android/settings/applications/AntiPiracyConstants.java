/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

/* A constants class list for known piracy apps. Please report new piracy
 * apps to ROM developers deploying this code.
 */
class AntiPiracyConstants {
    static final boolean DEBUG = false;

    static final String[] PACKAGES = {
        // Package names                          // App names
        "com.dimonvideo.luckypatcher",            // Lucky patcher
        "com.chelpus.lackypatch",                 // Lucky patcher secondary
        "com.blackmartalpha",                     // Black Mart alpha
        "org.blackmart.market",                   // Black Mart
    };
}
