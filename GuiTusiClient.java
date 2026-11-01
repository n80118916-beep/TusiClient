package ru.tusiclient;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;

public class GuiTusiClient extends GuiScreen {
    private int x, y, width = 520, height = 360;
    private final int SIDEBAR = 130;
    private String category = "Combat"; 

    private final int BG = new Color(18, 18, 20, 255).getRGB();
    private final int SIDE = new Color(24, 24, 27, 255).getRGB();
    private final int MOD = new Color(39, 39, 42, 255).getRGB();
    private final int TEXT = new Color(250, 250, 250, 255).getRGB();
    private final int DESC = new Color(161, 161, 170, 255).getRGB();

    @Override
    public void initGui() {
        this.x = (this.width / 2) - (this.width / 2);
        this.y = (this.height / 2) - (this.height / 2);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 180).getRGB());
        drawRect(x, y, x + width, y + height, BG);
        drawRect(x, y, x + SIDEBAR, y + height, SIDE);

        drawRect(x + 15, y + 15, x + 45, y + 45, new Color(180, 40, 40).getRGB());
        fontRendererObj.drawStringWithShadow("TusiClient", x + 55, y + 25, TEXT);

        drawCat("Combat", x + 15, y + 70, mouseX, mouseY);
        drawCat("Movement", x + 15, y + 100, mouseX, mouseY);
        drawCat("Render", x + 15, y + 130, mouseX, mouseY);
        drawCat("Player", x + 15, y + 160, mouseX, mouseY);
        drawCat("Util", x + 15, y + 190, mouseX, mouseY);
        
        fontRendererObj.drawString("Другие", x + 15, y + 220, DESC);
        drawCat("Configs", x + 15, y + 240, mouseX, mouseY);
        drawCat("Themes", x + 15, y + 270, mouseX, mouseY);

        drawRect(x + SIDEBAR + 20, y + 20, x + width - 20, y + 40, SIDE);
        fontRendererObj.drawString("Поиск...", x + SIDEBAR + 30, y + 26, DESC);

        List<ModuleManager.Module> list = ModuleManager.getModules(category);
        int startX = x + SIDEBAR + 20;
        int startY = y + 60;
        int col = 0, row = 0;

        for (ModuleManager.Module mod : list) {
            int mx = startX + (col * 190);
            int my = startY + (row * 45);

            if (mod.enabled) {
                drawGradient(mx, my, 180, 38, new Color(59, 130, 246).getRGB(), new Color(139, 92, 246).getRGB());
            } else {
                drawRect(mx, my, mx + 180, my + 38, MOD);
            }

            fontRendererObj.drawStringWithShadow(mod.name, mx + 8, my + 6, TEXT);

            GL11.glPushMatrix();
            GL11.glScalef(0.75f, 0.75f, 1f);
            fontRendererObj.drawString(mod.desc, (int)((mx + 8) / 0.75f), (int)((my + 20) / 0.75f), DESC);
            GL11.glPopMatrix();

            drawRect(mx + 160, my + 8, mx + 170, my + 18, mod.enabled ? new Color(255,255,255,200).getRGB() : SIDE);
            drawRect(mx + 155, my + 8, mx + 165, my + 18, mod.enabled ? new Color(255,255,255,100).getRGB() : SIDE);

            col++;
            if (col >= 2) { col = 0; row++; }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCat(String name, int cx, int cy, int mx, int my) {
        boolean active = category.equals(name);
        boolean hover = mx >= cx && mx <= cx + SIDEBAR - 20 && my >= cy && my <= cy + 20;

        if (active) {
            drawGradient(cx, cy, SIDEBAR - 20, 22, new Color(59, 130, 246).getRGB(), new Color(139, 92, 246).getRGB());
        } else if (hover) {
            drawRect(cx, cy, cx + SIDEBAR - 20, cy + 22, MOD);
        }
        fontRendererObj.drawString(name, cx + 10, cy + 7, active ? -1 : TEXT);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseY >= y + 70 && mouseY <= y + 92) { category = "Combat"; click(); }
        if (mouseY >= y + 100 && mouseY <= y + 122) { category = "Movement"; click(); }
        if (mouseY >= y + 130 && mouseY <= y + 152) { category = "Render"; click(); }

        List<ModuleManager.Module> list = ModuleManager.getModules(category);
        int startX = x + SIDEBAR + 20;
        int startY = y + 60;
        int col = 0, row = 0;

        for (ModuleManager.Module mod : list) {
            int mx = startX + (col * 190);
            int my = startY + (row * 45);

            if (mouseX >= mx && mouseX <= mx + 180 && mouseY >= my && mouseY <= my + 38) {
                mod.enabled = !mod.enabled;
                click();
            }
            col++;
            if (col >= 2) { col = 0; row++; }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }

    private void drawGradient(int x, int y, int w, int h, int c1, int c2) {
        float a1 = (float)(c1 >> 24 & 255) / 255.0F, r1 = (float)(c1 >> 16 & 255) / 255.0F, g1 = (float)(c1 >> 8 & 255) / 255.0F, b1 = (float)(c1 & 255) / 255.0F;
        float a2 = (float)(c2 >> 24 & 255) / 255.0F, r2 = (float)(c2 >> 16 & 255) / 255.0F, g2 = (float)(c2 >> 8 & 255) / 255.0F, b2 = (float)(c2 & 255) / 255.0F;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(r1, g1, b1, a1); GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x, y);
        GL11.glColor4f(r2, g2, b2, a2); GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}