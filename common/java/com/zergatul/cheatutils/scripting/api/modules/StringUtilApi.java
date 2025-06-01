package com.zergatul.cheatutils.scripting.api.modules;

import com.zergatul.cheatutils.utils.MathUtils;

import java.util.Locale;

public class StringUtilApi {
    public boolean contains(String target, String value) {
        return target != null && target.contains(value);
    }
}
