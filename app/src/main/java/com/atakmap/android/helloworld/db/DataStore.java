package com.atakmap.android.squire.db;

import static com.atakmap.android.squire.db.DataStore.DATA_STRUCT_TYPE.LIST;
import static com.atakmap.android.squire.db.DataStore.DATA_STRUCT_TYPE.MAP;
import static com.atakmap.android.squire.db.DataStore.DATA_STRUCT_TYPE.NONE;
import static com.atakmap.android.squire.db.DataStore.DATA_STRUCT_TYPE.SET;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.atakmap.android.squire.models.Report;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

// Used as a generic data structure for Squire reports
// Structures like List that have no key type may pass null values for K safely
public class DataStore<K, V> {
    private static final String TAG = "DataStore";
    enum DATA_STRUCT_TYPE {
        NONE, LIST, SET, MAP;
    }

    final DATA_STRUCT_TYPE type;
    ReentrantLock mutex;
    Collection<V> collection;
    Map<K, V> map;

    // Optional fields used only in MAP case
    String keyMemberName;

    // Collection constructor:
    // structType is passed as a class rather than a reference to a Collection to emphasize
    // that the storage is strictly internal here
    public DataStore(Class structType) {
        mutex = new ReentrantLock();

        if (structType == Map.class) {
            type = MAP;
            throw new InvalidParameterException("Squire Datastore invalid constructor used");
        } else if (structType == List.class) {
            type = LIST;
            collection = new ArrayList<V>();
        } else if (structType == Set.class) {
            type = SET;
            collection = new HashSet<V>();
        } else {
            type = NONE;
        }

        if (type == NONE) {
            throw new InvalidParameterException("Squire Datastore does not support that Collection");
        }
    }

    // Map constructor:
    public DataStore(String keyMemberName) {
        this.mutex = new ReentrantLock();
        this.keyMemberName = keyMemberName;
        this.map = new HashMap<K, V>();
        this.type = MAP;
    }

    // Returns new size post-add, or -1 on failure
    public int add(V data) {
        Log.d(TAG, "Adding data " + data);
        int retVal = -1;

        mutex.lock();
        if (type == SET || type == LIST) {
            collection.add(data);
            retVal = collection.size();

        } else if (type == MAP) {
            try {
                Field field = data.getClass().getDeclaredField(keyMemberName);
                field.setAccessible(true);
                K key = (K) field.get(data);
                map.put(key, data);
                retVal = map.keySet().size();

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                retVal = -1;
            }
        }
        mutex.unlock();
        return retVal;
    }

    public void saveToPrefs(Context ctx) {
        mutex.lock();
        Gson gson = new Gson();
        SharedPreferences preferences = ctx.getSharedPreferences("squire_medevac", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        String json = null;
        if (type == LIST) {
            Type listType = new TypeToken<ArrayList<Report>>(){}.getType();
            json = gson.toJson(collection, listType);
        } else if (type == SET) {
            Type setType = new TypeToken<HashSet<Report>>(){}.getType();
            json = gson.toJson(collection, setType);
        } else if (type == MAP) {
            Type mapType = new TypeToken<HashSet<Map>>(){}.getType();
            json = gson.toJson(map, mapType);
        }
        Log.d(TAG, "Writing prefs " + json);

        if (json != null) {
            editor.putString("reports", json);
            editor.apply();
        }
        mutex.unlock();
    }

    public void loadReportsFromPrefs(Context ctx) {
        mutex.lock();
        Gson gson = new Gson();
        SharedPreferences preferences = ctx.getSharedPreferences("squire_medevac", Context.MODE_PRIVATE);
        String json = preferences.getString("reports", null);
        Type listType = new TypeToken<ArrayList<Report>>(){}.getType();
        if (json != null) {
            collection = gson.fromJson(json, listType);
        }
        Log.d(TAG, "Loaded prefs " + json);
        mutex.unlock();
    }
}
