/**
 * Trigger a browser download from text content.
 * @param {string} content
 * @param {string} filename
 * @param {string} mimeType
 */
export function downloadTextFile(content, filename, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

/** @param {'csv'|'json'|'txt'} format */
export function exportFormatMime(format) {
  if (format === 'json') return 'application/json;charset=utf-8';
  if (format === 'txt') return 'text/plain;charset=utf-8';
  return 'text/csv;charset=utf-8';
}

/** @returns {string} e.g. 20260626-143052 */
export function timestampForFilename() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`;
}
