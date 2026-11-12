package cl.itatagroup.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Mantiene apagados los ajustes de envío de datos. Algunas teles los vuelven a encender solas (la Xiaomi
 * Mi TV Stick pone otra vez en 1 {@code user_experience_enabled} y {@code upload_log_pref} en cada arranque):
 * el inicio los revisa al encender la tele y cada vez que se vuelve a él, y corrige los que cambiaron.
 *
 * <p>Necesita el permiso {@code WRITE_SECURE_SETTINGS}, que una app normal no puede pedir: se da por ADB
 * ({@code adb shell pm grant cl.itatagroup.launcher android.permission.WRITE_SECURE_SETTINGS}; lo hace
 * {@code scripts/limpiar-tv.ps1} al instalar el inicio) y se quita con {@code pm revoke}. Sin él, no hace nada.
 * Solo escribe estos ajustes; no lee ni manda nada más. No puede volver a desactivar apps: eso Android solo
 * lo permite a las del sistema.
 *
 * <p>La lista es la de los ajustes del grupo «telemetría» de {@code scripts/catalogo-tv.json}: si cambia una,
 * cambiar la otra.
 */
final class GuardianPrivacidad {
    private static final String TAG = "Guardian";
    private static final String PREFS = "guardian";
    private static final String PREF_ACTIVO = "activo";
    private static final String PREF_CORRECCIONES = "correcciones";
    private static final String PREF_ULTIMA = "ultima";

    /** {espacio, clave, valor}: el valor que se mantiene. */
    private static final String[][] AJUSTES = {
            {"global", "user_experience_enabled", "0"},
            {"secure", "upload_log_pref", "0"},
            {"global", "send_action_app_error", "0"},
            {"global", "upload_apk_enable", "0"},
            {"secure", "limit_ad_tracking", "1"},
            {"global", "network_recommendations_enabled", "0"},
            {"global", "activity_starts_logging_enabled", "0"},
    };

    private GuardianPrivacidad() {}

    /** ¿Tiene el permiso para cambiar estos ajustes? (se da por ADB). */
    static boolean tienePermiso(Context contexto) {
        if (Build.VERSION.SDK_INT < 23) {
            return contexto.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return contexto.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean activo(Context contexto) {
        return prefs(contexto).getBoolean(PREF_ACTIVO, true);
    }

    static void fijarActivo(Context contexto, boolean activo) {
        prefs(contexto).edit().putBoolean(PREF_ACTIVO, activo).apply();
    }

    /** Cuántos ajustes tuvo que volver a apagar desde que se instaló. */
    static int correcciones(Context contexto) {
        return prefs(contexto).getInt(PREF_CORRECCIONES, 0);
    }

    /** Hora (epoch ms) de la última corrección; 0 si nunca hubo. */
    static long ultimaCorreccion(Context contexto) {
        return prefs(contexto).getLong(PREF_ULTIMA, 0);
    }

    /**
     * Revisa y corrige. Devuelve cuántos ajustes cambió. No hace nada si está apagado o sin permiso. Un ajuste
     * que la tele no tiene (null) no se crea: solo se corrige lo que existe con otro valor.
     */
    static int revisar(Context contexto) {
        if (!activo(contexto) || !tienePermiso(contexto)) return 0;
        ContentResolver cr = contexto.getContentResolver();
        int cambiados = 0;
        for (String[] a : AJUSTES) {
            try {
                boolean global = "global".equals(a[0]);
                String actual = global ? Settings.Global.getString(cr, a[1]) : Settings.Secure.getString(cr, a[1]);
                if (actual == null || actual.equals(a[2])) continue;
                boolean ok = global ? Settings.Global.putString(cr, a[1], a[2]) : Settings.Secure.putString(cr, a[1], a[2]);
                if (ok) {
                    cambiados++;
                    Log.i(TAG, a[0] + " " + a[1] + " estaba en " + actual + ": vuelto a " + a[2]);
                }
            } catch (SecurityException e) {
                Log.w(TAG, "sin permiso para " + a[1] + ": " + e.getMessage());
                return cambiados;
            } catch (RuntimeException e) {
                Log.w(TAG, "no se pudo revisar " + a[1] + ": " + e);
            }
        }
        if (cambiados > 0) {
            SharedPreferences p = prefs(contexto);
            p.edit().putInt(PREF_CORRECCIONES, p.getInt(PREF_CORRECCIONES, 0) + cambiados)
                    .putLong(PREF_ULTIMA, System.currentTimeMillis()).apply();
        }
        return cambiados;
    }

    /** Revisa fuera del hilo de la interfaz (son pocas lecturas, pero tocan el proveedor de ajustes). */
    static void revisarEnSegundoPlano(final Context contexto) {
        final Context app = contexto.getApplicationContext();
        new Thread(() -> revisar(app), "guardian").start();
    }

    private static SharedPreferences prefs(Context contexto) {
        return contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
