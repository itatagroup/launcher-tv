package cl.itatagroup.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tarjetas de una cuadrícula; el tamaño (favoritas grandes o el resto chicas) lo fija quien la crea. */
final class AppsAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final PackageManager pm;
    private final Resources res;
    private final int width;
    private final int height;
    private final List<AppEntry> apps = new ArrayList<>();
    /**
     * Banners ya reducidos, por paquete. Sobreviven a reload() (marcar una favorita no vuelve a
     * decodificar todo); los de un paquete que cambió los descarta forget(). Un banner que no
     * existe se guarda como NONE para no buscarlo de nuevo.
     */
    private final Map<String, Drawable> banners = new HashMap<>();
    private static final Drawable NONE = new android.graphics.drawable.ColorDrawable();

    AppsAdapter(Context context, int widthRes, int heightRes) {
        this.inflater = LayoutInflater.from(context);
        this.pm = context.getPackageManager();
        this.res = context.getResources();
        this.width = context.getResources().getDimensionPixelSize(widthRes);
        this.height = context.getResources().getDimensionPixelSize(heightRes);
    }

    /** Una app se instaló, actualizó o quitó: su banner puede haber cambiado. */
    void forget(String packageName) {
        banners.remove(packageName);
    }

    void setApps(List<AppEntry> newApps) {
        apps.clear();
        apps.addAll(newApps);
        // Suelta los banners de las apps que ya no están aquí (p. ej. pasaron a favoritas).
        java.util.Set<String> present = new java.util.HashSet<>();
        for (AppEntry app : newApps) present.add(app.packageName);
        banners.keySet().retainAll(present);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return apps.size(); }
    @Override public AppEntry getItem(int position) { return apps.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            convertView.setLayoutParams(new AbsListView.LayoutParams(
                    width, AbsListView.LayoutParams.WRAP_CONTENT));
            h = new Holder(convertView);
            h.card.getLayoutParams().width = width;
            h.card.getLayoutParams().height = height;
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        AppEntry app = apps.get(position);
        h.label.setText(app.label);
        Drawable banner = banners.get(app.packageName);
        if (banner == null) {
            banner = app.loadBanner(pm, res, width, height);
            if (banner == null) banner = NONE;
            banners.put(app.packageName, banner);
        }
        if (banner == NONE) banner = null;
        boolean hasBanner = banner != null;
        h.banner.setImageDrawable(banner);
        h.banner.setVisibility(hasBanner ? View.VISIBLE : View.GONE);
        h.icon.setImageDrawable(hasBanner ? null : app.icon(pm));
        h.icon.setVisibility(hasBanner ? View.GONE : View.VISIBLE);
        return convertView;
    }

    private static final class Holder {
        final View card;
        final ImageView banner;
        final ImageView icon;
        final TextView label;

        Holder(View root) {
            card = root.findViewById(R.id.card);
            card.setClipToOutline(true); // el banner toma las esquinas redondeadas de la tarjeta
            banner = (ImageView) root.findViewById(R.id.banner);
            icon = (ImageView) root.findViewById(R.id.icon);
            icon.setClipToOutline(true); // recorta con el contorno de res/drawable/icon_clip.xml
            label = (TextView) root.findViewById(R.id.label);
        }
    }
}
