/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.android.settings.search.FakeIndexProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeSliderController;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import androidx.slice.core.SliceHints;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceBroadcastReceiverTest {

    private final String fakeTitle = "title";
    private final String fakeSummary = "summary";
    private final String fakeScreenTitle = "screen_title";
    private final int fakeIcon = 1234;
    private final String fakeFragmentClassName = FakeIndexProvider.class.getName();
    private final Class fakeControllerName = FakeToggleController.class;

    private Context mContext;
    private SQLiteDatabase mDb;
    private SliceBroadcastReceiver mReceiver;
    private SearchFeatureProvider mSearchFeatureProvider;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mDb = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        mReceiver = new SliceBroadcastReceiver();
        SlicesDatabaseHelper helper = SlicesDatabaseHelper.getInstance(mContext);
        helper.setIndexedState();
        mSearchFeatureProvider = new SearchFeatureProviderImpl();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = mSearchFeatureProvider;
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testOnReceive_toggleChanged() {
        String key = "key";
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);
        // Turn on toggle setting
        FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);
        Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        assertThat(fakeToggleController.isChecked()).isTrue();

        // Toggle setting
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeToggleController.isChecked()).isFalse();
    }

    @Test
    public void testOnReceive_sliderChanged() {
        String key = "key";
        final int position = FakeSliderController.MAX_STEPS - 1;
        final int oldPosition = FakeSliderController.MAX_STEPS;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeSliderController.class, key);

        // Set slider setting
        FakeSliderController fakeSliderController = new FakeSliderController(mContext, key);
        fakeSliderController.setSliderPosition(oldPosition);
        // Build action
        Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED);
        intent.putExtra(SliceHints.EXTRA_RANGE_VALUE, position);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(oldPosition);

        // Update the setting.
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(position);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnReceive_invalidController_throwsException() {
        String key = "key";
        final int position = 0;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeToggleController.class, key);

        // Build action
        Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED);
        intent.putExtra(SliceHints.EXTRA_RANGE_VALUE, position);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        // Trigger the exception.
        mReceiver.onReceive(mContext, intent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sliderOnReceive_noKey_throwsException() {
        // Build action
        Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED);
        intent.putExtra(SliceHints.EXTRA_RANGE_VALUE, 0);

        // Trigger the exception.
        mReceiver.onReceive(mContext, intent);
    }

    @Test(expected =  IllegalStateException.class)
    public void toggleOnReceive_noExtra_illegalStateException() {
        Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        mReceiver.onReceive(mContext, intent);
    }

    @Test(expected =  IllegalStateException.class)
    public void toggleOnReceive_emptyKey_throwsIllegalStateException() {
        Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, (String) null);
        mReceiver.onReceive(mContext, intent);
    }

    private void insertSpecialCase(String key) {
        insertSpecialCase(fakeControllerName, key);
    }

    private void insertSpecialCase(Class controllerClass, String key) {
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, fakeTitle);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, fakeSummary);
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, fakeScreenTitle);
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, fakeIcon);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, fakeFragmentClassName);
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, controllerClass.getName());
        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
    }
}