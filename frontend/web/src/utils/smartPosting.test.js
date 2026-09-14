import { describe, expect, it } from 'vitest'
import { postReviewFingerprint } from './smartPosting.js'

describe('SmartPosting review binding', () => {
  it('invalidates the review after accepting optimized content', () => {
    const composer = {
      hubId: 5,
      title: 'Spring Boot 调用 Python 服务',
      body: '<p>原始正文</p>',
      type: 'QUESTION',
      topicId: 12,
    }
    const reviewedFingerprint = postReviewFingerprint(composer)

    composer.body = '<p>结构更清晰的优化正文</p>'

    expect(postReviewFingerprint(composer)).not.toBe(reviewedFingerprint)
  })
})
