package com.hiroshi.cimoc.utils;

import android.text.TextUtils;

import androidx.collection.LongSparseArray;

import com.hiroshi.cimoc.App;
import com.hiroshi.cimoc.manager.PreferenceManager;
import com.hiroshi.cimoc.model.Comic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Hiroshi on 2017/3/24.
 */

public class ComicUtils {

    protected static PreferenceManager mPreference;

    public static LongSparseArray<Comic> buildComicMap(List<Comic> list) {
        LongSparseArray<Comic> array = new LongSparseArray<>();
        for (Comic comic : list) {
            array.put(comic.getId(), comic);
        }
        return array;
    }

    public static PreferenceManager getPreference() {
        if (mPreference == null) {
            mPreference = App.getAppContext().getPreferenceManager();
        }
        return mPreference;
    }

    public static List<String> getSearchHistory() {
        String str = getPreference().getString("search_history");
        if (str == null) {
            return Collections.emptyList();
        }
        String[] arr = str.split(",");
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(arr));
        return list;
    }

    public static void updateSearchHistory(List<String> list) {
        getPreference().putString("search_history", TextUtils.join(",", list));
    }

}
