package cl.itatagroup.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de inicio: hora y fecha, una fila de favoritas y el resto de las apps, sin tienda,
 * recomendaciones ni publicidad. Mantener OK sobre una app abre su menú (favorita, información,
 * desinstalar y buscar una actualización del propio inicio).
 */
public class Home extends Activity {
    /** Manifiesto firmado del último release (contrato común de actualización). Solo se pide a mano. */
    private static final String MANIFIESTO =
            "https://github.com/itatagroup/launcher-tv/releases/latest/download/version.json";

    private AppsAdapter favoritesAdapter;
    private AppsAdapter appsAdapter;
    private GridView favoritesGrid;
    private View favoritesTitle;
    private Favorites favorites;
    private Actualizador actualizador;
    private AlertDialog downloadDialog;
    private android.widget.Button botonTelemetria;

    /** La lista solo se rearma cuando se instala, quita o cambia una app, no en cada resume. */
    private final BroadcastReceiver packageChanges = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data != null) {
                favoritesAdapter.forget(data.getSchemeSpecificPart());
                appsAdapter.forget(data.getSchemeSpecificPart());
            }
            reload();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        favorites = new Favorites(this);
        actualizador = new Actualizador(this, "itatagroup/launcher-tv", "launcher-android", "Inicio");
        favoritesAdapter = new AppsAdapter(this, R.dimen.fav_width, R.dimen.fav_height);
        appsAdapter = new AppsAdapter(this, R.dimen.app_width, R.dimen.app_height);
        favoritesTitle = findViewById(R.id.favorites_title);
        favoritesGrid = setUp(R.id.favorites, favoritesAdapter);
        setUp(R.id.apps, appsAdapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        // Son avisos del sistema: llegan igual sin exportar el receptor (Android 13+ exige decirlo).
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(packageChanges, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(packageChanges, filter);

        reload();
        // Botones de la cabecera: lo del propio inicio, a la vista (no en el menú de cada app).
        botonTelemetria = (android.widget.Button) findViewById(R.id.boton_telemetria);
        botonTelemetria.setOnClickListener(v -> showGuardian());
        findViewById(R.id.boton_actualizar).setOnClickListener(v -> checkForUpdate());
        View first = favoritesAdapter.getCount() > 0 ? favoritesGrid : findViewById(R.id.apps);
        first.requestFocus();
    }

    private GridView setUp(int id, final AppsAdapter adapter) {
        GridView grid = (GridView) findViewById(id);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
                launch(adapter.getItem(position));
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long rowId) {
                showMenu(adapter.getItem(position));
                return true;
            }
        });
        return grid;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packageChanges);
        actualizador.cancelar();
        closeDownloadDialog();
        super.onDestroy();
    }

    /** Es el inicio: Atrás no debe salir de aquí. */
    @Override
    public void onBackPressed() {}

    /** Favoritas en su orden guardado; el resto, alfabético y sin repetirlas. */
    private void reload() {
        List<AppEntry> all = AppCatalog.load(this);
        Map<String, AppEntry> byPackage = new HashMap<>();
        for (AppEntry app : all) byPackage.put(app.packageName, app);

        List<AppEntry> favs = new ArrayList<>();
        for (String pkg : favorites.packages()) {
            AppEntry app = byPackage.get(pkg);
            if (app != null) favs.add(app);
        }
        List<AppEntry> rest = new ArrayList<>();
        for (AppEntry app : all) {
            if (!favorites.contains(app.packageName)) rest.add(app);
        }
        favoritesAdapter.setApps(favs);
        appsAdapter.setApps(rest);
        int visibility = favs.isEmpty() ? View.GONE : View.VISIBLE;
        favoritesTitle.setVisibility(visibility);
        favoritesGrid.setVisibility(visibility);
        // Sin favoritas, desde «Todas las apps» la flecha arriba lleva a los botones de la cabecera.
        findViewById(R.id.apps).setNextFocusUpId(favs.isEmpty() ? R.id.boton_telemetria : R.id.favorites);
    }

    private void launch(AppEntry app) {
        start(app.intent);
    }

    private void start(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            // La app se desactivó o desinstaló después de listarse: se avisa y se rearma la lista.
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
            reload();
        }
    }

    /**
     * Menú de mantener OK. «Desinstalar» se muestra siempre; en las apps del sistema va bloqueado
     * con el motivo, no escondido.
     */
    private void showMenu(final AppEntry app) {
        final boolean favorite = favorites.contains(app.packageName);
        String[] options = {
                getString(favorite ? R.string.menu_remove_favorite : R.string.menu_add_favorite),
                getString(R.string.menu_info),
                getString(app.system ? R.string.menu_uninstall_blocked : R.string.menu_uninstall),
        };
        ArrayAdapter<String> items = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, options) {
            @Override
            public boolean areAllItemsEnabled() { return !app.system; }

            @Override
            public boolean isEnabled(int position) { return position != 2 || !app.system; }

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                row.setAlpha(isEnabled(position) ? 1f : 0.45f);
                return row;
            }
        };
        new AlertDialog.Builder(this, R.style.Menu)
                .setTitle(app.label)
                .setAdapter(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.fromParts("package", app.packageName, null);
                        if (which == 0) {
                            favorites.toggle(app.packageName);
                            reload();
                        } else if (which == 1) {
                            start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri));

                        } else if (!app.system) {
                            // Diálogo de confirmación del sistema; al quitarla, la lista se rearma sola.
                            start(new Intent(Intent.ACTION_DELETE, uri));
                        }
                    }
                })
                .show();
    }

    /** Revisa la telemetría cada vez que se vuelve al inicio (también al encender: ver AlEncender). */
    @Override
    protected void onResume() {
        super.onResume();
        GuardianPrivacidad.revisarEnSegundoPlano(this);
        pintarBotonTelemetria();
    }

    private void pintarBotonTelemetria() {
        botonTelemetria.setText(!GuardianPrivacidad.tienePermiso(this) ? R.string.boton_telemetria_sin_permiso
                : GuardianPrivacidad.activo(this) ? R.string.boton_telemetria_si : R.string.boton_telemetria_no);
    }

    /** Estado del guardián de privacidad, con su interruptor; sin permiso, explica cómo darlo. */
    private void showGuardian() {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(this, R.style.Menu).setTitle(R.string.guardian_titulo);
        if (!GuardianPrivacidad.tienePermiso(this)) {
            dialogo.setMessage(R.string.guardian_permiso).setPositiveButton(R.string.guardian_cerrar, null).show();
            return;
        }
        final boolean activo = GuardianPrivacidad.activo(this);
        long ultima = GuardianPrivacidad.ultimaCorreccion(this);
        String cuando = ultima == 0 ? "" : getString(R.string.guardian_ultima,
                android.text.format.DateFormat.getMediumDateFormat(this).format(new java.util.Date(ultima)) + " "
                        + android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date(ultima)));
        dialogo.setMessage(getString(R.string.guardian_estado, GuardianPrivacidad.correcciones(this), cuando))
                .setPositiveButton(activo ? R.string.guardian_apagar : R.string.guardian_encender, (d, w) -> {
                    GuardianPrivacidad.fijarActivo(Home.this, !activo);
                    if (!activo) GuardianPrivacidad.revisarEnSegundoPlano(Home.this);
                    pintarBotonTelemetria();
                })
                .setNegativeButton(R.string.guardian_cerrar, null)
                .show();
    }

    // --- actualización del propio inicio (ver docs/actualizaciones.md) ---

    /** Única forma en que el inicio se conecta a internet: cuando eliges esta opción. */
    private void checkForUpdate() {
        Toast.makeText(this, R.string.update_checking, Toast.LENGTH_SHORT).show();
        actualizador.comprobar(MANIFIESTO, new Actualizador.AlComprobar() {
            @Override
            public void hayVersion(Actualizador.Oferta oferta) {
                offerUpdate(oferta);
            }

            @Override
            public void alDia() {
                Toast.makeText(Home.this, getString(R.string.update_current, actualizador.versionNombre()),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void fallo() {
                // Sin detalles técnicos en pantalla: el motivo queda en el registro (logcat, «Actualizador»).
                Toast.makeText(Home.this, R.string.update_check_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void offerUpdate(final Actualizador.Oferta oferta) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this, R.style.Menu)
                .setTitle(getString(R.string.update_available, oferta.version))
                .setMessage(oferta.notas.isEmpty() ? getString(R.string.update_no_notes) : oferta.notas)
                .setPositiveButton(R.string.update_now, (dialog, which) -> update(oferta))
                .setNegativeButton(R.string.update_later, null)
                .show();
    }

    private void update(final Actualizador.Oferta oferta) {
        if (!actualizador.puedeInstalar()) {
            // Android 8+: cada app necesita el permiso «instalar apps desconocidas»; se explica antes de abrirlo.
            new AlertDialog.Builder(this, R.style.Menu)
                    .setTitle(R.string.update_permission_title)
                    .setMessage(R.string.update_permission)
                    .setPositiveButton(R.string.update_permission_open, (dialog, which) -> {
                        if (!Actualizador.abrirPermisoDeInstalacion(Home.this)) {
                            Toast.makeText(Home.this, R.string.update_permission_missing, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(R.string.update_later, null)
                    .show();
            return;
        }
        closeDownloadDialog();
        downloadDialog = new AlertDialog.Builder(this, R.style.Menu)
                .setTitle(getString(R.string.update_available, oferta.version))
                .setMessage(getString(R.string.update_downloading, oferta.version, 0))
                .setNegativeButton(R.string.update_cancel, (dialog, which) -> actualizador.cancelar())
                .setOnCancelListener(dialog -> actualizador.cancelar())
                .show();
        actualizador.descargarEInstalar(oferta, new Actualizador.AlDescargar() {
            @Override
            public void avance(int porcentaje) {
                if (downloadDialog != null) {
                    downloadDialog.setMessage(getString(R.string.update_downloading, oferta.version, porcentaje));
                }
            }

            @Override
            public void instalando() {
                closeDownloadDialog();
                Toast.makeText(Home.this, R.string.update_confirm, Toast.LENGTH_LONG).show();
            }

            @Override
            public void fallo() {
                closeDownloadDialog();
                Toast.makeText(Home.this, R.string.update_download_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void closeDownloadDialog() {
        if (downloadDialog != null && downloadDialog.isShowing()) downloadDialog.dismiss();
        downloadDialog = null;
    }
}
