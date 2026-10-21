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
      getTree: async ({ tree_sha }) => ({ data: tree_sha === 'new' ? (overrides.current || baseTree) : (overrides.previous || baseTree) }),
    },
    repos: { compareCommitsWithBasehead: async () => ({ data: { merge_base_commit: { sha: overrides.base || 'base' } } }) },
    actions: {
      listWorkflowRuns: async () => ({ data: { workflow_runs: [Object.assign(run, overrides.run)] } }),
      listJobsForWorkflowRun: async () => ({ data: { jobs: overrides.jobs || [{ conclusion: 'success', steps: [
        { name: 'Complete Android unit suite (no partial selection)', conclusion: 'success' },
      ] }] } }),
    },
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


test('reviewed panel moves and edits need compile, not another full unit suite', async () => {
  for (const leaf of ['TableEditorStylePanel.kt', 'TableEditorStructurePanel.kt']) {
    const current = { tree: [...baseTree.tree, entry('sharedUi/src/commonMain/kotlin/com/dudoziworkshop/dzlog/ui/table/mock/' + leaf, 'moved')] };
    assert.equal((await reuse.decide(fixture({ current }))).run, true);
    const result = await reuse.decide(fixture({ current, platform: 'android-unit' }));
    assert.equal(result.run, false);
    assert.match(result.reason, /compile current Android/);
  }
});
test('unit inputs keep adapters, state, policies, tests, build and unknown UI changes', async () => {
  for (const path of ['app/src/main/java/com/dudoziworkshop/dzlog/ui/home/HomeScreen.kt',
    'app/src/main/java/com/dudoziworkshop/dzlog/ui/table/mock/TableEditorV2StructureController.kt',
    'shared/src/commonMain/Policy.kt', 'app/src/test/NewTest.kt', 'build.gradle.kts',
    '.github/workflows/dzlog-focused-tests.yml', '.github/scripts/dzlog_ci_reuse.cjs',
    'sharedUi/src/commonMain/kotlin/com/dudoziworkshop/dzlog/ui/table/mock/UnknownPanel.kt']) {
    const current = { tree: [...baseTree.tree.filter(e => e.path !== path), entry(path, 'changed')] };
    assert.equal((await reuse.decide(fixture({ current, platform: 'android-unit' }))).run, true, path);
  }
});
test('compile-only/reused/failed full-suite jobs cannot supply unit evidence', async () => {
  for (const jobs of [[], [{ conclusion: 'success', steps: [
    { name: 'Complete Android unit suite (no partial selection)', conclusion: 'skipped' },
  ] }], [{ conclusion: 'failure', steps: [
    { name: 'Complete Android unit suite (no partial selection)', conclusion: 'success' },
  ] }], [{ conclusion: 'success', steps: [{ name: 'Compile Android', conclusion: 'success' }] }]]) {
    assert.equal((await reuse.decide(fixture({ platform: 'android-unit', jobs }))).run, true);
  }
});
test('unit-only evidence uses the same PR/base/repository/freshness gates', async () => {
  for (const run of [{ conclusion: 'failure' }, { id: 10 },
    { updated_at: new Date(Date.now() - 25 * 3600 * 1000).toISOString() },
    { pull_requests: [{ number: 75, base: { sha: 'base' } }] },
    { head_repository: { full_name: 'other/repo' } }]) {
    assert.equal((await reuse.decide(fixture({ platform: 'android-unit', run }))).run, true);
  }
  assert.equal((await reuse.decide(fixture({ platform: 'android-unit', base: 'different' }))).run, true);
});

test('moving an existing panel across modules keeps unit inputs while Android compile changes', async () => {
  const stem = 'com/dudoziworkshop/dzlog/ui/table/mock/TableEditorStructurePanel.kt';
  const previous = { tree: [...baseTree.tree, entry('app/src/main/java/' + stem)] };
  const current = { tree: [...baseTree.tree, entry('sharedUi/src/commonMain/kotlin/' + stem)] };
  assert.equal((await reuse.decide(fixture({ current, previous }))).run, true);
  assert.equal((await reuse.decide(fixture({ current, previous, platform: 'android-unit' }))).run, false);
});
test('unit-evidence API errors require the full suite', async () => {
  const input = fixture({ platform: 'android-unit' });
  input.github.rest.actions.listJobsForWorkflowRun = async () => { throw Error('denied'); };
  const outputs = {};
  await reuse({ ...input, core: { setOutput: (k, v) => { outputs[k] = v; }, warning() {}, info() {}, summary: {} } });
  assert.equal(outputs.run, 'true');
});

test('marking a PR ready for final review always runs the full suite', async () => {
  for (const platform of ['android', 'android-unit', 'core', 'ui']) {
    const input = fixture({ platform }); input.context.payload.action = 'ready_for_review';
    assert.equal((await reuse.decide(input)).run, true);
  }
});


test('focused iOS success cannot replace a complete UI regression result', async () => {
  for (const steps of [[],
    [{ name: 'Focused iOS editor flow (partial development check)', conclusion: 'success' }],
    [{ name: 'Complete iOS UI suite (no partial selection)', conclusion: 'skipped' }]]) {
    assert.equal((await reuse.decide(fixture({ platform: 'ui', jobs: [{ conclusion: 'success', steps }] }))).run, true);
  }
  assert.equal((await reuse.decide(fixture({ platform: 'ui', jobs: [{ conclusion: 'success', steps: [
    { name: 'Complete iOS UI suite (no partial selection)', conclusion: 'success' },
  ] }] }))).run, false);
});
