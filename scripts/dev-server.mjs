import http from 'node:http';
import { readFile, realpath } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = await realpath(fileURLToPath(new URL('../', import.meta.url)));
const args = process.argv.slice(2);
function option(name, fallback) {
  const index = args.indexOf(name);
  const inline = args.find(value => value.startsWith(name + '='));
  return inline ? inline.slice(name.length + 1) : index >= 0 ? args[index + 1] : fallback;
}
const port = Number(option('--port', process.env.PORT || '5173'));
const host = option('--host', '127.0.0.1');
const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.webp': 'image/webp', '.jpg': 'image/jpeg', '.png': 'image/png', '.svg': 'image/svg+xml' };
const server = http.createServer(async (req, res) => {
  if (!['GET', 'HEAD'].includes(req.method)) {
    res.writeHead(405, { Allow: 'GET, HEAD' });
    return res.end();
  }
  try {
    const url = new URL(req.url, 'http://localhost');
    const name = decodeURIComponent(url.pathname).replace(/^\/+/, '') || 'index.html';
    // Only public frontend files are served; backend, Git and dotfiles stay private.
    const allowed = name === 'index.html' || name === 'frontend/index.html' || /^(css|js|assets)\//.test(name);
    if (!allowed || name.split('/').some(part => part.startsWith('.'))) throw new Error('Not public');
    const file = await realpath(path.resolve(root, name));
    if (!file.startsWith(root + path.sep) || !types[path.extname(file)]) throw new Error('Not public');
    const content = await readFile(file);
    res.writeHead(200, { 'Content-Type': types[path.extname(file)], 'Content-Length': content.length, 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
    res.end(req.method === 'HEAD' ? undefined : content);
  } catch {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Not found');
  }
});
server.listen(port, host, () => console.log(`Hotel Vista preview: http://${host}:${port}`));
