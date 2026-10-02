package verification;

import java.nio.file.*;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import me.chengzhify.yetanotherbingoteamchest.*;
import me.chengzhify.yetanotherbingoteamchest.adapter.impl.*;
import me.jfenn.bingo.api.BingoApi;

public class TeamChestMultiplayerTest implements FabricClientGameTest {
    final String role = System.getProperty("verification.role", "A");
    final Path dir = Path.of(System.getProperty("verification.dir"));
    ClientGameTestContext ctx;
    int sequence;
    @Override public void runTest(ClientGameTestContext context) {
        ctx=context;
        if (!role.equals("A")) { secondary(); return; }
        if (Boolean.getBoolean("verification.external")) {new TeamChestProductionTest(this).run();return;}
        Properties properties=new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("server-port", "25633");
        properties.setProperty("online-mode", "false");
        properties.setProperty("white-list", "false");
        properties.setProperty("max-players", "10");
        properties.setProperty("enforce-secure-profile", "false");
        properties.setProperty("view-distance", "2");
        properties.setProperty("simulation-distance", "2");
        properties.setProperty("level-name", dir.resolve("world").toString());
        try (TestDedicatedServerContext server=context.worldBuilder().createServer(properties)) {
            try (TestDedicatedServerConnection connection=server.connect()) {
                write("ready", "127.0.0.1:25633");
                server.waitFor(s -> s.getPlayerList().getPlayerCount()==3, 12000);
                server.runOnServer(s -> {
                    check(s.isDedicatedServer(), "Dedicated server with three real connected players");
                    for(ServerPlayer p:s.getPlayerList().getPlayers()) {
                        check(p.connection.isAcceptingMessages(), "Live network session: "+p.getName().getString());
                        p.getAbilities().invulnerable = true;
                        p.setNoGravity(true);
                    }
                    execConsole(s,"op ChestA");
                    TeamChestConfig.setRows(3);
                    TeamChestConfig.setTeamChestEnabled(true);
                    TeamChestConfig.setCountForBingoEnabled(true);
                    TeamChestConfig.setTeamTeleportEnabled(true);
                    check(exec(s,p(s,"A"),"teamchest")==0,"Pregame chest rejected");
                    check(exec(s,p(s,"A"),"teamtp ChestB")==0,"Pregame teleport rejected");
                    check(exec(s,p(s,"A"),"teamtp")==0,"Missing teleport target rejected");
                    check(execConsole(s,"tc")==0,"Console chest rejected");
                    check(execConsole(s,"tc config")==0,"Console config rejected");
                    check(execConsole(s,"ttp ChestB")==0,"Console teleport rejected");
                    check(exec(s,p(s,"A"),"join red")==1,"Player A joins red");
                    check(exec(s,p(s,"B"),"join red")==1,"Player B joins red");
                    check(exec(s,p(s,"C"),"join blue")==1,"Player C joins blue");
                    check(exec(s,p(s,"A"),"tc toggle")==1&&!TeamChestConfig.isTeamChestEnabled(),"Operator chest toggle disables");
                    check(exec(s,p(s,"A"),"teamchest toggle")==1&&TeamChestConfig.isTeamChestEnabled(),"Operator chest toggle enables");
                    check(exec(s,p(s,"A"),"tptoggle")==1&&!TeamChestConfig.isTeamTeleportEnabled(),"Operator teleport toggle disables");
                    check(exec(s,p(s,"A"),"tptoggle")==1&&TeamChestConfig.isTeamTeleportEnabled(),"Operator teleport toggle enables");
                    for(String command:List.of("tc config","teamchest toggle","tptoggle")) {
                        try {s.getCommands().getDispatcher().execute(command,p(s,"B").createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS));throw new AssertionError("Unauthorized command: "+command);}
                        catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){check(true,"Non-operator denied: "+command);}
                    }
                    
                });
                local("command tc config");
                ctx.waitForScreen(ContainerScreen.class);
                server.runOnServer(s -> execConsole(s,"deop ChestA"));
                local("click 16 0 PICKUP");
                ctx.waitTicks(10);
                server.runOnServer(s -> {
                    check(TeamChestConfig.getRows()==3,"Revoked operator cannot change an already-open config menu");
                    execConsole(s,"op ChestA");
                    p(s,"A").closeContainer();
                    check(exec(s,p(s,"A"),"bingo start ignore_warnings")==1,"Real three-player Bingo starts");
                });
                ctx.waitFor(c -> c.gui.screen()==null);
                server.waitFor(s -> YetAnotherBingoAPIImpl.isStarted(),1200);
                ctx.getInput().pressKey(InputConstants.KEY_B);
                ctx.waitForScreen(ContainerScreen.class);
                remote("B","command tc");remote("C","command teamchest");
                server.waitFor(s -> p(s,"B").containerMenu instanceof ChestMenu && p(s,"C").containerMenu instanceof ChestMenu);
                server.runOnServer(s -> {
                    String red=team(p(s,"A")),blue=team(p(s,"C"));
                    check(red.equals(team(p(s,"B")))&&!red.equals(blue),"Real Bingo team membership");
                    check(chest(p(s,"A"))==chest(p(s,"B")),"Teammates open the identical server container");
                    check(chest(p(s,"A"))!=chest(p(s,"C")),"Different teams have separate server containers");
                    chest(p(s,"A")).setItem(0,new ItemStack(Items.DIAMOND,32));
                    chest(p(s,"C")).setItem(0,new ItemStack(Items.EMERALD,11));
                    p(s,"A").containerMenu.broadcastChanges();p(s,"B").containerMenu.broadcastChanges();p(s,"C").containerMenu.broadcastChanges();
                });
                local("expect 0 minecraft:diamond 32");
                remote("B","expect 0 minecraft:diamond 32");remote("C","expect 0 minecraft:emerald 11");
                check(true,"All three clients receive shared and isolated contents");
                ctx.takeScreenshot("multiplayer-red-before");remote("C","screenshot multiplayer-blue");
                local("click 0 1 PICKUP");
                server.waitFor(s -> chest(p(s,"A")).getItem(0).getCount()==16);
                remote("B","expect 0 minecraft:diamond 16");remote("C","expect 0 minecraft:emerald 11");
                check(true,"Right-click withdrawal synchronizes to teammate and leaves other team unchanged");
                local("click 1 0 PICKUP");
                server.waitFor(s -> chest(p(s,"A")).getItem(1).getCount()==16);
                remote("B","expect 1 minecraft:diamond 16");
                remote("B","click 1 0 QUICK_MOVE");
                server.waitFor(s -> chest(p(s,"A")).getItem(1).isEmpty()&&count(p(s,"B"),Items.DIAMOND)==16);
                local("expect 1 minecraft:air 0");
                check(true,"Network shift-click withdrawal updates teammate and inventory");
                remote("B","deposit minecraft:diamond");
                server.waitFor(s -> count(p(s,"B"),Items.DIAMOND)==0&&chest(p(s,"A")).getItem(0).getCount()==32);
                check(true,"Network shift-click deposit restores all items without duplication");
                // Send competing requests while the server remains paused by the test scheduler.
                int request=send("B","click 0 0 PICKUP");
                local("click 0 0 PICKUP");await("B",request);
                ctx.waitTicks(10);
                server.runOnServer(s -> {
                    int total=chest(p(s,"A")).getItem(0).getCount()+p(s,"A").containerMenu.getCarried().getCount()+p(s,"B").containerMenu.getCarried().getCount()+count(p(s,"A"),Items.DIAMOND)+count(p(s,"B"),Items.DIAMOND);
                    check(total==32,"Concurrent network withdrawal conserves item count");
                    check(p(s,"A").containerMenu.getCarried().isEmpty()||p(s,"B").containerMenu.getCarried().isEmpty(),"One stack cannot be withdrawn twice");
                });
                local("close");remote("B","close");remote("C","close");
                ctx.waitTicks(10);
                server.runOnServer(s -> {
                    check(count(p(s,"A"),Items.DIAMOND)+count(p(s,"B"),Items.DIAMOND)==32,"Closing menus returns carried items exactly once");
                    for(ServerPlayer p:s.getPlayerList().getPlayers()){p.getInventory().clearContent();p.containerMenu.broadcastChanges();}
                    check(exec(s,p(s,"A"),"teamtp ChestC")==0,"Different-team teleport rejected");
                    check(exec(s,p(s,"A"),"teamtp ChestA")==0,"Self teleport rejected");
                    var b=p(s,"B");b.teleportTo(s.overworld(),20,80,30,Set.<Relative>of(),67,-15,true);
                    var a=p(s,"A");a.teleportTo(s.overworld(),0,80,0,Set.<Relative>of(),0,0,true);
                    check(exec(s,a,"teamtp ChestB")==1,"Teammate teleport succeeds");
                    check(a.level()==b.level()&&a.position().distanceTo(b.position())<0.01&&a.getYRot()==b.getYRot()&&a.getXRot()==b.getXRot(),"Teleport copies server position and rotation");
                });
                local("position 20 80 30");check(true,"Teleport position reaches the actual sender client");
                server.runOnServer(s -> {
                    var b=p(s,"B");b.teleportTo(s.getLevel(Level.NETHER),10,90,10,Set.<Relative>of(),32,8,true);
                    check(exec(s,p(s,"A"),"ttp ChestB")==1,"Teleport alias succeeds across dimensions");
                });
                local("dimension minecraft:the_nether");remote("B","dimension minecraft:the_nether");
                local("position 10 90 10");check(true,"Cross-dimension teleport reaches both network clients");
                server.runOnServer(s -> {
                    TeamChestConfig.setTeamTeleportEnabled(false);
                    check(exec(s,p(s,"A"),"ttp ChestB")==0,"Disabled teleport rejected");TeamChestConfig.setTeamTeleportEnabled(true);
                    TeamChestConfig.setTeamChestEnabled(false);
                    check(exec(s,p(s,"A"),"tc")==0,"Disabled chest rejected");TeamChestConfig.setTeamChestEnabled(true);
                    for(String cmd:List.of("teamchest config","tc toggle","tptoggle"))check(exec(s,p(s,"A"),cmd)==0,"Playing configuration locked: "+cmd);
                });
                remote("B","disconnect");remote("C","disconnect");
                write("done","ok");
                System.out.println("TEAMCHEST_MULTIPLAYER_PASSED");
            }
        }
    }
    void secondary() {
        ctx.waitFor(c -> Files.exists(dir.resolve("ready")),12000);
        connect(read("ready"));
        int last=0;
        while(!Files.exists(dir.resolve("done"))) {
            final int previous=last;
            ctx.waitFor(c -> Files.exists(dir.resolve("done")) || Files.exists(dir.resolve(role+".request"))&&Integer.parseInt(read(role+".request").split("\n",2)[0])>previous,24000);
            if(Files.exists(dir.resolve("done")))break;
            String[] request=read(role+".request").split("\n",2);last=Integer.parseInt(request[0]);
            try {local(request[1]);write(role+".response",last+"\nOK");}
            catch(Throwable e){write(role+".response",last+"\nFAIL "+e);throw e;}
        }
        ctx.runOnClient(c -> c.disconnect(new TitleScreen(),false));
        System.out.println("TEAMCHEST_REMOTE_PASSED: "+role);
    }
    void connect(String address) {
        ctx.runOnClient(c -> ConnectScreen.startConnecting(c.gui.screen(),c,ServerAddress.parseString(address),new ServerData("Team chest verification",address,ServerData.Type.OTHER),false,null));
        ctx.waitFor(c -> c.player!=null&&c.level!=null&&c.gui.screen()==null,12000);
    }
    void local(String command) {
        String[] a=command.split(" ");
        switch(a[0]) {
            case "noop" -> {}
            case "connect" -> connect(read("ready"));
            case "command" -> ctx.runOnClient(c -> c.player.connection.sendCommand(command.substring(8)));
            case "deposit" -> ctx.runOnClient(c -> {int slot=-1;for(int i=27;i<c.player.containerMenu.slots.size();i++)if(BuiltInRegistries.ITEM.getKey(c.player.containerMenu.getSlot(i).getItem().getItem()).toString().equals(a[1])){slot=i;break;}if(slot<0)throw new AssertionError("Item missing from client inventory");c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,0,ContainerInput.QUICK_MOVE,c.player);});
            case "close" -> ctx.runOnClient(c -> c.player.closeContainer());
            case "click" -> ctx.runOnClient(c -> c.gameMode.handleContainerInput(c.player.containerMenu.containerId,Integer.parseInt(a[1]),Integer.parseInt(a[2]),ContainerInput.valueOf(a[3]),c.player));
            case "rows" -> ctx.waitFor(c -> c.player.containerMenu instanceof ChestMenu menu&&menu.getRowCount()==Integer.parseInt(a[1]),1200);
            case "expect" -> ctx.waitFor(c -> c.player!=null&&c.player.containerMenu instanceof ChestMenu&&BuiltInRegistries.ITEM.getKey(c.player.containerMenu.getSlot(Integer.parseInt(a[1])).getItem().getItem()).toString().equals(a[2])&&c.player.containerMenu.getSlot(Integer.parseInt(a[1])).getItem().getCount()==Integer.parseInt(a[3]),1200);
            case "position" -> ctx.waitFor(c -> c.player!=null&&Math.abs(c.player.getX()-Double.parseDouble(a[1]))<0.05&&Math.abs(c.player.getY()-Double.parseDouble(a[2]))<0.05&&Math.abs(c.player.getZ()-Double.parseDouble(a[3]))<0.05,1200);
            case "dimension" -> ctx.waitFor(c -> c.level!=null&&c.level.dimension().identifier().toString().equals(a[1]),1200);
            case "screenshot" -> ctx.takeScreenshot(a[1]);
            case "disconnect" -> ctx.runOnClient(c -> c.disconnect(new TitleScreen(),false));
            default -> throw new AssertionError(command);
        }
    }
    int send(String target,String command){int id=++sequence;write(target+".request",id+"\n"+command);return id;}
    void remote(String target,String command){await(target,send(target,command));}
    void await(String target,int id){ctx.waitFor(c -> Files.exists(dir.resolve(target+".response"))&&read(target+".response").startsWith(id+"\n"),12000);check(read(target+".response").equals(id+"\nOK"),"Remote "+target+": "+read(target+".response"));}
    String read(String name){try{return Files.readString(dir.resolve(name));}catch(Exception e){throw new RuntimeException(e);}}
    void write(String name,String text){try{Files.createDirectories(dir);Path temp=dir.resolve(name+".tmp");Files.writeString(temp,text);Files.move(temp,dir.resolve(name),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){throw new RuntimeException(e);}}
    static ServerPlayer p(MinecraftServer s,String role){return Objects.requireNonNull(s.getPlayerList().getPlayerByName("Chest"+role));}
    static String team(ServerPlayer p){return YetAnotherBingoAPIImpl.getTeamId(p.getUUID());}
    static net.minecraft.world.Container chest(ServerPlayer p){return ((ChestMenu)p.containerMenu).getContainer();}
    static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    static int exec(MinecraftServer s,ServerPlayer p,String cmd){try{return s.getCommands().getDispatcher().execute(cmd,p.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS));}catch(Exception e){throw new AssertionError(cmd,e);}}
    static int execConsole(MinecraftServer s,String cmd){try{return s.getCommands().getDispatcher().execute(cmd,s.createCommandSourceStack());}catch(Exception e){throw new AssertionError(cmd,e);}}
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);System.out.println("TEAMCHEST_MULTIPLAYER_CHECK: "+message);}
}
