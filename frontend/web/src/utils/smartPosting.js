export const postReviewFingerprint = (composer) =>
  JSON.stringify([
    composer.hubId,
    String(composer.title || '').trim(),
    String(composer.body || '').trim(),
    composer.type,
    composer.topicId || null,
  ])
