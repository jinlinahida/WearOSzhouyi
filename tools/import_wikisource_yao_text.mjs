#!/usr/bin/env node

/**
 * Generates engine/src/main/assets/yao_text.json from Chinese Wikisource.
 *
 * This is intentionally a one-purpose import tool, not runtime code. It
 * records each source revision in the generated JSON so later updates are
 * reviewable. See docs/research/yao-text-data-source.md before running it.
 */
import { mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const sourceBaseUrl = "https://zh.wikisource.org";
const titlesInKingWenOrder = [
  "乾", "坤", "屯", "蒙", "需", "訟", "師", "比", "小畜", "履", "泰", "否", "同人", "大有", "謙", "豫",
  "隨", "蠱", "臨", "觀", "噬嗑", "賁", "剝", "復", "无妄", "大畜", "頤", "大過", "坎", "離", "咸", "恒",
  "遯", "大壯", "晉", "明夷", "家人", "睽", "蹇", "解", "損", "益", "夬", "姤", "萃", "升", "困", "井",
  "革", "鼎", "震", "艮", "漸", "歸妹", "豐", "旅", "巽", "兌", "渙", "節", "中孚", "小過", "既濟", "未濟",
];
const trigramBitsFromBottom = {
  乾: "111",
  兌: "110",
  離: "101",
  震: "100",
  巽: "011",
  坎: "010",
  艮: "001",
  坤: "000",
};
const expectedLabels = [
  /^初[九六]$/,
  /^[九六]二$/,
  /^[九六]三$/,
  /^[九六]四$/,
  /^[九六]五$/,
  /^上[九六]$/,
];
const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const outputPath = resolve(scriptDirectory, "../engine/src/main/assets/yao_text.json");

async function main() {
  // MediaWiki accepts multiple titles in a single request. Batching keeps this
  // one-off import gentle on Wikisource and avoids a request per hexagram.
  const entries = await fetchAllHexagrams();
  const codes = new Set(entries.map((entry) => entry.code));
  if (codes.size !== 64) {
    throw new Error(`Expected 64 unique hexagram codes, got ${codes.size}.`);
  }
  if (entries.flatMap((entry) => entry.lines).length !== 384) {
    throw new Error("Expected exactly 384 positional line texts.");
  }

  const dataset = {
    schemaVersion: 1,
    source: {
      name: "中文维基文库《周易》",
      workStatus: "Public domain source work (PD-old)",
      transcriptionLicense: "CC BY-SA 4.0",
      licenseUrl: "https://creativecommons.org/licenses/by-sa/4.0/",
      sourceIndexUrl: "https://zh.wikisource.org/w/index.php?title=周易&oldid=7907208",
      retrievedAt: new Date().toISOString().slice(0, 10),
      modification: "移除了 MediaWiki 展示标记和版式脚注，并重排为 64×6 的位置索引；未加入译文或解释。",
    },
    hexagrams: entries,
  };

  await mkdir(dirname(outputPath), { recursive: true });
  await writeFile(outputPath, `${JSON.stringify(dataset, null, 2)}\n`, "utf8");
  process.stdout.write(`Wrote ${outputPath} with ${entries.length * 6} line texts.\n`);
}

async function fetchAllHexagrams() {
  const pageByTitle = new Map();
  for (const titles of chunks(titlesInKingWenOrder, 50)) {
    const apiUrl = new URL("/w/api.php", sourceBaseUrl);
    apiUrl.search = new URLSearchParams({
      action: "query",
      format: "json",
      formatversion: "2",
      prop: "revisions",
      rvprop: "ids|content",
      rvslots: "main",
      titles: titles.map((title) => `周易/${title}`).join("|"),
    }).toString();
    const response = await fetchWithRetry(apiUrl, titles.join("、"));
    const pages = (await response.json()).query?.pages ?? [];
    pages.forEach((page) => pageByTitle.set(page.title, page));
  }
  return titlesInKingWenOrder.map((title) => parseHexagramPage(pageByTitle.get(`周易/${title}`), title));
}

function parseHexagramPage(page, title) {
  const revision = page?.revisions?.[0];
  const content = revision?.slots?.main?.content;
  if (!content || !revision?.revid) throw new Error(`No wikitext or revision for ${title}.`);
  const normalized = normalizeWikitext(content);
  const shape = normalized.match(/([乾兌離震巽坎艮坤])下([乾兌離震巽坎艮坤])上/);
  if (!shape) throw new Error(`Unable to determine trigram shape for ${title}.`);
  const lines = extractLines(normalized, title);
  const classics = extractClassics(normalized, title);

  return {
    code: trigramBitsFromBottom[shape[1]] + trigramBitsFromBottom[shape[2]],
    name: title,
    sourceRevision: revision.revid,
    sourceUrl: `${sourceBaseUrl}/w/index.php?title=${encodeURIComponent(`周易/${title}`)}&oldid=${revision.revid}`,
    ...classics,
    lines,
  };
}

function extractClassics(wikitext, title) {
  const guaSection = section(wikitext, "'''易經：'''", "'''彖曰：'''", title);
  const guaText = firstBullet(guaSection, false, title);
  const tuanText = firstBullet(section(wikitext, "'''彖曰：'''", "'''象曰：'''", title), false, title);
  const imageSection = section(wikitext, "'''象曰：'''", "'''文言曰：'''", title, true);
  const imageText = firstBullet(imageSection, false, title);
  return { guaText, tuanText, imageText };
}

function section(value, startMarker, endMarker, title, allowEnd = false) {
  const start = value.indexOf(startMarker);
  const end = value.indexOf(endMarker, start);
  if (start < 0 || (end < 0 && !allowEnd)) throw new Error(`Unable to find ${startMarker} section for ${title}.`);
  return value.slice(start + startMarker.length, end < 0 ? value.length : end);
}

function firstBullet(value, numbered, title) {
  const prefix = numbered ? "*#" : "*";
  const line = value.split("\n").find((item) => item.startsWith(prefix) && (numbered || !item.startsWith("*#")));
  if (!line) throw new Error(`Unable to find reference text for ${title}.`);
  const cleaned = cleanLine(line.slice(prefix.length));
  return (cleaned?.text ?? line.slice(prefix.length))
    .replace(/^\*+\s*/, "")
    .replace(/<[^>]+>/g, "")
    .replace(/'''|''/g, "")
    .replace(/\[\[([^\]|]+)\|([^\]]+)]]/g, "$2")
    .replace(/\[\[([^\]]+)]]/g, "$1")
    .replace(/{{[^{}]*}}/g, "")
    .trim();
}

async function fetchWithRetry(apiUrl, title) {
  for (let attempt = 0; attempt < 4; attempt += 1) {
    const response = await fetch(apiUrl, {
      headers: { "User-Agent": "BoompalaYaoTextImporter/1.0 (offline data provenance)" },
    });
    if (response.status !== 429 || attempt === 3) {
      if (!response.ok) throw new Error(`Unable to fetch ${title}: HTTP ${response.status}`);
      return response;
    }
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 2_000 * (attempt + 1)));
  }
  throw new Error(`Unable to fetch ${title}.`);
}

function extractLines(wikitext, title) {
  const start = wikitext.indexOf("'''易經：'''");
  const end = wikitext.indexOf("'''彖曰：'''", start);
  if (start < 0 || end < 0) throw new Error(`Unable to find 易經 section for ${title}.`);

  const lines = wikitext
    .slice(start, end)
    .split("\n")
    .filter((line) => line.startsWith("*#"))
    .map((line) => cleanLine(line.slice(2)))
    .filter((line) => line !== null);

  if (lines.length !== 6) {
    throw new Error(`Expected six positional lines for ${title}, got ${lines.length}.`);
  }
  lines.forEach((line, index) => {
    if (!expectedLabels[index].test(line.label)) {
      throw new Error(`Unexpected line label ${line.label} at ${title} position ${index}.`);
    }
  });
  return lines.map((line, position) => ({ position, text: line.text }));
}

function cleanLine(line) {
  const cleaned = line
    .replace(/<[^>]+>/g, "")
    .replace(/'''|''/g, "")
    .replace(/\[\[([^\]|]+)\|([^\]]+)]]/g, "$2")
    .replace(/\[\[([^\]]+)]]/g, "$1")
    .replace(/{{[^{}]*}}/g, "")
    .trim();
  if (/[{}[\]]/.test(cleaned)) {
    throw new Error(`Unexpected unparsed MediaWiki markup: ${cleaned}`);
  }
  const match = cleaned.match(/^(初[九六]|[九六][二三四五]|上[九六]|用[九六])[：，]\s*(.+)$/);
  if (!match) return null;
  const [, label, text] = match;
  if (label === "用九" || label === "用六") return null;
  return { label, text: text.trim() };
}

function normalizeWikitext(value) {
  let normalized = value;
  while (/-\{([^{}]*)}-/.test(normalized)) {
    normalized = normalized.replace(/-\{([^{}]*)}-/g, "$1");
  }
  return normalized;
}

function chunks(values, size) {
  const groups = [];
  for (let index = 0; index < values.length; index += size) {
    groups.push(values.slice(index, index + size));
  }
  return groups;
}

main().catch((error) => {
  process.stderr.write(`${error.stack ?? error}\n`);
  process.exitCode = 1;
});
