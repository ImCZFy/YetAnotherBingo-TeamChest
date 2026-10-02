import pathlib,subprocess,time,json
root=pathlib.Path(__file__).resolve().parent
production=pathlib.Path((root/'latest-production.txt').read_text())
run=root/'bingo-shutdown-baseline';run.mkdir(exist_ok=True)
cp=(production/'classpath.args').read_text().splitlines()[1].split(':')
cp=[p for p in cp if 'YetAnotherBingo-TeamChest-mc26.3-' not in p and '/mc26.3/build/classes/java/gametest' not in p and '/mc26.3/build/resources/gametest' not in p]
(run/'classpath.args').write_text('-classpath\n'+':'.join(cp)+'\n')
config=[l for l in (production/'launch.cfg').read_text().splitlines() if not l.startswith('\tfabric.classPathGroups=')]
(run/'launch.cfg').write_text('\n'.join(config)+'\n')
(run/'eula.txt').write_text('eula=true\n')
(run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25635\nonline-mode=false\nwhite-list=false\nenforce-secure-profile=false\nview-distance=2\nsimulation-distance=2\nlevel-type=minecraft:flat\ngenerator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}\n')
configdir=run/'config/yet-another-minecraft-bingo';configdir.mkdir(parents=True,exist_ok=True)
config=json.loads((production/'server/config/yet-another-minecraft-bingo/config.json').read_text())
(configdir/'config.json').write_text(json.dumps(config,indent=2))
launch=json.loads((root/'launch.json').read_text())
args=[launch['java'],'-Xmx1800m','--sun-misc-unsafe-memory-access=allow','--enable-native-access=ALL-UNNAMED','@'+str(run/'classpath.args'),'-Dfabric.dli.config='+str(run/'launch.cfg'),'-Dfabric.dli.env=server','-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer',launch['main'],'nogui']
logpath=run/'server.log'
with logpath.open('w') as out:
 p=subprocess.Popen(args,cwd=run,stdin=subprocess.PIPE,stdout=out,stderr=subprocess.STDOUT,text=True)
 try:
  deadline=time.time()+90
  while time.time()<deadline and p.poll() is None:
   s=logpath.read_text()
   if 'Done (' in s and 'GameState changed: UNINITIALIZED -> PREGAME' in s:break
   time.sleep(1)
  else:raise RuntimeError('Bingo baseline did not start normally')
  p.stdin.write('stop\n');p.stdin.flush();p.wait(timeout=60)
 finally:
  if p.poll() is None:p.terminate();p.wait(timeout=20)
s=logpath.read_text()
print('Bingo-only server exit:',p.returncode)
print('TeamChest loaded:', 'yetanotherbingo-teamchest' in s)
print('Bingo scope shutdown diagnostic reproduced:', 'getScope invoked, but the server scope does not exist!' in s)
print('WorldDeleter shutdown diagnostic reproduced:', 'Skipping erroneous WorldDeleter call' in s)
assert p.returncode==0 and 'yetanotherbingo-teamchest' not in s
