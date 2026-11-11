package cl.itatagroup.launcher;

import android.service.dreams.DreamService;
import android.view.View;
import java.util.Random;

/**
 * Salvapantallas: hora y fecha tenues sobre negro, que cambian de lugar cada minuto para no marcar
 * la pantalla. Corre en el mismo proceso que el inicio y no usa red ni imágenes.
 */
public class ClockDream extends DreamService {
    private static final long MOVE_EVERY_MS = 60_000;

    private final Random random = new Random();
    private View block;

    private final Runnable move = new Runnable() {
        @Override
        public void run() {
            View root = (View) block.getParent();
            if (block.getWidth() == 0 || root.getWidth() == 0) {
                block.post(this); // todavía sin medir: sin tamaños, el reloj podría quedar fuera de la pantalla
                return;
            }
            int maxX = Math.max(0, root.getWidth() - block.getWidth());
            int maxY = Math.max(0, root.getHeight() - block.getHeight());
            block.setTranslationX(random.nextInt(maxX + 1));
            block.setTranslationY(random.nextInt(maxY + 1));
            block.postDelayed(this, MOVE_EVERY_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false); // cualquier tecla lo cierra
        setFullscreen(true);
        setContentView(R.layout.dream_clock);
        block = findViewById(R.id.dream_block);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        // Después del primer dibujo ya se conocen los tamaños para elegir un lugar al azar.
        block.post(move);
    }

    @Override
    public void onDreamingStopped() {
        block.removeCallbacks(move);
        super.onDreamingStopped();
    }
}
