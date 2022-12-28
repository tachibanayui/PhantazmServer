package org.phantazm.zombies.scoreboard.sidebar;

import com.github.steanky.element.core.annotation.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.Activable;
import org.phantazm.zombies.scoreboard.sidebar.section.SidebarSection;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Model("zombies.sidebar.updater")
public class SidebarUpdater implements Activable {

    private final Sidebar sidebar;
    private final List<SidebarSection> sections;
    private final int[] sizes;
    private int totalSize = 0;

    @FactoryMethod
    public SidebarUpdater(@NotNull Data data, @NotNull Sidebar sidebar,
            @NotNull @Child("sections") Collection<SidebarSection> sections) {
        this.sidebar = Objects.requireNonNull(sidebar, "sidebar");
        this.sections = List.copyOf(sections);
        this.sizes = new int[sections.size()];
    }

    @Override
    public void start() {
        for (int i = 0; i < sizes.length; i++) {
            int size = sections.get(i).getSize();
            sizes[i] = size;
            totalSize += size;
        }

        for (SidebarSection section : sections) {
            section.invalidateCache();
        }

        int clampedSize = Math.min(totalSize, 15);
        for (int i = 0; i < clampedSize; i++) {
            sidebar.createLine(new Sidebar.ScoreboardLine(lineId(i), Component.empty(), clampedSize - i));
        }
    }

    @Override
    public void tick(long time) {
        refreshSections();
        for (int i = 0; i < sections.size(); i++) {
            List<Optional<Component>> newLines = sections.get(i).update(time);
            for (int j = 0; j < newLines.size(); j++) {
                int index = i + j;
                if (0 <= index && index < 15) {
                    newLines.get(j).ifPresent(newLine -> {
                        String lineId = lineId(index);
                        sidebar.updateLineContent(lineId, newLine);
                    });
                }
            }
        }
    }

    @Override
    public void end() {
        for (Sidebar.ScoreboardLine line : sidebar.getLines()) {
            sidebar.removeLine(line.getId());
        }
    }

    private void refreshSections() {
        boolean shouldInvalidate = false;
        int newTotalSize = 0;
        for (int i = 0; i < sizes.length; i++) {
            int newSize = sections.get(i).getSize();
            if (sizes[i] != newSize) {
                shouldInvalidate = true;
                sizes[i] = newSize;
            }
            newTotalSize += newSize;
        }

        if (shouldInvalidate) {
            for (SidebarSection section : sections) {
                section.invalidateCache();
            }

            int oldClampedSize = Math.min(totalSize, 15);
            int newClampedSize = Math.min(newTotalSize, 15);
            if (oldClampedSize < newClampedSize) {
                for (int i = oldClampedSize; i < newClampedSize; i++) {
                    sidebar.createLine(new Sidebar.ScoreboardLine(lineId(i), Component.empty(), newClampedSize - i));
                }
            }
            else if (oldClampedSize > newClampedSize) {
                for (int i = oldClampedSize - 1; i >= newClampedSize; i--) {
                    sidebar.removeLine(lineId(i));
                }
            }

            totalSize = newTotalSize;
        }
    }

    private @NotNull String lineId(int index) {
        return "line" + index;
    }

    @DataObject
    public record Data(@NotNull @DataPath("sections") Collection<String> sectionPaths) {

        public Data {
            Objects.requireNonNull(sectionPaths, "sectionPaths");
        }

    }

}
