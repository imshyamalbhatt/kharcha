// Builds a Claude Artifact preview of the app from index.html.
// Usage: node tools/make-preview.js <output.html>
// The artifact host supplies <html>/<head>/<body>, so those wrappers and the
// phone-only tags (manifest, icons) are stripped; everything else is the real app.
const fs = require('fs'), path = require('path');
const src = fs.readFileSync(path.join(__dirname, '..', 'index.html'), 'utf8');
const out = src
  .replace(/<!doctype html>\s*/i, '')
  .replace(/<\/?html[^>]*>\s*/gi, '')
  .replace(/<\/?head>\s*/gi, '')
  .replace(/<\/?body>\s*/gi, '')
  .replace(/<meta (charset|name="viewport")[^>]*>\s*/gi, '')
  .replace(/<link rel="(manifest|icon|apple-touch-icon)"[^>]*>\s*/gi, '');
fs.writeFileSync(process.argv[2] || 'preview.html', out);
console.log('preview written');
