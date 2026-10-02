import json,pathlib,subprocess,time,sys
root=pathlib.Path(__file__).resolve().parent
repo=root.parent.parent
launch=json.loads((root/'launch.json').read_text())
run=root/('production-'+time.strftime('%Y%m%d-%H%M%S'));run.mkdir();(root/'latest-production.txt').write_text(str(run))
wd=run/'server';wd.mkdir();(wd/'eula.txt').write_text('eula=true\n')
(wd/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25634\nonline-mode=false\nwhite-list=false\nenforce-whitelist=false\nenforce-secure-profile=false\nmax-players=10\nview-distance=2\nsimulation-distance=2\nlevel-type=minecraft:flat\nlevel-seed=1\ngenerator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}\nspawn-protection=0\nlevel-name=world\n')
previous=pathlib.Path((root/'latest-multiplayer.txt').read_text())/'A/config/yet-another-minecraft-bingo'
configdir=wd/'config/yet-another-minecraft-bingo';configdir.mkdir(parents=True)
config=json.loads((previous/'config.json').read_text());config['server'].update(isLobbyMode=True,templateEnabled=False,fixedSeedEnabled=True,fixedSeed=1,preloadViewDistance=2);config['countdownDelayTicks']=20;config['countdownSeconds']=1;config['lobbyTutorialBook']=False;config['nextRoundWhenEveryoneDisconnects']=False
(configdir/'config.json').write_text(json.dumps(config,indent=2))
options=json.loads((previous/'game-options-default.json').read_text());options['spawnDistance']=1
(configdir/'game-options.json').write_text(json.dumps(options,indent=2))
# Load the actual release JAR instead of the Gradle main/client source output.
original=pathlib.Path([a[1:] for a in launch['jvmArgs'] if a.startswith('@')][0]).read_text().splitlines()
cp=original[1].split(':')
exclude=[str(repo/'mc26.3/build/classes/java/main'),str(repo/'mc26.3/build/classes/java/client'),str(repo/'mc26.3/build/resources/main'),str(repo/'mc26.3/build/resources/client')]
cp=[p for p in cp if p not in exclude]+[str(repo/'build/libs/YetAnotherBingo-TeamChest-mc26.3-1.3.3.jar')]
(run/'classpath.args').write_text('-classpath\n'+':'.join(cp)+'\n')
configpath=repo/'.gradle/loom-cache/projects/mc26.3/launch.cfg'
lines=configpath.read_text().splitlines();lines=[l if not l.startswith('\tfabric.classPathGroups=') else '\tfabric.classPathGroups='+str(repo/'build/libs/YetAnotherBingo-TeamChest-mc26.3-1.3.3.jar')+'::'+str(repo/'mc26.3/build/classes/java/gametest')+':'+str(repo/'mc26.3/build/resources/gametest') for l in lines]
(run/'launch.cfg').write_text('\n'.join(lines)+'\n')
base=[a for a in launch['jvmArgs'] if not a.startswith('@') and not a.startswith('-Dfabric.dli.') and not a.startswith('-Dfabric.client.gametest') and a!='-XstartOnFirstThread']
common=[launch['java'],'-Xmx1800m',*base,'@'+str(run/'classpath.args'),'-Dfabric.dli.config='+str(run/'launch.cfg'),'-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient','-Dverification.external=true','-Dverification.dir='+str(run)]
procs={};generation=0

def server():
 global generation
 generation+=1
 f=(run/('server-'+str(generation)+'.log')).open('w')
 args=[a.replace('KnotClient','KnotServer') for a in common]+['-Dfabric.dli.env=server','-Dverification.boot='+str(generation),launch['main'],'nogui']
 p=subprocess.Popen(args,cwd=wd,stdin=subprocess.DEVNULL,stdout=f,stderr=subprocess.STDOUT)
 procs['server']=(p,f);print('Started production dedicated server, boot',generation,flush=True)

try:
 server()
 for role in 'ABC':
  clientwd=run/role;clientwd.mkdir()
  (clientwd/'options.txt').write_text('renderDistance:2\nsimulationDistance:5\nmaxFps:30\ninactivityFpsLimit:minimized\npauseOnLostFocus:false\nguiScale:2\nfullscreen:false\n')
  args=[*common,'-XstartOnFirstThread','-Dfabric.dli.env=client','-Dfabric.client.gametest','-Dverification.role='+role,launch['main'],'--username','Chest'+role,'--width','700','--height','480']
  f=(run/(role+'.log')).open('w');procs[role]=(subprocess.Popen(args,cwd=clientwd,stdout=f,stderr=subprocess.STDOUT),f)
  print('Launched release JAR client',role,flush=True)
 deadline=time.time()+900
 while time.time()<deadline:
  sp,sf=procs['server']
  if sp.poll() is not None and not (run/'server.finished').exists():
   if sp.returncode==0 and (run/'restart.expected').exists():
    sf.close();(run/'restart.expected').unlink();server()
   else:break
  if any(p.poll() is not None and p.returncode!=0 for p,f in procs.values()):break
  if all(p.poll() is not None for p,f in procs.values()):break
  time.sleep(1)
 print('Run:',run,flush=True)
 for role,(p,f) in procs.items():print(role,'exit',p.poll(),flush=True)
 if any(p.poll() is None or p.returncode!=0 for p,f in procs.values()) or not (run/'server.finished').exists():sys.exit(1)
finally:
 for p,f in procs.values():
  if p.poll() is None:p.terminate()
 for p,f in procs.values():
  try:p.wait(timeout=15)
  except subprocess.TimeoutExpired:p.kill();p.wait()
  f.close()
