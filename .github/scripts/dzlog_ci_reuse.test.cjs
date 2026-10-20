const { test } = require('node:test');
const assert = require('node:assert/strict');
const reuse = require('./dzlog_ci_reuse.cjs');

const entry = (path, sha = 'same') => ({ path, sha, type: 'blob', mode: '100644' });
const baseTree = { tree: [entry('build.gradle.kts'), entry('shared/src/commonMain/Policy.kt'),
  entry('app/src/main/Camera.kt'), entry('iosApp/PoCUITests/Test.swift'),
  entry('sharedUi/src/iosMain/Main.kt'), entry('.github/scripts/dzlog_ci_reuse.cjs')] };
function fixture(overrides = {}) {
  const context = { repo: { owner: 'owner', repo: 'repo' }, runId: 10, payload: { pull_request: {
    number: 74, base: { sha: 'base' }, head: { sha: 'new', ref: 'branch', repo: { full_name: 'owner/repo' } },
  } } };
  const run = { id: 9, head_sha: 'old', head_repository: { full_name: 'owner/repo' },
    updated_at: new Date(Date.now() - 1000).toISOString(),
    conclusion: 'success', event: 'pull_request', html_url: 'https://github.com/owner/repo/actions/runs/9',
    pull_requests: [{ number: 74, base: { sha: 'base' } }] };
  const github = { rest: {
    git: {
      getCommit: async ({ commit_sha }) => ({ data: { tree: { sha: commit_sha } } }),
      getTree: async ({ tree_sha }) => ({ data: tree_sha === 'new' ? (overrides.current || baseTree) : baseTree }),
    },
    repos: { compareCommitsWithBasehead: async () => ({ data: { merge_base_commit: { sha: overrides.base || 'base' } } }) },
    actions: { listWorkflowRuns: async () => ({ data: { workflow_runs: [Object.assign(run, overrides.run)] } }) },
  } };
  return { github, context, platform: overrides.platform || 'android' };
}

test('same successful inputs reuse explicit prior evidence', async () => {
  const result = await reuse.decide(fixture());
  assert.equal(result.run, false);
  assert.match(result.previous, /runs\/9$/);
});
test('iOS test changes rerun UI but reuse Android/core and retain framework key', async () => {
  const current = { tree: [...baseTree.tree.filter(e => !e.path.startsWith('iosApp/')), entry('iosApp/PoCUITests/Test.swift', 'changed')] };
  for (const platform of ['android', 'core']) assert.equal((await reuse.decide(fixture({ platform, current }))).run, false);
  const ui = await reuse.decide(fixture({ platform: 'ui', current }));
  assert.equal(ui.run, true);
  assert.equal(ui.framework, reuse.fingerprint(baseTree, 'framework'));
});
test('iOS Kotlin changes rerun UI and framework; Android/core remain identical', async () => {
  const current = { tree: [...baseTree.tree.filter(e => !e.path.startsWith('sharedUi/')), entry('sharedUi/src/iosMain/Main.kt', 'changed')] };
  assert.equal((await reuse.decide(fixture({ current }))).run, false);
  assert.equal((await reuse.decide(fixture({ platform: 'core', current }))).run, false);
  const ui = await reuse.decide(fixture({ platform: 'ui', current }));
  assert.equal(ui.run, true);
  assert.notEqual(ui.framework, reuse.fingerprint(baseTree, 'framework'));
});
test('changed common/build/unknown/workflow inputs invalidate their suites', async () => {
  for (const path of ['shared/src/commonMain/Policy.kt', 'build.gradle.kts', 'new-build-input', '.github/scripts/dzlog_ci_reuse.cjs']) {
    const current = { tree: [...baseTree.tree.filter(e => e.path !== path), entry(path, 'changed')] };
    for (const platform of ['android', 'core', 'ui']) assert.equal((await reuse.decide(fixture({ platform, current }))).run, true, `${platform} ${path}`);
  }
});
test('failed, other PR/base/repository, and current run evidence are never reused', async () => {
  for (const run of [{ conclusion: 'failure' }, { id: 10 }, { pull_requests: [{ number: 75, base: { sha: 'base' } }] },
    { pull_requests: [{ number: 74, base: { sha: 'different' } }] }, { head_repository: { full_name: 'other/repo' } },
    { updated_at: new Date(Date.now() - 25 * 3600 * 1000).toISOString() }]) {
    assert.equal((await reuse.decide(fixture({ run }))).run, true);
  }
});
test('diverged bases, push and external PR always run fresh checks', async () => {
  assert.equal((await reuse.decide(fixture({ base: 'different' }))).run, true);
  assert.equal((await reuse.decide(fixture({ platform: 'ui', base: 'different' }))).framework, undefined);
  const push = fixture(); delete push.context.payload.pull_request;
  assert.equal((await reuse.decide(push)).run, true);
  const external = fixture(); external.context.payload.pull_request.head.repo.full_name = 'other/repo';
  assert.equal((await reuse.decide(external)).run, true);
});
test('API failure and truncated input tree fail open to full validation', async () => {
  const denied = fixture();
  denied.github.rest.git.getTree = async () => { throw Error('denied'); };
  for (const input of [fixture({ current: { ...baseTree, truncated: true } }), denied]) {
    const outputs = {};
    await reuse({ ...input, core: { setOutput: (k, v) => { outputs[k] = v; }, warning() {}, info() {}, summary: {} } });
    assert.equal(outputs.run, 'true');
  }
});
