"""Package freshly compiled classes and resources; verify all unrelated 0.6.2-rc.1 entries are unchanged."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED,ZipInfo
import hashlib,json
root=Path(__file__).resolve().parents[1]
baseline=root/'reference/reference-harrower-0.6.0-rc.1.jar'
out=root/'build/libs/harrower-neoforge-1.21.1-1.0.0.jar'
files={}
for base in [root/'build/classes/java/main',root/'build/resources/main']:
 for p in base.rglob('*'):
  if p.is_file():files[p.relative_to(base).as_posix()]=p.read_bytes()
manifest=json.loads((root/'reference/release-approved-entry-hashes.json').read_text())
files['META-INF/MANIFEST.MF']=b'Manifest-Version: 1.0\r\n\r\n'
allowed=set(json.loads((root/'reference/release-allowed-changes.json').read_text()))
for name,digest in manifest['entries'].items():
 if name in allowed:continue
 assert name in files,'Missing baseline entry '+name
 assert hashlib.sha256(files[name]).hexdigest()==digest,'Unrelated baseline content changed: '+name
preserved=len(manifest['entries'])-len(allowed)
with ZipFile(out,'w',ZIP_DEFLATED,compresslevel=9) as z:
 for name,data in sorted(files.items()):
  info=ZipInfo(name,(1980,1,1,0,0,0));info.compress_type=ZIP_DEFLATED;info.external_attr=0o644<<16;z.writestr(info,data)
with ZipFile(out) as z:assert z.testzip() is None
report={'baseline_sha256':manifest['baseline_sha256'],'jar_sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'preserved_original_entries':preserved,'modified_original_entries':sorted(allowed),'entries':len(files)}
(root/'build/preservation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
