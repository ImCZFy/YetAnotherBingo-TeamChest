"""Prepare the isolated, ignored runtime files for the recorded 26.3 game tests."""
import argparse,hashlib,json,pathlib,shutil,urllib.request
source=pathlib.Path(__file__).resolve().parent
repo=source.parent.parent
out=repo/'build/verification';out.mkdir(parents=True,exist_ok=True)
parser=argparse.ArgumentParser();parser.add_argument('--mode',choices=['multiplayer','singleplayer'],default='multiplayer');mode=parser.parse_args().mode
for name in ['client-tests.init.gradle','run-multiplayer.py','run-production.py','run-bingo-baseline.py']:
 shutil.copyfile(source/name,out/name)
shutil.copytree(source/'gametest',out/'gametest',dirs_exist_ok=True)
metadata=out/'gametest/resources/fabric.mod.json';j=json.loads(metadata.read_text())
if mode=='singleplayer':j['entrypoints']['fabric-client-gametest']=['verification.TeamChestClientTest']
metadata.write_text(json.dumps(j))
jar=out/'bingo-2.14.0+mc26.3.jar'
sha='c3630d7d04a776591aa0e9ed17bceef9d2e20641a2627a66ab3ede838e7af54452107daa9855fe23616b14ffc06f55e1492379147533f5ca75b0ae50baf5ed42'
if not jar.exists():
 urllib.request.urlretrieve('https://cdn.modrinth.com/data/mHeNceaH/versions/ZEQxfmtp/bingo-2.14.0%2Bmc26.3.jar',jar)
if hashlib.sha512(jar.read_bytes()).hexdigest()!=sha:raise SystemExit('Bingo verification dependency hash mismatch')
print('Prepared:',mode,'at',out)
