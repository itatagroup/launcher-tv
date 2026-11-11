package cl.itatagroup.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Arma la lista de apps del inicio: las de TV primero y luego las normales, sin repetir paquete,
 * ordenadas por nombre, y al final una entrada fija a los Ajustes del sistema (que no tienen
 * ícono de launcher en la TV, y sin ella no habría cómo abrirlos desde el control remoto).
 */
final class AppCatalog {
    private AppCatalog() {}

    static List<AppEntry> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Set<String> hidden = new HashSet<>(Arrays.asList(
                context.getResources().getStringArray(R.array.hidden_packages)));
        hidden.add(context.getPackageName());

        List<AppEntry> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        add(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, hidden, seen, apps);
        add(pm, Intent.CATEGORY_LAUNCHER, hidden, seen, apps);

        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.toString().compareToIgnoreCase(b.label.toString());
            }
        });

        AppEntry settings = settingsEntry(context, pm, hidden, seen);
        if (settings != null) apps.add(settings);
        return apps;
    }

    private static void add(PackageManager pm, String category, Set<String> hidden,
                            Set<String> seen, List<AppEntry> out) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            String pkg = info.activityInfo.packageName;
            if (hidden.contains(pkg) || seen.contains(pkg)) continue;
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) continue;
            seen.add(pkg);
            out.add(new AppEntry(pkg, info.loadLabel(pm), launch, info));
        }
    }

    private static AppEntry settingsEntry(Context context, PackageManager pm, Set<String> hidden,
                                          Set<String> seen) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResolveInfo info = pm.resolveActivity(intent, 0);
        if (info == null) return null;
        String pkg = info.activityInfo.packageName;
        if (hidden.contains(pkg) || seen.contains(pkg)) return null;
        return new AppEntry(pkg, context.getString(R.string.settings), intent, info);
    }
}
