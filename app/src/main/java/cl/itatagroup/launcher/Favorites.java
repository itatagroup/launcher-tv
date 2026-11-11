package cl.itatagroup.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Favoritas en orden, guardadas en el equipo. Mientras no se haya tocado ninguna desde la TV, vale
 * la lista de partida de res/values/config.xml (default_favorites).
 */
final class Favorites {
    private static final String PREFS = "inicio";
    private static final String KEY = "favoritas";

    private final SharedPreferences prefs;
    private final List<String> packages = new ArrayList<>();

    Favorites(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY, null);
        if (saved == null) {
            packages.addAll(Arrays.asList(
                    context.getResources().getStringArray(R.array.default_favorites)));
        } else if (!saved.isEmpty()) {
            packages.addAll(Arrays.asList(saved.split(",")));
        }
    }

    List<String> packages() { return packages; }

    boolean contains(String pkg) { return packages.contains(pkg); }

    /** Agrega al final o quita; queda guardado al momento. */
    void toggle(String pkg) {
        if (!packages.remove(pkg)) packages.add(pkg);
        prefs.edit().putString(KEY, TextUtils.join(",", packages)).apply();
    }
}
