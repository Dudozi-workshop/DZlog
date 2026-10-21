const { createHash } = require('node:crypto');

const workflows = {
  android: 'dzlog-focused-tests.yml',
  'android-unit': 'dzlog-focused-tests.yml',
  core: 'dzlog-shared-ios-compile.yml',
  ui: 'dzlog-ios-ui-poc.yml',
  framework: 'dzlog-ios-ui-poc.yml',
};

// Reviewed presentation leaves only. Unknown UI files and all adapters/policies remain unit inputs.
const presentationLeaves = new Set([
  'common/DDZButton.kt', 'common/DDZCard.kt', 'common/DDZDialog.kt', 'common/DDZIconButton.kt',
  'common/DDZTopBar.kt', 'common/AlbumViewAction.kt', 'common/NextCaptureInfoFields.kt',
  'home/HomeContent.kt', 'home/HomeAmbientContent.kt', 'home/HomeRecentCaptureContent.kt',
  'home/HomeThreeZoneLayout.kt', 'home/HomeShadowBoundaryMask.kt', 'home/HomeUiSpec.kt',
  'home/HomeUtilityButton.kt', 'home/HomeSaveInfoSheet.kt',
  'theme/DDZColor.kt', 'theme/DDZLayout.kt', 'theme/DDZSpacing.kt',
  'theme/DDZTypography.kt', 'theme/DDZTheme.kt',
  'table/template/TableTemplateListContent.kt', 'table/template/TableTemplateReadOnlyPreviewDialog.kt',
  'table/mock/TemplateMiniPreview.kt', 'table/mock/TableEditorStylePanel.kt',
  'table/mock/TableEditorStructurePanel.kt',
]);
function presentationOnly(path) {
  return ['app/src/main/java/', 'sharedUi/src/commonMain/kotlin/'].some(root => {
    const prefix = root + 'com/dudoziworkshop/dzlog/ui/';
    return path.startsWith(prefix) && presentationLeaves.has(path.slice(prefix.length));
  });
}

function relevant(path, platform) {
  if (!workflows[platform]) throw new Error('Unknown validation platform');
  if (platform === 'android-unit') return !presentationOnly(path) && relevant(path, 'android');
  if (path === 'README.md' || path.startsWith('docs/')) return false;
  if (path.startsWith('.github/')) {
    return path === `.github/workflows/${workflows[platform]}` ||
      path === '.github/scripts/dzlog_ci_reuse.cjs' ||
      path === '.github/scripts/dzlog_ci_reuse.test.cjs' ||
      (platform === 'android' && path.startsWith('.github/scripts/'));
  }
  if (platform === 'android') {
    return !['iosApp/', 'scripts/ios/', 'sharedUi/src/iosMain/', 'shared/src/iosMain/']
      .some(prefix => path.startsWith(prefix));
  }
  if (platform === 'core') {
    return !['app/', 'sharedUi/', 'iosApp/', 'scripts/', 'billing-server/']
      .some(prefix => path.startsWith(prefix));
  }
  if (['app/', 'billing-server/'].some(prefix => path.startsWith(prefix))) return false;
  if (platform === 'framework' && ['iosApp/', 'scripts/ios/'].some(prefix => path.startsWith(prefix))) return false;
  return true; // Unknown files remain inputs: uncertainty triggers validation.
}

function fingerprint(tree, platform) {
  if (tree.truncated || !Array.isArray(tree.tree)) throw new Error('Incomplete Git tree');
  const records = tree.tree.filter(entry => entry.type !== 'tree' && relevant(entry.path, platform))
    .map(entry => `${entry.path}\0${entry.mode}\0${entry.type}\0${entry.sha}`)
    .sort();
  if (!records.length) throw new Error('Empty validation inputs');
  return createHash('sha256').update(records.join('\n')).digest('hex');
}

async function decide({ github, context, platform }) {
  const pr = context.payload.pull_request;
  if (context.payload.action === 'ready_for_review') {
    return { run: true, reason: 'Final PR review checkpoint: run full validation' };
  }
  if (!pr || pr.head.repo?.full_name !== `${context.repo.owner}/${context.repo.repo}`) {
    return { run: true, reason: 'Push, manual run, or external PR: run full validation' };
  }
  const args = context.repo;
  const trees = new Map();
  async function tree(sha) {
    if (!trees.has(sha)) {
      const commit = await github.rest.git.getCommit({ ...args, commit_sha: sha });
      const result = await github.rest.git.getTree({ ...args, tree_sha: commit.data.tree.sha, recursive: '1' });
      trees.set(sha, result.data);
    }
    return trees.get(sha);
  }
  const current = await tree(pr.head.sha);
  const hash = fingerprint(current, platform);
  const framework = platform === 'ui' ? fingerprint(current, 'framework') : undefined;
  const ancestry = await github.rest.repos.compareCommitsWithBasehead({ ...args, basehead: `${pr.base.sha}...${pr.head.sha}` });
  if (ancestry.data.merge_base_commit?.sha !== pr.base.sha) {
    return { run: true, hash, reason: 'Base diverged: require fresh validation without head-only cache' };
  }
  const result = await github.rest.actions.listWorkflowRuns({
    ...args, workflow_id: workflows[platform], event: 'pull_request', branch: pr.head.ref,
    status: 'success', per_page: 20,
  });
  const candidates = result.data.workflow_runs.filter(run =>
    run.id !== context.runId && run.conclusion === 'success' && run.event === 'pull_request' &&
    Date.now() - Date.parse(run.updated_at) >= 0 && Date.now() - Date.parse(run.updated_at) < 24 * 3600 * 1000 &&
    run.head_repository?.full_name === pr.head.repo.full_name &&
    run.pull_requests?.some(p => p.number === pr.number && p.base.sha === pr.base.sha)
  ).slice(0, platform === 'android-unit' ? 20 : 5);
  for (const run of candidates) {
    if (fingerprint(await tree(run.head_sha), platform) !== hash) continue;
    if (platform === 'android-unit' || platform === 'ui') {
      // A partial/reused green job is never proof that a complete suite actually ran.
      const fullStep = platform === 'ui' ? 'Complete iOS UI suite (no partial selection)'
        : 'Complete Android unit suite (no partial selection)';
      const jobs = await github.rest.actions.listJobsForWorkflowRun({ ...args, run_id: run.id, per_page: 100 });
      if (!jobs.data.jobs.some(job => job.conclusion === 'success' && job.steps?.some(step =>
        step.name === fullStep && step.conclusion === 'success'))) continue;
    }
    const previous = await github.rest.repos.compareCommitsWithBasehead({ ...args, basehead: `${pr.base.sha}...${run.head_sha}` });
    if (previous.data.merge_base_commit?.sha !== pr.base.sha) continue;
    return { run: false, framework, hash, previous: run.html_url, previousHead: run.head_sha,
      reason: platform === 'android-unit'
        ? 'Only reviewed presentation inputs differ; compile current Android, reuse actual prior full-suite evidence; no unit tests executed in this run'
        : 'Same validated inputs, same PR and base; no tests executed in this run' };
  }
  return { run: true, framework, hash, reason: 'No successful run with identical inputs: run full validation' };
}

module.exports = async function reuse({ github, context, core, platform }) {
  core.setOutput('run', 'true');
  try {
    const result = await decide({ github, context, platform });
    core.setOutput('run', String(result.run));
    if (result.framework) core.setOutput('framework', result.framework);
    core.info(result.reason);
    const summary = core.summary.addHeading(`DZlog ${platform}: ${result.run ? 'RUN' : 'REUSE'}`, 3)
      .addRaw(`${result.reason}\n\n`);
    if (result.hash) summary.addCodeBlock(result.hash);
    if (result.previous) summary.addLink(`Previous successful evidence (${result.previousHead})`, result.previous);
    await summary.write();
  } catch (error) {
    // Missing permissions, unavailable history, truncation, or any uncertainty runs the suite.
    core.setOutput('run', 'true');
    core.warning(`Validation reuse unavailable; running full suite: ${error.message}`);
  }
};
module.exports.decide = decide;
module.exports.fingerprint = fingerprint;
