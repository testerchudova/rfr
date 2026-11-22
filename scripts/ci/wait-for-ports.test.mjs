import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {once} from 'node:events';
import {createServer} from 'node:net';
import {test} from 'node:test';
import {fileURLToPath} from 'node:url';

const helper = fileURLToPath(new URL('./wait-for-ports.mjs', import.meta.url));

async function listen(server, port = 0) {
  server.listen(port, 'localhost');
  await once(server, 'listening');
  return server.address().port;
}

async function unusedPort() {
  const server = createServer();
  const port = await listen(server);
  await new Promise(resolve => server.close(resolve));
  return port;
}

function runHelper(t, ports, timeoutSeconds = '2') {
  const child = spawn(process.execPath, [helper, ...ports.map(String)], {
    env: {...process.env, RFR_STARTUP_TIMEOUT_SECONDS: timeoutSeconds},
    windowsHide: true,
  });
  t.after(() => child.kill());
  let stdout = '';
  let stderr = '';
  child.stdout.on('data', data => { stdout += data; });
  child.stderr.on('data', data => { stderr += data; });
  const result = once(child, 'close').then(([code]) => ({code, stdout, stderr}));
  return {child, result};
}

test('all open ports are reported as ready', {timeout: 5000}, async t => {
  const servers = [createServer(socket => socket.end()), createServer(socket => socket.end())];
  t.after(() => servers.forEach(server => server.close()));
  const ports = await Promise.all(servers.map(server => listen(server)));
  const {result} = runHelper(t, ports);
  const output = await result;
  assert.equal(output.code, 0, output.stderr);
  for (const port of ports) assert.match(output.stdout, new RegExp(`localhost:${port} is ready`));
});

test('a port that opens later is retried until ready', {timeout: 5000}, async t => {
  const port = await unusedPort();
  const server = createServer(socket => socket.end());
  t.after(() => server.close());
  const {child, result} = runHelper(t, [port]);
  await once(child.stdout, 'data');
  await new Promise(resolve => setTimeout(resolve, 150));
  await listen(server, port);
  const output = await result;
  assert.equal(output.code, 0, output.stderr);
  assert.match(output.stdout, new RegExp(`localhost:${port} is ready`));
});

test('one unavailable port makes the command fail within the deadline', {timeout: 5000}, async t => {
  const server = createServer(socket => socket.end());
  t.after(() => server.close());
  const open = await listen(server);
  const closed = await unusedPort();
  const {result} = runHelper(t, [open, closed], '0.3');
  const output = await result;
  assert.equal(output.code, 1);
  assert.match(output.stdout, new RegExp(`localhost:${open} is ready`));
  assert.match(output.stderr, new RegExp(`Timed out waiting for localhost:${closed}`));
});

test('invalid arguments are rejected before waiting', {timeout: 5000}, async t => {
  const {result} = runHelper(t, ['invalid']);
  const output = await result;
  assert.equal(output.code, 2);
  assert.match(output.stderr, /Usage:/);
});
