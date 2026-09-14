import DOMPurify from 'dompurify'

const ALLOWED_TAGS = [
  'p',
  'br',
  'h2',
  'h3',
  'strong',
  'b',
  'em',
  'i',
  'u',
  's',
  'strike',
  'blockquote',
  'ul',
  'ol',
  'li',
  'code',
  'pre',
  'a',
  'img',
  'video',
]

const RICH_TEXT_TAG =
  /<\s*\/?\s*(?:p|br|h2|h3|strong|b|em|i|u|s|strike|blockquote|ul|ol|li|code|pre|a|img|video)(?:\s|\/?>)/i
const INTERNAL_MEDIA =
  /^\/api\/file\/getResource\?sourceName=\d{6}(?:%2F|\/)[A-Za-z0-9_-]{8,80}\.(?:jpe?g|png|gif|webp|avif|mp4|webm|mov)$/i

const escapeHtml = (value) =>
  String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')

const legacyTextToHtml = (value) =>
  String(value || '')
    .replace(/\r\n?/g, '\n')
    .split(/\n{2,}/)
    .map((paragraph) => `<p>${escapeHtml(paragraph).replaceAll('\n', '<br>')}</p>`)
    .join('')

export const sanitizeRichText = (value) => {
  if (!value) return ''
  const source = RICH_TEXT_TAG.test(value) ? String(value) : legacyTextToHtml(value)
  const clean = DOMPurify.sanitize(source, {
    ALLOWED_TAGS,
    ALLOWED_ATTR: [
      'href',
      'title',
      'target',
      'rel',
      'src',
      'alt',
      'loading',
      'decoding',
      'controls',
      'preload',
      'playsinline',
    ],
    ALLOW_DATA_ATTR: false,
  })

  const document = new DOMParser().parseFromString(`<div>${clean}</div>`, 'text/html')
  document.querySelectorAll('a').forEach((link) => {
    const href = link.getAttribute('href') || ''
    if (!/^https?:\/\//i.test(href)) {
      link.removeAttribute('href')
      link.removeAttribute('target')
      link.removeAttribute('rel')
      return
    }
    link.setAttribute('target', '_blank')
    link.setAttribute('rel', 'nofollow noopener noreferrer')
  })
  document.querySelectorAll('img').forEach((image) => {
    if (
      !INTERNAL_MEDIA.test(image.getAttribute('src') || '') ||
      !/\.(?:jpe?g|png|gif|webp|avif)$/i.test(image.getAttribute('src') || '')
    )
      return image.remove()
    image.setAttribute('loading', 'lazy')
    image.setAttribute('decoding', 'async')
  })
  document.querySelectorAll('video').forEach((video) => {
    if (
      !INTERNAL_MEDIA.test(video.getAttribute('src') || '') ||
      !/\.(?:mp4|webm|mov)$/i.test(video.getAttribute('src') || '')
    )
      return video.remove()
    video.setAttribute('controls', '')
    video.setAttribute('preload', 'metadata')
    video.setAttribute('playsinline', '')
  })
  return document.body.firstElementChild?.innerHTML || ''
}

export const plainTextFromHtml = (value) => {
  if (!value) return ''
  const source = RICH_TEXT_TAG.test(value) ? String(value) : escapeHtml(value)
  const document = new DOMParser().parseFromString(source, 'text/html')
  return (document.body.textContent || '').replace(/\s+/g, ' ').trim()
}

export const richTextExcerpt = (value, limit = 260) => {
  const text = plainTextFromHtml(value)
  return text.length > limit ? `${text.slice(0, limit).trimEnd()}…` : text
}

export const richTextLength = (value) => plainTextFromHtml(value).length
