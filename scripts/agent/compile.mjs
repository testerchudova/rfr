import {readFileSync, existsSync} from 'node:fs';
import {dirname, extname, isAbsolute, relative, resolve, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const frontend = resolve(root, 'rfr-gql-client');

export function changedFiles(event) {
  const input = event.tool_input ?? {};
  const files = [input.file_path, input.filePath, input.path, event.tool_response?.filePath]
    .filter(value => typeof value === 'string');
  if (typeof input.pathInProject === 'string') {
    files.push(resolve(root, input.pathInProject));
  }
  const patch = typeof input === 'string' ? input : input.command ?? input.patch ?? '';
  if (typeof patch === 'string') {
    for (const match of patch.matchAll(/^\*\*\* (?:Add File|Update File|Delete File|Move to): (.+)\r?$/gm)) {
      files.push(match[1].trim());
    }
  }
  return [...new Set(files.map(file => {
    const absolute = isAbsolute(file) ? file : resolve(event.cwd ?? root, file);
    return relative(root, absolute).split(sep).join('/');
  }).filter(file => file && file !== '..' && !file.startsWith('../')))];
}

export function checksFor(files) {
  const checks = [];
  const modules = new Set();
  for (const file of files) {
    const module = file.split('/')[0];
    if (['rfr-auth', 'rfr-api', 'rfr-e2e'].includes(module) && extname(file) === '.java') {
      modules.add(module);
    }
  }
  for (const module of modules) {
    const task = `:${module}:${module === 'rfr-e2e' ? 'compileTestJava' : 'compileJava'}`;
    checks.push(process.platform === 'win32'
      ? {name: task, command: process.env.ComSpec ?? 'cmd.exe', args: ['/d', '/c', 'gradlew.bat', '--no-daemon', '-q', task], cwd: root}
      : {name: task, command: './gradlew', args: ['-q', task], cwd: root});
  }
  const filesInFrontend = files.filter(file => /^rfr-gql-client\/src\/.*\.tsx?$/.test(file));
  if (filesInFrontend.length) {
    checks.push({name: 'TypeScript', command: process.execPath,
      args: [resolve(frontend, 'node_modules/typescript/bin/tsc'), '--noEmit', '--project', 'tsconfig.json'], cwd: frontend});
    const lintFiles = filesInFrontend.filter(file => existsSync(resolve(root, file)))
      .map(file => file.slice('rfr-gql-client/'.length));
    if (lintFiles.length) {
      checks.push({name: 'ESLint', command: process.execPath,
        args: [resolve(frontend, 'node_modules/eslint/bin/eslint.js'), '--max-warnings', '0', '--', ...lintFiles], cwd: frontend});
    }
  }
  return checks;
}

export function compile(event) {
  const failures = [];
  for (const check of checksFor(changedFiles(event))) {
    const result = spawnSync(check.command, check.args, {
      cwd: check.cwd, encoding: 'utf8', timeout: 120_000, maxBuffer: 8 * 1024 * 1024,
      windowsHide: true,
    });
    if (result.status !== 0 || result.error) {
      const output = `${result.stdout ?? ''}\n${result.stderr ?? ''}`.trim().split(/\r?\n/).slice(-60).join('\n');
      failures.push(`[${check.name}] failed\n${result.error?.message ?? ''}\n${output}`.trim());
    }
  }
  return failures;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const files = process.argv.slice(2);
    const event = files.length
      ? {cwd: root, tool_input: {command: files.map(file => `*** Update File: ${file}`).join('\n')}}
      : JSON.parse(readFileSync(0, 'utf8').replace(/^\uFEFF/, ''));
    const failures = compile(event);
    if (failures.length) {
      const reason = failures.join('\n\n');
      if (event.hook_event_name === 'PostToolUse') {
        console.log(JSON.stringify({decision: 'block', reason}));
      } else {
        console.error(reason);
        process.exitCode = 2;
      }
    }
  } catch (error) {
    console.error(`Compilation hook failed: ${error.message}`);
    process.exitCode = 2;
  }
}
