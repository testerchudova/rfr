import assert from 'node:assert/strict';
import {test} from 'node:test';
import {checksFor} from './compile.mjs';

const tasksFor = files => checksFor(files).map(check => check.args.at(-1));

for (const module of ['rfr-auth', 'rfr-api']) {
  const main = `${module}/src/main/java/Example.java`;
  const tests = `${module}/src/test/java/ExampleTest.java`;

  test(`${module}: main sources compile without test sources`, () => {
    assert.deepEqual(tasksFor([main]), [`:${module}:compileJava`]);
  });

  test(`${module}: test sources require test compilation`, () => {
    assert.deepEqual(tasksFor([tests]), [`:${module}:compileTestJava`]);
  });

  test(`${module}: mixed changes compile tests once, regardless of file order`, () => {
    for (const files of [[main, tests], [tests, main]]) {
      assert.deepEqual(tasksFor(files), [`:${module}:compileTestJava`]);
    }
  });
}

test('e2e sources retain test compilation', () => {
  assert.deepEqual(tasksFor(['rfr-e2e/src/test/java/ExampleTest.java']), [':rfr-e2e:compileTestJava']);
});

test('each backend module selects its own source set', () => {
  assert.deepEqual(tasksFor([
    'rfr-api/src/main/java/Example.java',
    'rfr-auth/src/test/java/ExampleTest.java',
  ]), [':rfr-api:compileJava', ':rfr-auth:compileTestJava']);
});
