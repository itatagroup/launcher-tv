package cl.itatagroup.launcher;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** Una app lanzable: su nombre, el intent que la abre y sus imágenes (cargadas una sola vez). */
final class AppEntry {
    final String packageName;
    final CharSequence label;
    final Intent intent;
    /** Las apps del sistema no se pueden desinstalar desde el inicio. */
    final boolean system;
    private final ResolveInfo info;
    private Drawable icon;

    AppEntry(String packageName, CharSequence label, Intent intent, ResolveInfo info) {
        this.packageName = packageName;
        this.label = label;
        this.intent = intent;
        this.info = info;
        this.system = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    Drawable icon(PackageManager pm) {
        if (icon == null) icon = info.loadIcon(pm);
        return icon;
    }

    /**
     * El banner 16:9 de TV (de la actividad o, si no tiene, de la app) reducido al tamaño de su
     * tarjeta; null si no hay. Algunas apps traen banners de alta resolución: guardar el original
     * costaba ~20 MB entre memoria y GPU con 12 apps; reducido son ~2,5 MB. No se guarda aquí: lo
     * guarda cada cuadrícula (AppsAdapter), que conoce su tamaño de tarjeta.
     */
    Drawable loadBanner(PackageManager pm, Resources res, int width, int height) {
        Drawable original = info.activityInfo.loadBanner(pm);
        return original == null ? null : new BitmapDrawable(res, fit(original, width, height));
    }

    /** Dibuja el banner recortado al centro (como centerCrop) en un bitmap del tamaño justo. */
    private static Bitmap fit(Drawable d, int width, int height) {
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int iw = d.getIntrinsicWidth();
        int ih = d.getIntrinsicHeight();
        if (iw <= 0 || ih <= 0) {
            d.setBounds(0, 0, width, height);
        } else {
            float scale = Math.max((float) width / iw, (float) height / ih);
            int w = Math.round(iw * scale);
            int h = Math.round(ih * scale);
            int left = (width - w) / 2;
            int top = (height - h) / 2;
            d.setBounds(left, top, left + w, top + h);
        }
        d.draw(new Canvas(out));
        return out;
    }
}
