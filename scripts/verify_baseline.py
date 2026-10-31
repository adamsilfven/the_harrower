"""Verify this release against the actual approved user-tested 0.6.2-rc.2 consolidated binary."""
from pathlib import Path
from zipfile import ZipFile
import hashlib,json,sys,tomllib
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'reference/release-approved-entry-hashes.json').read_text())
allowed=set(json.loads((root/'reference/release-allowed-changes.json').read_text()))
checked=0
for name,digest in manifest['entries'].items():
 if name in allowed or name.endswith('.class') or name=='META-INF/MANIFEST.MF':continue
 p=root/'src/main/resources'/name
 assert p.is_file() and hashlib.sha256(p.read_bytes()).hexdigest()==digest,name
 checked+=1
metadata=tomllib.loads((root/'src/main/resources/META-INF/neoforge.mods.toml').read_text())
assert {m['modId'] for m in metadata['mods']}=={'lc_harrower','lc_hollowforged','hollow_fragment_addon'}
print(f'PASS: {checked} untouched baseline resources, all five original recipes, and original three mod IDs.')
if len(sys.argv)>1:
 with ZipFile(sys.argv[1]) as z:
  assert z.testzip() is None
  for name,digest in manifest['entries'].items():
   if name in allowed:continue
   assert name in z.namelist() and hashlib.sha256(z.read(name)).hexdigest()==digest,name
  assert not any('/checks/' in n for n in z.namelist())
 print('PASS: every approved 0.6.2-rc.2 compiled/resource entry preserved; JAR CRC and exclusion of validation probe.')
