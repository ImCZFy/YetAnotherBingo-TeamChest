import json,pathlib,subprocess,time,sys
root=pathlib.Path(__file__).resolve().parent
launch=json.loads((root/'launch.json').read_text())
run=root/('multiplayer-'+time.strftime('%Y%m%d-%H%M%S'))
run.mkdir();(root/'latest-multiplayer.txt').write_text(str(run))
procs={}
try:
 for role in 'ABC':
  wd=run/role;wd.mkdir();(wd/'eula.txt').write_text('eula=true\n')
  (wd/'options.txt').write_text('renderDistance:2\nsimulationDistance:5\nmaxFps:30\ninactivityFpsLimit:minimized\npauseOnLostFocus:false\nguiScale:2\nfullscreen:false\n')
  args=[launch['java'],'-Xmx1500m',*launch['jvmArgs'],'-Dverification.role='+role,'-Dverification.dir='+str(run),launch['main'],'--username','Chest'+role,'--uuid',{'A':'00000000-0000-4000-8000-000000000001','B':'00000000-0000-4000-8000-000000000002','C':'00000000-0000-4000-8000-000000000003'}[role],'--width','700','--height','480']
  f=(run/(role+'.log')).open('w');procs[role]=(subprocess.Popen(args,cwd=wd,stdout=f,stderr=subprocess.STDOUT),f)
  print('Launched Minecraft client',role,flush=True)
 deadline=time.time()+600
 while time.time()<deadline:
  if any(p.poll() is not None and p.returncode!=0 for p,f in procs.values()):break
  if all(p.poll() is not None for p,f in procs.values()):break
  time.sleep(1)
 print('Run:',run,flush=True)
 for role,(p,f) in procs.items():print(role,'exit',p.poll(),flush=True)
 if any(p.poll() is None or p.returncode!=0 for p,f in procs.values()):sys.exit(1)
finally:
 for p,f in procs.values():
  if p.poll() is None:p.terminate()
 for p,f in procs.values():
  try:p.wait(timeout=15)
  except subprocess.TimeoutExpired:p.kill();p.wait()
  f.close()
