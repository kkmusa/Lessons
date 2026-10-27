import urllib.request, hashlib, zipfile, os, shutil, concurrent.futures
packages=[('platform-35_r02.zip','0bb560a90a7a2cbd0dd8348224d518b638fe7949','platforms/android-35'),('build-tools_r35_linux.zip','2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc','build-tools/35.0.0')]
def install(p):
 name,sha,dest=p; target='/workspace/android-sdk/'+dest
 if os.path.exists(target): return
 path='/tmp/'+name
 urllib.request.urlretrieve('https://dl.google.com/android/repository/'+name,path)
 assert hashlib.sha1(open(path,'rb').read()).hexdigest()==sha, 'Checksum mismatch'
 tmp='/tmp/extract-'+name;os.makedirs(tmp,exist_ok=True)
 with zipfile.ZipFile(path) as z:
  z.extractall(tmp)
  for i in z.infolist():
   mode=i.external_attr>>16
   if mode:os.chmod(tmp+'/'+i.filename,mode)
 root=os.listdir(tmp)[0];os.makedirs(os.path.dirname(target),exist_ok=True);shutil.move(tmp+'/'+root,target)
 print('Installed and verified',dest)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(install,packages))
url='https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar'
os.makedirs('/workspace/tools',exist_ok=True)
compiler='/workspace/tools/ecj.jar'
expected=urllib.request.urlopen(url+'.sha1').read().decode().strip()
if not os.path.exists(compiler) or hashlib.sha1(open(compiler,'rb').read()).hexdigest()!=expected:
 urllib.request.urlretrieve(url,compiler)
assert hashlib.sha1(open(compiler,'rb').read()).hexdigest()==expected
print('Eclipse Java compiler 3.40.0 verified')
