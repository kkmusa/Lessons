import http from 'node:http';
import {readFile} from 'node:fs/promises';
const root = new URL('./public/', import.meta.url);
const types = {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.json':'application/json','.webmanifest':'application/manifest+json','.svg':'image/svg+xml'};
http.createServer(async (req,res)=>{
 try {
 const path=decodeURIComponent(new URL(req.url,'http://localhost').pathname);
 const url=new URL('.'+(path==='/'?'/index.html':path),root);
 if(!url.href.startsWith(root.href)) {res.writeHead(403).end();return;}
 const data=await readFile(url);
 res.writeHead(200,{'Content-Type':types[url.pathname.slice(url.pathname.lastIndexOf('.'))]||'application/octet-stream','Cache-Control':'no-cache','X-Content-Type-Options':'nosniff'}).end(data);
 } catch {res.writeHead(404).end('Not found');}
}).listen(Number(process.env.PORT||3000),'0.0.0.0',()=>console.log('Lessons listening on port '+(process.env.PORT||3000)));
