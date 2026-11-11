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
 * desinstalar).
 */
public class Home extends Activity {
    private AppsAdapter favoritesAdapter;
    private AppsAdapter appsAdapter;
    private GridView favoritesGrid;
    private View favoritesTitle;
    private Favorites favorites;

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
        registerReceiver(packageChanges, filter);

        reload();
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
}
