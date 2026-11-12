package cl.itatagroup.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Al terminar de arrancar la tele: el guardián de privacidad revisa los ajustes de envío de datos, que algunas
 * teles vuelven a encender en cada arranque (ver {@link GuardianPrivacidad}).
 */
public class AlEncender extends BroadcastReceiver {
    @Override
    public void onReceive(Context contexto, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final PendingResult pendiente = goAsync();
        final Context app = contexto.getApplicationContext();
        new Thread(() -> {
            try {
                GuardianPrivacidad.revisar(app);
            } finally {
                pendiente.finish();
            }
        }, "guardian-arranque").start();
    }
}
