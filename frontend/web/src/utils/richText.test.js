import { describe, expect, it } from 'vitest'
import { plainTextFromHtml, richTextExcerpt, richTextLength, sanitizeRichText } from './richText.js'

describe('community rich text safety', () => {
  it('removes scripts and external embedded media', () => {
    const result = sanitizeRichText(
      '<p>Hello<script>alert(1)</script></p><img src="https://evil.example/image.png">',
    )

    expect(result).toContain('<p>Hello</p>')
    expect(result).not.toContain('script')
    expect(result).not.toContain('evil.example')
  })

  it('keeps approved internal image and video resources', () => {
    const image = '/api/file/getResource?sourceName=260828%2Fabcdefgh.png'
    const video = '/api/file/getResource?sourceName=260828%2Fabcdefgh.mp4'
    const result = sanitizeRichText(`<p>现场</p><img src="${image}"><video src="${video}"></video>`)

    expect(result).toContain('loading="lazy"')
    expect(result).toContain('controls=""')
  })

  it('creates readable excerpts from legacy and rich text', () => {
    expect(plainTextFromHtml('<p>演唱会 <strong>现场</strong></p>')).toBe('演唱会 现场')
    expect(richTextExcerpt('123456', 4)).toBe('1234…')
    expect(richTextLength('<p>知潮社区</p>')).toBe(4)
  })
})
