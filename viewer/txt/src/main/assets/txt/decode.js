// Kept free of the native bridge so it can be run outside a WebView.

const startsWith = (bytes, first, second) => bytes[0] === first && bytes[1] === second;

// A text file does not say how it is encoded. UTF-16 is only recognised by its byte order mark;
// anything that is not valid UTF-8 is taken to be the legacy Korean encoding (CP949), which is
// what Windows Notepad wrote by default for years.
export function decodeText(bytes) {
  if (startsWith(bytes, 0xff, 0xfe)) return new TextDecoder('utf-16le').decode(bytes);
  if (startsWith(bytes, 0xfe, 0xff)) return new TextDecoder('utf-16be').decode(bytes);
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(bytes);
  } catch (e) {
    return new TextDecoder('euc-kr').decode(bytes);
  }
}
