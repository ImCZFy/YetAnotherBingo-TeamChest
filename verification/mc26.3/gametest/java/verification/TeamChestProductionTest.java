package verification;

import java.nio.file.Files;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

public class TeamChestProductionTest {
    final TeamChestMultiplayerTest h;
    TeamChestProductionTest(TeamChestMultiplayerTest h){this.h=h;}
    void run() {
        h.ctx.waitFor(c -> Files.exists(h.dir.resolve("ready")),12000);h.connect(h.read("ready"));
        // Wait until all three real clients can accept requests.
        h.remote("B","noop");h.remote("C","noop");rpc("setup");
        h.local("command tc config");h.ctx.waitForScreen(ContainerScreen.class);
        for(int row: new int[]{4,5,6,1,2,3}) {h.local("click 16 0 PICKUP");h.ctx.waitTicks(3);rpc("cfgrows "+row);}
        for(int row: new int[]{2,1,6,5,4,3}) {h.local("click 16 1 PICKUP");h.ctx.waitTicks(3);rpc("cfgrows "+row);}
        h.local("click 10 0 PICKUP");h.ctx.waitTicks(3);rpc("cfgflags false true true");
        h.local("click 10 0 PICKUP");h.local("click 12 0 PICKUP");h.ctx.waitTicks(3);rpc("cfgflags true false true");
        h.local("click 12 0 PICKUP");h.local("click 14 0 PICKUP");h.ctx.waitTicks(3);rpc("cfgflags true true false");
        h.local("click 14 0 PICKUP");h.ctx.waitTicks(3);rpc("cfgflags true true true");
        h.local("click 22 2 CLONE");h.ctx.waitTicks(3);
        h.local("command tc config");h.ctx.waitForScreen(ContainerScreen.class);h.local("click 0 0 QUICK_MOVE");h.ctx.waitTicks(3);rpc("config-integrity");
        rpc("deop");h.local("click 16 0 PICKUP");h.ctx.waitTicks(5);rpc("permission-revoked");
        rpc("start");waitState("playing");rpc("nogame-team");rpc("objectives");
        for(int rows=1;rows<=6;rows++){
            rpc("size "+rows);h.local("command tc");h.local("rows "+rows);h.remote("B","command tc");h.remote("B","rows "+rows);rpc("size-check "+rows);h.local("close");h.remote("B","close");
        }
        rpc("size 3");rpc("fill");waitState("scored");rpc("normal-score");
        h.ctx.getInput().pressKey(InputConstants.KEY_B);h.ctx.waitForScreen(ContainerScreen.class);
        h.local("expect 0 minecraft:diamond 5");h.remote("B","command tc");h.remote("B","expect 26 minecraft:poplar_log 2");
        h.ctx.takeScreenshot("production-real-bingo-score");h.local("close");h.remote("B","close");
        // Disconnect/reconnect while teammates remain connected.
        h.remote("B","disconnect");h.ctx.waitTicks(10);h.remote("B","connect");h.remote("B","command tc");h.remote("B","expect 0 minecraft:diamond 5");h.remote("B","close");
        System.out.println("TEAMCHEST_PRODUCTION_CHECK: Reconnecting teammate receives the persistent shared chest");
        h.remote("B","disconnect");h.remote("C","disconnect");
        String boot=h.read("server.boot");rpc("stop");h.ctx.waitFor(c -> c.level==null,12000);
        h.ctx.waitFor(c -> Files.exists(h.dir.resolve("server.boot"))&&!h.read("server.boot").equals(boot),24000);
        h.connect(h.read("ready"));h.remote("B","connect");h.remote("C","connect");rpc("restored");
        h.local("command tc");h.ctx.waitForScreen(ContainerScreen.class);h.local("expect 26 minecraft:poplar_log 2");h.local("close");
        rpc("reset");waitState("pregame");rpc("reset-clear");rpc("join-blue");
        h.local("command teamchest config");h.ctx.waitForScreen(ContainerScreen.class);h.local("click 12 0 PICKUP");h.ctx.waitTicks(3);rpc("cfgflags true false true");h.local("close");
        rpc("start");waitState("playing");rpc("objectives");rpc("fill");h.ctx.waitTicks(80);rpc("disabled-score");waitState("player-scored");rpc("player-score-check");
        rpc("reset");waitState("pregame");rpc("reset-clear");rpc("join-blue");
        h.local("command tc config");h.ctx.waitForScreen(ContainerScreen.class);h.local("click 12 0 PICKUP");h.ctx.waitTicks(3);rpc("cfgflags true true true");h.local("close");
        rpc("consume-enable");rpc("start");waitState("playing");rpc("consume-objectives");waitState("scored");h.ctx.waitTicks(20);rpc("consume-score");
        h.local("command tc");h.ctx.waitForScreen(ContainerScreen.class);h.local("expect 0 minecraft:diamond 4");h.remote("B","command tc");h.remote("B","expect 0 minecraft:diamond 4");h.remote("C","command tc");h.remote("C","expect 0 minecraft:emerald 9");
        h.ctx.takeScreenshot("production-real-consume");rpc("postgame");h.local("close");h.remote("B","close");h.remote("C","close");
        rpc("reset");waitState("pregame");rpc("reset-clear");
        h.remote("B","disconnect");h.remote("C","disconnect");h.local("disconnect");
        rpc("finish");h.write("done","ok");System.out.println("TEAMCHEST_PRODUCTION_PASSED");
    }
    String rpc(String command) {
        int id=++h.sequence;h.write("server.request",id+"\n"+command);
        h.ctx.waitFor(c -> Files.exists(h.dir.resolve("server.response"))&&h.read("server.response").startsWith(id+"\n"),24000);
        String result=h.read("server.response").split("\n",2)[1];if(result.startsWith("FAIL"))throw new AssertionError(command+": "+result);return result;
    }
    void waitState(String state) {
        for(int i=0;i<1000;i++){if(rpc(state).equals("YES"))return;h.ctx.waitTicks(5);}throw new AssertionError("Timed out waiting for "+state);
    }
}
