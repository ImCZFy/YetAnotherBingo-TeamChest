package verification;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.*;
import me.chengzhify.yetanotherbingoteamchest.*;
import me.chengzhify.yetanotherbingoteamchest.adapter.impl.*;
import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.*;

public class TeamChestServerVerification implements ModInitializer {
    final Path dir=Path.of(System.getProperty("verification.dir", "."));
    int last=0;
    boolean announced=false;
    @Override public void onInitialize() {
        if(FabricLoader.getInstance().getEnvironmentType()!=EnvType.SERVER || !Boolean.getBoolean("verification.external"))return;
        if(Files.exists(dir.resolve("server.request")))last=Integer.parseInt(read("server.request").split("\n",2)[0]);
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if(!announced){
                if(BingoApi.getINSTANCE()==null)return;
                check(BingoApi.getINSTANCE().getConfig().isLobbyMode(),"Production dedicated server initializes real Bingo API in lobby mode");
                write("ready","127.0.0.1:25634");write("server.boot",System.getProperty("verification.boot"));announced=true;
            }
            if(!Files.exists(dir.resolve("server.request")))return;
            String[] r=read("server.request").split("\n",2);int id=Integer.parseInt(r[0]);if(id<=last)return;last=id;
            try {String result=stage(s,r[1]);write("server.response",id+"\n"+result);}
            catch(Throwable e){e.printStackTrace();write("server.response",id+"\nFAIL "+e);}
        });
    }
    String stage(MinecraftServer s,String cmd) throws Exception {
        String[] parts=cmd.split(" ");
        switch(parts[0]) {
            case "setup" -> {
                check(s.getPlayerList().getPlayerCount()==3,"Three TCP clients on production dedicated server");
                exec(s,"op ChestA");
                TeamChestConfig.setRows(3);TeamChestConfig.setTeamChestEnabled(true);TeamChestConfig.setTeamTeleportEnabled(true);TeamChestConfig.setCountForBingoEnabled(true);
                for(ServerPlayer p:s.getPlayerList().getPlayers()){p.setPermanentlyInvulnerable(true);}
                playerCmd(s,"A","join red");playerCmd(s,"B","join red");
                check(YetAnotherBingoAPIImpl.getTeamId(p(s,"C").getUUID())==null,"Player C remains unassigned to a team");
            }
            case "cfgrows" -> check(TeamChestConfig.getRows()==Integer.parseInt(parts[1]),"Network config row selection "+parts[1]);
            case "cfgflags" -> {
                check(TeamChestConfig.isTeamChestEnabled()==Boolean.parseBoolean(parts[1]),"Network chest toggle "+parts[1]);
                check(TeamChestConfig.isCountForBingoEnabled()==Boolean.parseBoolean(parts[2]),"Network scoring toggle "+parts[2]);
                check(TeamChestConfig.isTeamTeleportEnabled()==Boolean.parseBoolean(parts[3]),"Network teleport toggle "+parts[3]);
            }
            case "config-integrity" -> {
                var a=p(s,"A");check(a.containerMenu instanceof ChestMenu,"Config menu remains open");
                check(a.containerMenu.getCarried().isEmpty(),"Config icons cannot be withdrawn");
                check(count(a,Items.CHEST)==0&&count(a,Items.TARGET)==0&&count(a,Items.BARRIER)==0,"Config UI did not leak decorative items");
            }
            case "deop" -> exec(s,"deop ChestA");
            case "permission-revoked" -> {
                check(TeamChestConfig.getRows()==3,"Revoked operator cannot modify config through an old window");
                check(p(s,"A").containerMenu instanceof InventoryMenu,"Revoked operator config is closed by server");
                exec(s,"op ChestA");
            }
            case "start" -> {
                for(ServerPlayer p:s.getPlayerList().getPlayers())p.closeContainer();
                playerCmd(s,"A","bingo options end_when never");
                playerCmd(s,"A","bingo start ignore_warnings");
            }
            case "playing" -> {return YetAnotherBingoAPIImpl.isStarted()?"YES":"NO";}
            case "nogame-team" -> {
                check(playerCmd(s,"C","tc")==0,"Unassigned real player cannot open team chest");
                check(playerCmd(s,"C","ttp ChestA")==0,"Unassigned real player cannot teleport to a team");
            }
            case "objectives" -> {
                String red=team(p(s,"A"));var card=BingoApi.getINSTANCE().getCards().getTeamCard(red);
                BingoApi.getINSTANCE().getCards().replaceEntry(card,0,0,"minecraft:diamond");
                BingoApi.getINSTANCE().getCards().replaceEntry(card,0,1,"minecraft:poplar_log");
                for(ServerPlayer p:s.getPlayerList().getPlayers())p.getInventory().clearContent();
                var chest=chest(s,red);chest.clearContent();
                check(!card.objective(0,0).hasTeamAchieved(red),"Known diamond objective initially unscored");
            }
            case "size" -> {int rows=Integer.parseInt(parts[1]);TeamChestConfig.setRows(rows);}
            case "size-check" -> {
                int rows=Integer.parseInt(parts[1]);
                for(String r:List.of("A","B")){
                    var menu=(ChestMenu)p(s,r).containerMenu;
                    check(menu.getRowCount()==rows&&menu.getContainer().getContainerSize()==rows*9,"Actual network chest menu "+rows+" rows for player "+r);
                }
            }
            case "fill" -> {
                var chest=chest(s,team(p(s,"A")));chest.setItem(0,new ItemStack(Items.DIAMOND,5));chest.setItem(26,new ItemStack(Items.POPLAR_LOG,2));
                check(count(p(s,"A"),Items.DIAMOND)==0&&count(p(s,"B"),Items.DIAMOND)==0,"Objective items exist only inside team chest");
            }
            case "scored" -> {
                String red=team(p(s,"A"));var card=BingoApi.getINSTANCE().getCards().getTeamCard(red);
                return card.objective(0,0).hasTeamAchieved(red)&&card.objective(0,1).hasTeamAchieved(red)?"YES":"NO";
            }
            case "normal-score" -> {
                String red=team(p(s,"A"));var card=BingoApi.getINSTANCE().getCards().getTeamCard(red);
                check(card.objective(0,0).hasPlayerAchieved(p(s,"A").getUUID())||card.objective(0,0).hasPlayerAchieved(p(s,"B").getUUID()),"Real Bingo attributes the shared objective to a connected teammate");
                check(score(red).getItems()==2,"Real Bingo adds two team objectives without double scoring teammates");
                check(chest(s,red).getItem(0).getCount()==5&&chest(s,red).getItem(26).getCount()==2,"Normal scoring retains chest contents");
            }
            case "stop" -> {
                s.saveEverything(false,true,true);
                s.overworld().getDataStorage().saveAndJoin();
                check(!chest(s,team(p(s,"A"))).isEmpty(),"Shared inventory saved before normal server shutdown");
                write("restart.expected","true");s.halt(false);
            }
            case "restored" -> {
                check(YetAnotherBingoAPIImpl.isStarted(),"Real Bingo game remains PLAYING after a complete process restart");
                String red=team(p(s,"A"));check(red.equals(team(p(s,"B"))),"Team membership persists across server restart");
                check(chest(s,red).getItem(0).getCount()==5&&chest(s,red).getItem(26).is(Items.POPLAR_LOG)&&chest(s,red).getItem(26).getCount()==2,"Production SavedData reloads counts and 26.3 items after process restart");
                check(TeamChestConfig.getRows()==3&&TeamChestConfig.isTeamChestEnabled()&&TeamChestConfig.isCountForBingoEnabled(),"TOML configuration reloads after process restart");
            }
            case "reset" -> playerCmd(s,"A","bingo reset");
            case "pregame" -> {return YetAnotherBingoAPIImpl.isConfigEditable()?"YES":"NO";}
            case "reset-clear" -> {
                check(YetAnotherBingoAPIImpl.isConfigEditable(),"Actual Bingo reset returns to PREGAME");
                var encoded=TeamChestStateImpl.CODEC.encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,s.registryAccess()),TeamChestStateImpl.getServerState(s)).getOrThrow();
                check(encoded.getAsJsonObject().getAsJsonObject("inventories").isEmpty(),"Actual Bingo reset removes every saved team inventory");
                for(ServerPlayer p:s.getPlayerList().getPlayers())check(p.containerMenu instanceof InventoryMenu,"Actual reset closes old menu: "+p.getName().getString());
            }
            case "join-blue" -> {playerCmd(s,"A","join red");playerCmd(s,"B","join red");playerCmd(s,"C","join blue");}
            case "disabled-score" -> {
                check(!TeamChestConfig.isCountForBingoEnabled(),"Scoring disabled through config UI");
                String red=team(p(s,"A"));var card=BingoApi.getINSTANCE().getCards().getTeamCard(red);
                check(!card.objective(0,0).hasTeamAchieved(red)&&!card.objective(0,1).hasTeamAchieved(red),"Real Bingo ignores chest objectives while scoring is disabled");
                check(chest(s,red).getItem(0).getCount()==5,"Disabled scoring retains items");
                p(s,"A").getInventory().setItem(0,new ItemStack(Items.DIAMOND));
            }
            case "player-scored" -> {String red=team(p(s,"A"));return BingoApi.getINSTANCE().getCards().getTeamCard(red).objective(0,0).hasTeamAchieved(red)?"YES":"NO";}
            case "player-score-check" -> {
                String red=team(p(s,"A"));var card=BingoApi.getINSTANCE().getCards().getTeamCard(red);
                check(card.objective(0,0).hasTeamAchieved(red)&&!card.objective(0,1).hasTeamAchieved(red),"Disabling chest scoring preserves normal player-inventory scoring");
            }
            case "consume-enable" -> {playerCmd(s,"A","bingo mode consume_items true");}
            case "consume-objectives" -> {
                String red=team(p(s,"A")),blue=team(p(s,"C"));
                for(String team:List.of(red,blue)){var card=BingoApi.getINSTANCE().getCards().getTeamCard(team);BingoApi.getINSTANCE().getCards().replaceEntry(card,0,0,"minecraft:diamond");BingoApi.getINSTANCE().getCards().replaceEntry(card,0,1,"minecraft:poplar_log");chest(s,team).clearContent();}
                for(ServerPlayer p:s.getPlayerList().getPlayers())p.getInventory().clearContent();
                chest(s,red).setItem(0,new ItemStack(Items.DIAMOND,5));chest(s,red).setItem(26,new ItemStack(Items.POPLAR_LOG,2));
                chest(s,blue).setItem(0,new ItemStack(Items.EMERALD,9));
            }
            case "consume-score" -> {
                String red=team(p(s,"A")),blue=team(p(s,"C"));var card=BingoApi.getINSTANCE().getCards().getTeamCard(red);
                check(card.objective(0,0).hasTeamAchieved(red)&&card.objective(0,1).hasTeamAchieved(red),"Real Bingo consume mode scores team chest items");
                check(!card.objective(0,0).hasTeamAchieved(blue),"Real Bingo never grants chest score to the other team");
                check(chest(s,red).getItem(0).getCount()==4&&chest(s,red).getItem(26).getCount()==1,"Real Bingo consumes each shared objective exactly once");
                check(chest(s,blue).getItem(0).getCount()==9,"Consume mode leaves other team's chest untouched");
                check(TeamChestStateImpl.getServerState(s).isDirty(),"Real consumption marks shared SavedData dirty");
            }
            case "postgame" -> {playerCmd(s,"A","bingo end");check(playerCmd(s,"A","tc")==0&&playerCmd(s,"A","ttp ChestB")==0,"Chest and teleport rejected in POSTGAME");}
            case "finish" -> {write("server.finished","ok");s.halt(false);}
            default -> throw new AssertionError(cmd);
        }
        return "OK";
    }
    static BingoTeamScore score(String team){for(var t:BingoApi.getINSTANCE().getTeams())if(t.getId().equals(team))return t.getScore();throw new AssertionError(team);}
    static ServerPlayer p(MinecraftServer s,String r){return Objects.requireNonNull(s.getPlayerList().getPlayerByName("Chest"+r));}
    static String team(ServerPlayer p){return YetAnotherBingoAPIImpl.getTeamId(p.getUUID());}
    static net.minecraft.world.Container chest(MinecraftServer s,String team){return TeamChestStateImpl.getServerState(s).getInventory(team);}
    static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    static int exec(MinecraftServer s,String cmd)throws Exception{return s.getCommands().getDispatcher().execute(cmd,s.createCommandSourceStack());}
    static int playerCmd(MinecraftServer s,String r,String cmd)throws Exception{return s.getCommands().getDispatcher().execute(cmd,p(s,r).createCommandSourceStack());}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("TEAMCHEST_SERVER_CHECK: "+message);}
    String read(String file){try{return Files.readString(dir.resolve(file));}catch(Exception e){throw new RuntimeException(e);}}
    void write(String file,String contents){try{Files.createDirectories(dir);Path tmp=dir.resolve(file+".tmp");Files.writeString(tmp,contents);Files.move(tmp,dir.resolve(file),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){throw new RuntimeException(e);}}
}
