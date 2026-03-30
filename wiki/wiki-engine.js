/**
 * IssueFlow Wiki Engine
 * Data layer: storage, markdown parsing, search, versioning
 */
const WikiEngine = (function () {
  const PAGES_KEY = 'wiki:pages';
  const RECENT_KEY = 'wiki:recentEdits';
  const SETTINGS_KEY = 'wiki:settings';
  const MAX_RECENT = 100;
  const MAX_HISTORY = 50;

  // ── Storage Helpers ──────────────────────────────────────────────

  function loadPages() {
    try {
      return JSON.parse(localStorage.getItem(PAGES_KEY)) || {};
    } catch {
      return {};
    }
  }

  function savePages(pages) {
    localStorage.setItem(PAGES_KEY, JSON.stringify(pages));
  }

  function loadRecent() {
    try {
      return JSON.parse(localStorage.getItem(RECENT_KEY)) || [];
    } catch {
      return [];
    }
  }

  function saveRecent(recent) {
    localStorage.setItem(RECENT_KEY, JSON.stringify(recent.slice(0, MAX_RECENT)));
  }

  function loadSettings() {
    try {
      return JSON.parse(localStorage.getItem(SETTINGS_KEY)) || {};
    } catch {
      return {};
    }
  }

  function saveSettings(settings) {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  }

  // ── Slug Utilities ───────────────────────────────────────────────

  function slugify(title) {
    return title
      .toLowerCase()
      .trim()
      .replace(/[^\w\s-]/g, '')
      .replace(/[\s_]+/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');
  }

  function deslugify(slug) {
    return slug
      .replace(/-/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase());
  }

  // ── CRUD Operations ──────────────────────────────────────────────

  function getPage(slug) {
    const pages = loadPages();
    return pages[slug] || null;
  }

  function getAllPages() {
    const pages = loadPages();
    return Object.values(pages).sort((a, b) =>
      a.title.localeCompare(b.title)
    );
  }

  function savePage(slug, title, content, tags, editSummary) {
    const pages = loadPages();
    const existing = pages[slug];
    const now = Date.now();

    if (existing) {
      // Push current state to history
      const historyEntry = {
        content: existing.content,
        tags: [...existing.tags],
        timestamp: existing.updatedAt,
        summary: editSummary || 'Edit'
      };
      const history = [historyEntry, ...(existing.history || [])].slice(0, MAX_HISTORY);

      pages[slug] = {
        slug,
        title,
        content,
        tags: tags || [],
        createdAt: existing.createdAt,
        updatedAt: now,
        history
      };
    } else {
      pages[slug] = {
        slug,
        title,
        content,
        tags: tags || [],
        createdAt: now,
        updatedAt: now,
        history: []
      };
    }

    savePages(pages);

    // Record in recent edits
    const recent = loadRecent();
    recent.unshift({
      slug,
      title,
      timestamp: now,
      action: existing ? 'edited' : 'created'
    });
    saveRecent(recent);

    return pages[slug];
  }

  function deletePage(slug) {
    const pages = loadPages();
    const page = pages[slug];
    if (!page) return false;

    delete pages[slug];
    savePages(pages);

    const recent = loadRecent();
    recent.unshift({
      slug,
      title: page.title,
      timestamp: Date.now(),
      action: 'deleted'
    });
    saveRecent(recent);

    return true;
  }

  function getRecentChanges(limit) {
    return loadRecent().slice(0, limit || 20);
  }

  // ── Version History ──────────────────────────────────────────────

  function getHistory(slug) {
    const page = getPage(slug);
    if (!page) return [];
    return page.history || [];
  }

  function restoreVersion(slug, historyIndex) {
    const page = getPage(slug);
    if (!page || !page.history || !page.history[historyIndex]) return null;
    const version = page.history[historyIndex];
    return savePage(slug, page.title, version.content, version.tags, 'Restored earlier version');
  }

  // ── Markdown Parser ──────────────────────────────────────────────

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function parseInline(text) {
    let result = escapeHtml(text);

    // Wiki links [[Page Name]]
    result = result.replace(/\[\[([^\]]+)\]\]/g, (match, pageName) => {
      const targetSlug = slugify(pageName);
      const pages = loadPages();
      const exists = !!pages[targetSlug];
      const cls = exists ? 'wiki-link' : 'wiki-link wiki-link-missing';
      const href = exists
        ? `#/page/${targetSlug}`
        : `#/new?title=${encodeURIComponent(pageName)}`;
      return `<a href="${href}" class="${cls}">${escapeHtml(pageName)}</a>`;
    });

    // Images ![alt](url)
    result = result.replace(/!\[([^\]]*)\]\(([^)]+)\)/g,
      '<img src="$2" alt="$1" class="wiki-image" />');

    // Links [text](url)
    result = result.replace(/\[([^\]]+)\]\(([^)]+)\)/g,
      '<a href="$2" class="external-link" target="_blank" rel="noopener">$1</a>');

    // Bold **text**
    result = result.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

    // Italic *text*
    result = result.replace(/\*(.+?)\*/g, '<em>$1</em>');

    // Inline code `text`
    result = result.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Strikethrough ~~text~~
    result = result.replace(/~~(.+?)~~/g, '<del>$1</del>');

    return result;
  }

  function parse(content) {
    if (!content) return '';

    const lines = content.split('\n');
    const html = [];
    let inCodeBlock = false;
    let codeContent = [];
    let inList = false;
    let listType = '';
    let inBlockquote = false;
    let blockquoteContent = [];

    function closeList() {
      if (inList) {
        html.push(listType === 'ul' ? '</ul>' : '</ol>');
        inList = false;
        listType = '';
      }
    }

    function closeBlockquote() {
      if (inBlockquote) {
        html.push(`<blockquote>${blockquoteContent.map(l => parseInline(l)).join('<br>')}</blockquote>`);
        inBlockquote = false;
        blockquoteContent = [];
      }
    }

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      // Fenced code blocks
      if (line.trim().startsWith('```')) {
        if (inCodeBlock) {
          html.push(`<pre><code>${escapeHtml(codeContent.join('\n'))}</code></pre>`);
          codeContent = [];
          inCodeBlock = false;
        } else {
          closeList();
          closeBlockquote();
          inCodeBlock = true;
        }
        continue;
      }

      if (inCodeBlock) {
        codeContent.push(line);
        continue;
      }

      // Blank lines
      if (line.trim() === '') {
        closeList();
        closeBlockquote();
        continue;
      }

      // Headings
      const headingMatch = line.match(/^(#{1,6})\s+(.+)$/);
      if (headingMatch) {
        closeList();
        closeBlockquote();
        const level = headingMatch[1].length;
        const text = parseInline(headingMatch[2]);
        const id = slugify(headingMatch[2]);
        html.push(`<h${level} id="${id}">${text}</h${level}>`);
        continue;
      }

      // Horizontal rule
      if (/^(-{3,}|_{3,}|\*{3,})$/.test(line.trim())) {
        closeList();
        closeBlockquote();
        html.push('<hr>');
        continue;
      }

      // Blockquote
      if (line.trim().startsWith('> ')) {
        closeList();
        inBlockquote = true;
        blockquoteContent.push(line.trim().slice(2));
        continue;
      } else {
        closeBlockquote();
      }

      // Unordered list
      if (/^\s*[-*+]\s+/.test(line)) {
        if (!inList || listType !== 'ul') {
          closeList();
          html.push('<ul>');
          inList = true;
          listType = 'ul';
        }
        const text = parseInline(line.replace(/^\s*[-*+]\s+/, ''));
        html.push(`<li>${text}</li>`);
        continue;
      }

      // Ordered list
      if (/^\s*\d+\.\s+/.test(line)) {
        if (!inList || listType !== 'ol') {
          closeList();
          html.push('<ol>');
          inList = true;
          listType = 'ol';
        }
        const text = parseInline(line.replace(/^\s*\d+\.\s+/, ''));
        html.push(`<li>${text}</li>`);
        continue;
      }

      // Checkbox list
      if (/^\s*[-*+]\s+\[[ x]\]\s+/.test(line)) {
        if (!inList || listType !== 'ul') {
          closeList();
          html.push('<ul class="checklist">');
          inList = true;
          listType = 'ul';
        }
        const checked = /\[x\]/i.test(line);
        const text = parseInline(line.replace(/^\s*[-*+]\s+\[[ x]\]\s+/, ''));
        html.push(`<li class="check-item"><input type="checkbox" disabled ${checked ? 'checked' : ''}/> ${text}</li>`);
        continue;
      }

      // Regular paragraph
      closeList();
      html.push(`<p>${parseInline(line)}</p>`);
    }

    // Close any open blocks
    if (inCodeBlock) {
      html.push(`<pre><code>${escapeHtml(codeContent.join('\n'))}</code></pre>`);
    }
    closeList();
    closeBlockquote();

    return html.join('\n');
  }

  // ── Search ───────────────────────────────────────────────────────

  function search(query) {
    if (!query || !query.trim()) return [];

    const q = query.toLowerCase().trim();
    const pages = loadPages();
    const results = [];

    for (const page of Object.values(pages)) {
      const titleMatch = page.title.toLowerCase().includes(q);
      const contentMatch = page.content.toLowerCase().includes(q);

      if (titleMatch || contentMatch) {
        let snippet = '';
        if (contentMatch) {
          const idx = page.content.toLowerCase().indexOf(q);
          const start = Math.max(0, idx - 40);
          const end = Math.min(page.content.length, idx + q.length + 40);
          snippet = (start > 0 ? '...' : '') +
            page.content.slice(start, end) +
            (end < page.content.length ? '...' : '');
        }

        results.push({
          slug: page.slug,
          title: page.title,
          snippet,
          titleMatch,
          tags: page.tags,
          updatedAt: page.updatedAt
        });
      }
    }

    // Title matches first, then by recency
    results.sort((a, b) => {
      if (a.titleMatch && !b.titleMatch) return -1;
      if (!a.titleMatch && b.titleMatch) return 1;
      return b.updatedAt - a.updatedAt;
    });

    return results;
  }

  // ── Backlinks ────────────────────────────────────────────────────

  function getBacklinks(slug) {
    const pages = loadPages();
    const targetPage = pages[slug];
    if (!targetPage) return [];

    const backlinks = [];
    const titlePattern = targetPage.title.toLowerCase();

    for (const page of Object.values(pages)) {
      if (page.slug === slug) continue;
      const wikiLinks = page.content.match(/\[\[([^\]]+)\]\]/g) || [];
      for (const link of wikiLinks) {
        const linkTitle = link.slice(2, -2);
        if (slugify(linkTitle) === slug) {
          backlinks.push({ slug: page.slug, title: page.title });
          break;
        }
      }
    }

    return backlinks;
  }

  // ── Tags ─────────────────────────────────────────────────────────

  function getAllTags() {
    const pages = loadPages();
    const tagCounts = {};

    for (const page of Object.values(pages)) {
      for (const tag of (page.tags || [])) {
        tagCounts[tag] = (tagCounts[tag] || 0) + 1;
      }
    }

    return Object.entries(tagCounts)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  function getPagesByTag(tag) {
    const pages = loadPages();
    return Object.values(pages)
      .filter(p => (p.tags || []).includes(tag))
      .sort((a, b) => a.title.localeCompare(b.title));
  }

  // ── Settings ─────────────────────────────────────────────────────

  function getSetting(key) {
    return loadSettings()[key];
  }

  function setSetting(key, value) {
    const settings = loadSettings();
    settings[key] = value;
    saveSettings(settings);
  }

  // ── Seed Content ─────────────────────────────────────────────────

  function seedIfEmpty() {
    const pages = loadPages();
    if (Object.keys(pages).length > 0) return;

    savePage('welcome', 'Welcome', `# Welcome to IssueFlow

IssueFlow is your personal wiki for organizing thoughts, documenting projects, and connecting ideas.

## Getting Started

- Click **+ New Page** in the sidebar to create your first page
- Use \`[[Page Name]]\` syntax to link between pages
- Add **tags** to organize pages by topic
- Use the **search** to find anything instantly

## Formatting Guide

IssueFlow supports a simple markdown syntax:

- **Bold text** with \`**double asterisks**\`
- *Italic text* with \`*single asterisks*\`
- \`Inline code\` with backticks
- Headings with \`# H1\`, \`## H2\`, \`### H3\`
- Lists with \`-\` or \`1.\`
- Blockquotes with \`>\`
- Code blocks with triple backticks
- Wiki links with \`[[Page Name]]\`
- External links with \`[text](url)\`

## Example Pages

Check out these starter pages:

- [[Quick Notes]] - A scratchpad for ideas
- [[Project Ideas]] - Track your projects

---

*Built with care. Designed for clarity.*`, ['getting-started', 'help'], 'Initial seed');

    savePage('quick-notes', 'Quick Notes', `# Quick Notes

A place for quick thoughts and ideas.

## Today

- Start organizing your wiki
- Create pages for your projects
- Link related ideas with [[Wiki Links]]

## Tips

> The best way to organize information is to **start writing** and let structure emerge naturally.

Use tags to categorize pages, and wiki links to connect related concepts.

See also: [[Welcome]] for formatting help.`, ['notes'], 'Initial seed');

    savePage('project-ideas', 'Project Ideas', `# Project Ideas

Track and develop your project ideas here.

## Active Projects

### IssueFlow Wiki
A beautiful, Apple-inspired wiki for personal knowledge management.

**Status:** In progress
**Tags:** \`web\`, \`design\`, \`productivity\`

## Backlog

- Personal dashboard
- Reading list tracker
- Habit tracker

## Completed

- [x] Set up IssueFlow
- [x] Create starter pages
- [ ] Customize theme

---

Back to [[Welcome]]`, ['projects', 'planning'], 'Initial seed');
  }

  // ── Storage Info ─────────────────────────────────────────────────

  function getStorageInfo() {
    let totalSize = 0;
    for (const key of Object.keys(localStorage)) {
      if (key.startsWith('wiki:')) {
        totalSize += localStorage.getItem(key).length * 2; // UTF-16
      }
    }
    const pages = loadPages();
    return {
      pageCount: Object.keys(pages).length,
      storageUsedBytes: totalSize,
      storageUsedMB: (totalSize / (1024 * 1024)).toFixed(2)
    };
  }

  // ── Public API ───────────────────────────────────────────────────

  return {
    slugify,
    deslugify,
    getPage,
    getAllPages,
    savePage,
    deletePage,
    getRecentChanges,
    getHistory,
    restoreVersion,
    parse,
    parseInline,
    search,
    getBacklinks,
    getAllTags,
    getPagesByTag,
    getSetting,
    setSetting,
    seedIfEmpty,
    getStorageInfo,
    escapeHtml
  };
})();
