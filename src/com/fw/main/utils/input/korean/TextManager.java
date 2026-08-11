package com.fw.main.utils.input.korean;

import com.fw.internal.utils.Internal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TextManager {
    public static Map<UUID, TextObject> koreanObjectMap = new ConcurrentHashMap<>();
    public static Map<UUID, TextObject> activeObjectsMap = new ConcurrentHashMap<>();
    public static void koreanObjectPut(TextObject o) {koreanObjectMap.put(o.id,o);}
    public static void activeObjectPut(TextObject o) {activeObjectsMap.put(o.id,o);}
    public static void koreanObjectRemove(TextObject o) {koreanObjectMap.remove(o.id);}
    public static void activeObjectRemove(TextObject o) {activeObjectsMap.remove(o.id);}

    @Internal
    public static boolean isActiveKoreanObjectIsEmpty() { return activeObjectsMap.isEmpty(); }
}
