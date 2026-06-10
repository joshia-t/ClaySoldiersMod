package io.github.joshiat.claylegion.client.gui;

import io.github.joshiat.claylegion.entity.team.TeamRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeSlot;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Clay Lexicon screen — a paged in-game wiki (issue #23).
 *
 * <p>All copy flows through translation keys (lexicon.clay-legion.*) so resource
 * packs can localize every page. Upgrade pages are generated from the live
 * UpgradeRegistry, so newly registered upgrades appear automatically; entries
 * missing a localized description fall back to a shared placeholder line.
 */
@Environment(EnvType.CLIENT)
public class LexiconScreen extends Screen {

    private static final int PAGE_WIDTH = 240;
    private static final int UPGRADES_PER_PAGE = 6;
    private static final int TEAMS_PER_PAGE = 13;

    private record LexiconPage(Component title, List<Component> body) {}

    private final List<LexiconPage> pages = new ArrayList<>();
    private int pageIndex;

    private Button prevButton;
    private Button nextButton;

    public LexiconScreen() {
        super(Component.translatable("lexicon.clay-legion.title"));
        buildPages();
    }

    private void buildPages() {
        // Introduction.
        pages.add(new LexiconPage(
            Component.translatable("lexicon.clay-legion.intro.title"),
            List.of(
                Component.translatable("lexicon.clay-legion.intro.body1"),
                Component.translatable("lexicon.clay-legion.intro.body2"),
                Component.translatable("lexicon.clay-legion.intro.body3")
            )));

        // Teams, generated from the registry.
        List<Component> teamLines = new ArrayList<>();
        for (int id = 0; id < TeamRegistry.size(); id++) {
            teamLines.add(Component.literal("• " + TeamRegistry.getById(id).name()));
            if (teamLines.size() == TEAMS_PER_PAGE) {
                pages.add(new LexiconPage(
                    Component.translatable("lexicon.clay-legion.teams.title"), List.copyOf(teamLines)));
                teamLines.clear();
            }
        }
        if (!teamLines.isEmpty()) {
            pages.add(new LexiconPage(
                Component.translatable("lexicon.clay-legion.teams.title"), List.copyOf(teamLines)));
        }

        // Upgrades, one chapter per slot, generated from the registry.
        for (UpgradeSlot slot : UpgradeSlot.values()) {
            List<Component> entries = new ArrayList<>();
            for (int bit = 0; bit < 64; bit++) {
                UpgradeSpec spec = UpgradeRegistry.getSpec(1L << bit);
                if (spec == null || spec.slot() != slot) {
                    continue;
                }
                entries.add(upgradeEntry(spec));
            }
            Component chapterTitle = Component.translatable(
                "lexicon.clay-legion.slot." + slot.name().toLowerCase(java.util.Locale.ROOT));
            for (int from = 0; from < entries.size(); from += UPGRADES_PER_PAGE) {
                pages.add(new LexiconPage(chapterTitle,
                    entries.subList(from, Math.min(entries.size(), from + UPGRADES_PER_PAGE))));
            }
        }

        // Mounts.
        pages.add(new LexiconPage(
            Component.translatable("lexicon.clay-legion.mounts.title"),
            List.of(
                Component.translatable("lexicon.clay-legion.mounts.horse"),
                Component.translatable("lexicon.clay-legion.mounts.pegasus"),
                Component.translatable("lexicon.clay-legion.mounts.turtle"),
                Component.translatable("lexicon.clay-legion.mounts.bunny"),
                Component.translatable("lexicon.clay-legion.mounts.gecko")
            )));
    }

    private Component upgradeEntry(UpgradeSpec spec) {
        String flagName = UpgradeFlags.nameOf(spec.flag()).toLowerCase(java.util.Locale.ROOT);
        String descKey = "lexicon.clay-legion.upgrade." + flagName;
        Component description = Language.getInstance().has(descKey)
            ? Component.translatable(descKey)
            : Component.translatable("lexicon.clay-legion.upgrade.unknown");
        return Component.literal("• ")
            .append(Component.literal(prettyName(flagName)).withStyle(style -> style.withBold(true)))
            .append(Component.literal(": "))
            .append(description);
    }

    private static String prettyName(String flagName) {
        String[] words = flagName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height - 32;

        prevButton = addRenderableWidget(Button.builder(
                Component.translatable("lexicon.clay-legion.prev"),
                b -> turnPage(-1))
            .bounds(centerX - 110, buttonY, 70, 20)
            .build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> onClose())
            .bounds(centerX - 30, buttonY, 60, 20)
            .build());

        nextButton = addRenderableWidget(Button.builder(
                Component.translatable("lexicon.clay-legion.next"),
                b -> turnPage(1))
            .bounds(centerX + 40, buttonY, 70, 20)
            .build());

        updateButtons();
    }

    private void turnPage(int delta) {
        pageIndex = Math.max(0, Math.min(pages.size() - 1, pageIndex + delta));
        updateButtons();
    }

    private void updateButtons() {
        prevButton.active = pageIndex > 0;
        nextButton.active = pageIndex < pages.size() - 1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        LexiconPage page = pages.get(pageIndex);
        int centerX = this.width / 2;

        graphics.centeredText(this.font, this.title, centerX, 16, 0xFFFFFFFF);
        graphics.centeredText(this.font, page.title(), centerX, 32, 0xFFFFD080);

        int y = 50;
        for (Component line : page.body()) {
            for (FormattedCharSequence wrapped : this.font.split(FormattedText.of(
                    line.getString()), PAGE_WIDTH)) {
                graphics.text(this.font, wrapped, centerX - PAGE_WIDTH / 2, y, 0xFFE0E0E0);
                y += 11;
            }
            y += 3;
        }

        Component pageLabel = Component.translatable("lexicon.clay-legion.page",
            pageIndex + 1, pages.size());
        graphics.centeredText(this.font, pageLabel, centerX, this.height - 46, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
