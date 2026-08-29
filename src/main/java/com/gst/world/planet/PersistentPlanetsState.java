package com.gst.world.planet;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashSet;
import java.util.Set;

/**
 * "gst:planets" dimension'ı için hangi hücrelerin (gezegenlerin) kalıcı
 * olarak işaretlendiğini (tabela konduğunu) tutar.
 *
 * Bu sette OLMAYAN hücrelerin chunk'ları disk'e yazılmaz (bkz. ileride
 * eklenecek chunk-save mixin'i) — yani oyuncu ayrıldığında o gezegen
 * bir sonraki ziyarette seed'den taze üretilir, yapılan değişiklikler kaybolur.
 */
public class PersistentPlanetsState extends PersistentState {

    private static final String STORAGE_KEY = "gst_persistent_planets";

    private final Set<Long> markedCells = new HashSet<>();

    public boolean isMarked(int cellX, int cellZ) {
        return markedCells.contains(PlanetGridManager.packCell(cellX, cellZ));
    }

    /** @return true ise hücre yeni işaretlendi, false ise zaten işaretliydi */
    public boolean mark(int cellX, int cellZ) {
        boolean added = markedCells.add(PlanetGridManager.packCell(cellX, cellZ));
        if (added) {
            markDirty();
        }
        return added;
    }

    /** @return true ise işaret kaldırıldı, false ise zaten işaretli değildi */
    public boolean unmark(int cellX, int cellZ) {
        boolean removed = markedCells.remove(PlanetGridManager.packCell(cellX, cellZ));
        if (removed) {
            markDirty();
        }
        return removed;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (long packedCell : markedCells) {
            list.add(NbtLong.of(packedCell));
        }
        nbt.put("markedCells", list);
        return nbt;
    }

    private static PersistentPlanetsState createFromNbt(NbtCompound nbt) {
        PersistentPlanetsState state = new PersistentPlanetsState();
        NbtList list = nbt.getList("markedCells", NbtElement.LONG_TYPE);
        for (int i = 0; i < list.size(); i++) {
            state.markedCells.add(((NbtLong) list.get(i)).longValue());
        }
        return state;
    }

    /**
     * "gst:planets" dimension'ının ServerWorld'ünü vererek bu state'e eriş.
     * Yoksa otomatik oluşturulur.
     */
    public static PersistentPlanetsState get(ServerWorld planetsWorld) {
        PersistentStateManager manager = planetsWorld.getPersistentStateManager();
        return manager.getOrCreate(PersistentPlanetsState::createFromNbt, PersistentPlanetsState::new, STORAGE_KEY);
    }
}