import {Socket} from 'node:net';
import {setTimeout as delay} from 'node:timers/promises';

const ports = [...new Set(process.argv.slice(2).map(Number))];
const timeoutSeconds = Number(process.env.RFR_STARTUP_TIMEOUT_SECONDS ?? 180);

if (!ports.length || ports.some(port => !Number.isInteger(port) || port < 1 || port > 65535)
    || !Number.isFinite(timeoutSeconds) || timeoutSeconds <= 0) {
  console.error('Usage: node scripts/ci/wait-for-ports.mjs PORT [PORT ...]; RFR_STARTUP_TIMEOUT_SECONDS must be positive.');
  process.exit(2);
}

const deadline = Date.now() + timeoutSeconds * 1000;
console.log(`Waiting up to ${timeoutSeconds} seconds for localhost ports: ${ports.join(', ')}`);

const results = await Promise.allSettled(ports.map(async port => {
  while (Date.now() < deadline) {
    const ready = await new Promise(resolve => {
      const socket = new Socket();
      const finish = connected => {
        socket.destroy();
        resolve(connected);
      };
      socket.setTimeout(Math.min(1000, Math.max(1, deadline - Date.now())));
      socket.once('connect', () => finish(true));
      socket.once('error', () => finish(false));
      socket.once('timeout', () => finish(false));
      socket.connect(port, 'localhost');
    });
    if (ready) {
      console.log(`localhost:${port} is ready`);
      return;
    }
    await delay(Math.max(0, Math.min(250, deadline - Date.now())));
  }
  throw new Error(`Timed out waiting for localhost:${port} after ${timeoutSeconds} seconds.`);
}));

const failures = results.filter(result => result.status === 'rejected');
for (const failure of failures) console.error(failure.reason.message);
if (failures.length) process.exitCode = 1;
