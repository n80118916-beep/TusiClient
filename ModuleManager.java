package ru.tusiclient;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    
    public static List<Module> modules = new ArrayList<>();
    public static List<String> friends = new ArrayList<>();
    public static Minecraft mc = Minecraft.getMinecraft();
    private static long lastAttack = 0;
    private static long lastPvpMsg = 0;
    private static long lastBowShoot = 0;
    private static long lastPot = 0;

    public ModuleManager() {
        friends.add("Tusi"); // Твой ник

        // Combat (Рабочие)
        modules.add(new Module("Auto Explosion", "Автоматически ставит и взрывает ТНТ", "Combat", false));
        modules.add(new Module("Friend Protect", "Не бьет друзей", "Combat", true));
        modules.add(new Module("Auto GApple", "Автоматически ест золотое яблоко", "Combat", false));
        modules.add(new Module("Attack Aura", "Автоматически бьет окружающих", "Combat", false));
        modules.add(new Module("Auto Totem", "Ищет тотем в хотбаре", "Combat", false));
        modules.add(new Module("Keep Sprint", "Не сбрасывает спринт", "Combat", true));
        modules.add(new Module("PVP Helper", "Автоматически пишет /pvp", "Combat", false));
        modules.add(new Module("Bow Spam", "Быстрая стрельба из лука", "Combat", false));
        modules.add(new Module("Auto Pot", "Автоматически кидает зелья", "Combat", false));
        modules.add(new Module("Hitboxes", "Увеличивает хитбоксы", "Combat", false));
        modules.add(new Module("Velocity", "Убирает откидывание", "Combat", false));
        
        // Render
        modules.add(new Module("Fullbright", "Максимальная яркость", "Render", true));
        modules.add(new Module("ESP", "Подсветка игроков", "Render", false));
        modules.add(new Module("Tracers", "Линии до игроков", "Render", false));
        modules.add(new Module("NoHurtCam", "Убирает тряску камеры", "Render", true));
        modules.add(new Module("Storage ESP", "Подсветка сундуков", "Render", false));
        modules.add(new Module("Item ESP", "Подсветка предметов", "Render", false));
        modules.add(new Module("Nametags", "Красивые ники", "Render", false));
        modules.add(new Module("Block ESP", "Подсветка руд", "Render", false));
        modules.add(new Module("Chams", "Игроки сквозь стены", "Render", false));
        modules.add(new Module("Freecam", "Свободная камера (NoClip)", "Render", false));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;

        // 1. Attack Aura
        if (isEnabled("Attack Aura")) {
            EntityLivingBase target = findClosestTarget(4.5);
            if (target != null) {
                if (isEnabled("Friend Protect") && target instanceof EntityPlayer && friends.contains(target.getCommandSenderName())) {
                    // Пропускаем
                } else {
                    float[] rot = getRotations(target);
                    mc.thePlayer.rotationYaw = rot[0];
                    mc.thePlayer.rotationPitch = rot[1];
                    
                    if (System.currentTimeMillis() - lastAttack > 450) {
                        mc.playerController.attackEntity(mc.thePlayer, target);
                        mc.thePlayer.swingItem();
                        lastAttack = System.currentTimeMillis();
                    }
                }
            }
        }

        // 2. Auto GApple
        if (isEnabled("Auto GApple") && mc.thePlayer.getHealth() <= 15.0F) {
            int slot = findItemInHotbar(Items.golden_apple);
            if (slot != -1) {
                mc.thePlayer.inventory.currentItem = slot;
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem());
            }
        }

        // 3. Auto Pot
        if (isEnabled("Auto Pot") && mc.thePlayer.getHealth() <= 15.0F && System.currentTimeMillis() - lastPot > 1000) {
            int slot = findSplashHealthPotion();
            if (slot != -1) {
                mc.thePlayer.inventory.currentItem = slot;
                mc.thePlayer.rotationPitch = 90.0F;
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem());
                lastPot = System.currentTimeMillis();
            }
        }

        // 4. Keep Sprint
        if (isEnabled("Keep Sprint")) {
            if ((mc.thePlayer.moveForward > 0 || mc.thePlayer.moveStrafing > 0) && !mc.thePlayer.isSneaking()) {
                mc.thePlayer.setSprinting(true);
            }
        }

        // 5. PVP Helper
        if (isEnabled("PVP Helper") && System.currentTimeMillis() - lastPvpMsg > 5000) {
            mc.thePlayer.sendChatMessage("/pvp");
            lastPvpMsg = System.currentTimeMillis();
        }

        // 6. Bow Spam
        if (isEnabled("Bow Spam") && mc.gameSettings.keyBindUseItem.getIsKeyPressed()) {
            ItemStack held = mc.thePlayer.getCurrentEquippedItem();
            if (held != null && held.getItem() == Items.bow && System.currentTimeMillis() - lastBowShoot > 150) {
                mc.thePlayer.stopUsingItem();
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(5, 0, 0, 0, 0));
                lastBowShoot = System.currentTimeMillis();
            }
        }

        // 7. Auto Explosion (ТНТ)
        if (isEnabled("Auto Explosion")) {
            EntityLivingBase target = findClosestTarget(4.0);
            if (target != null) {
                int tntSlot = findItemInHotbar(Item.getItemFromBlock(Blocks.tnt));
                if (tntSlot != -1) {
                    mc.thePlayer.inventory.currentItem = tntSlot;
                    int x = MathHelper.floor_double(target.posX);
                    int y = MathHelper.floor_double(target.posY);
                    int z = MathHelper.floor_double(target.posZ);
                    
                    if (mc.theWorld.getBlock(x, y, z) == Blocks.air) {
                        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), x, y, z, 1, new Vec3(x, y, z));
                    }
                    int flintSlot = findItemInHotbar(Items.flint_and_steel);
                    if (flintSlot != -1) {
                        mc.thePlayer.inventory.currentItem = flintSlot;
                        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), x, y, z, 1, new Vec3(x, y, z));
                    }
                }
            }
        }

        // --- RENDER TICK ---
        if (isEnabled("Fullbright")) mc.gameSettings.gammaSetting = 100.0F;
        else if (mc.gameSettings.gammaSetting == 100.0F) mc.gameSettings.gammaSetting = 1.0F;

        if (isEnabled("NoHurtCam")) {
            mc.thePlayer.hurtTime = 0;
            mc.thePlayer.maxHurtTime = 0;
        }

        if (isEnabled("Velocity") && mc.thePlayer.hurtTime > 0) {
            mc.thePlayer.motionX *= 0.1;
            mc.thePlayer.motionZ *= 0.1;
        }

        if (isEnabled("Freecam")) {
            mc.thePlayer.noClip = true;
            mc.thePlayer.capabilities.isFlying = true;
            mc.thePlayer.motionY = 0;
            if (mc.gameSettings.keyBindJump.getIsKeyPressed()) mc.thePlayer.motionY = 0.5;
            if (mc.gameSettings.keyBindSneak.getIsKeyPressed()) mc.thePlayer.motionY = -0.5;
        } else {
            mc.thePlayer.capabilities.isFlying = false;
            mc.thePlayer.noClip = false;
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving == mc.thePlayer && isEnabled("Velocity")) {
            event.entityLiving.motionX = 0;
            event.entityLiving.motionZ = 0;
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (isEnabled("Chams") && event.entityPlayer != mc.thePlayer) GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (isEnabled("Chams") && event.entityPlayer != mc.thePlayer) GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        boolean esp = isEnabled("ESP");
        boolean tracers = isEnabled("Tracers");
        boolean storage = isEnabled("Storage ESP");
        boolean items = isEnabled("Item ESP");
        boolean blockEsp = isEnabled("Block ESP");
        boolean nametags = isEnabled("Nametags");
        boolean hitboxes = isEnabled("Hitboxes");

        if (!esp && !tracers && !storage && !items && !blockEsp && !nametags && !hitboxes) return;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);

        float pTicks = event.partialTicks;
        double rX = RenderManager.renderPosX;
        double rY = RenderManager.renderPosY;
        double rZ = RenderManager.renderPosZ;

        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * pTicks - rX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * pTicks - rY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pTicks - rZ;

            if (tracers) {
                GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);
                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex3d(0, -0.1, 0);
                GL11.glVertex3d(x, y + player.getEyeHeight() / 2, z);
                GL11.glEnd();
            }
            if (esp) {
                GL11.glColor4f(0.0F, 1.0F, 0.0F, 1.0F);
                drawBox(x, y, z, player.width / 2, player.height);
            }
            if (hitboxes) {
                GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);
                drawBox(x, y, z, (player.width / 2) + 0.2, player.height + 0.2);
            }
            if (nametags) {
                GL11.glPushMatrix();
                GL11.glTranslated(x, y + player.height + 0.5, z);
                GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
                float scale = 0.02666667F;
                GL11.glScalef(-scale, -scale, scale);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDepthMask(false);
                
                String name = player.getDisplayName();
                int width = mc.fontRenderer.getStringWidth(name) / 2;
                drawRect(-width - 1, -1, width + 1, 8, 0x80000000);
                mc.fontRenderer.drawStringWithShadow(name, -width, 0, 0xFFFFFF);
                
                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glPopMatrix();
            }
        }

        if (storage) {
            GL11.glColor4f(1.0F, 1.0F, 0.0F, 1.0F);
            for (Object obj : mc.theWorld.loadedTileEntityList) {
                if (obj instanceof TileEntityChest) {
                    TileEntityChest chest = (TileEntityChest) obj;
                    drawBox(chest.xCoord + 0.5 - rX, chest.yCoord + 0.5 - rY, chest.zCoord + 0.5 - rZ, 0.5, 1.0);
                }
            }
        }

        if (items) {
            GL11.glColor4f(1.0F, 0.5F, 0.0F, 1.0F);
            for (Object obj : mc.theWorld.loadedEntityList) {
                if (obj instanceof EntityItem) {
                    EntityItem item = (EntityItem) obj;
                    double x = item.lastTickPosX + (item.posX - item.lastTickPosX) * pTicks - rX;
                    double y = item.lastTickPosY + (item.posY - item.lastTickPosY) * pTicks - rY;
                    double z = item.lastTickPosZ + (item.posZ - item.lastTickPosZ) * pTicks - rZ;
                    drawBox(x, y, z, 0.2, 0.4);
                }
            }
        }

        if (blockEsp) {
            int radius = 20;
            int px = (int) mc.thePlayer.posX;
            int py = (int) mc.thePlayer.posY;
            int pz = (int) mc.thePlayer.posZ;
            GL11.glColor4f(0.0F, 1.0F, 1.0F, 1.0F);
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block block = mc.theWorld.getBlock(px + x, py + y, pz + z);
                        if (block == Blocks.diamond_ore || block == Blocks.gold_ore || block == Blocks.iron_ore || block == Blocks.emerald_ore) {
                            drawBox((px + x) - rX, (py + y) - rY, (pz + z) - rZ, 0.5, 1.0);
                        }
                    }
                }
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawBox(double x, double y, double z, double w, double h) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(x - w, y, z - w); GL11.glVertex3d(x + w, y, z - w);
        GL11.glVertex3d(x + w, y, z + w); GL11.glVertex3d(x - w, y, z + w);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(x - w, y + h, z - w); GL11.glVertex3d(x + w, y + h, z - w);
        GL11.glVertex3d(x + w, y + h, z + w); GL11.glVertex3d(x - w, y + h, z + w);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x - w, y, z - w); GL11.glVertex3d(x - w, y + h, z - w);
        GL11.glVertex3d(x + w, y, z - w); GL11.glVertex3d(x + w, y + h, z - w);
        GL11.glVertex3d(x + w, y, z + w); GL11.glVertex3d(x + w, y + h, z + w);
        GL11.glVertex3d(x - w, y, z + w); GL11.glVertex3d(x - w, y + h, z + w);
        GL11.glEnd();
    }

    private EntityLivingBase findClosestTarget(double range) {
        EntityLivingBase target = null;
        double dist = range;
        for (Object obj : mc.theWorld.loadedEntityList) {
            if (obj instanceof EntityLivingBase) {
                EntityLivingBase entity = (EntityLivingBase) obj;
                if (entity == mc.thePlayer || entity.isDead || entity.getHealth() <= 0) continue;
                if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode) continue;
                double d = mc.thePlayer.getDistanceToEntity(entity);
                if (d < dist) { dist = d; target = entity; }
            }
        }
        return target;
    }

    private float[] getRotations(Entity entity) {
        double diffX = entity.posX - mc.thePlayer.posX;
        double diffY = entity.posY + entity.getEyeHeight() / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = entity.posZ - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);
        return new float[]{yaw, pitch};
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) return i;
        }
        return -1;
    }

    private int findSplashHealthPotion() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.potionitem) {
                List<PotionEffect> effects = Items.potionitem.getEffects(stack);
                if (effects != null) {
                    for (PotionEffect effect : effects) {
                        if (effect.getPotionID() == Potion.heal.id) return i;
                    }
                }
            }
        }
        return -1;
    }

    public static boolean isEnabled(String name) {
        for (Module m : modules) if (m.name.equals(name)) return m.enabled;
        return false;
    }

    public static List<Module> getModules(String category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) if (m.category.equals(category)) list.add(m);
        return list;
    }

    public static class Module {
        public String name, desc, category;
        public boolean enabled;
        public Module(String name, String desc, String category, boolean enabled) {
            this.name = name; this.desc = desc; this.category = category; this.enabled = enabled;
        }
    }
}