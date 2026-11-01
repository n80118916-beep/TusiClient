package ru.tusiclient;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

@Mod(modid = "tusiclient", name = "TusiClient", version = "1.0")
public class TusiClient {
    
    public static KeyBinding openGuiKey;
    public static ModuleManager moduleManager;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Клавиша Right Shift
        openGuiKey = new KeyBinding("Открыть меню TusiClient", Keyboard.KEY_RSHIFT, "TusiClient");
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(openGuiKey);
        
        // Инициализация модулей
        moduleManager = new ModuleManager();
        
        // Автоматическая регистрация событий
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(moduleManager);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openGuiKey.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTusiClient());
        }
    }
}