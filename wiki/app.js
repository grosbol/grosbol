/**
 * IssueFlow Wiki — Application Controller
 * SPA router, view renderers, event handling
 */
const App = (function () {
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => [...document.querySelectorAll(sel)];

  // ── Icons (inline SVG) ───────────────────────────────────────────

  const icons = {
    edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>',
    trash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>',
    clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
    tag: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>',
    page: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>',
    search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>',
    back: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>',
    restore: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>',
    home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>',
    empty: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>'
  };

  // ── Time Formatting ──────────────────────────────────────────────

  function timeAgo(timestamp) {
    const seconds = Math.floor((Date.now() - timestamp) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    const months = Math.floor(days / 30);
    if (months < 12) return `${months}mo ago`;
    return `${Math.floor(months / 12)}y ago`;
  }

  function formatDate(timestamp) {
    return new Date(timestamp).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  // ── Router ───────────────────────────────────────────────────────

  function getRoute() {
    const hash = window.location.hash || '#/';
    const [path, queryString] = hash.slice(1).split('?');
    const params = new URLSearchParams(queryString || '');
    const segments = path.split('/').filter(Boolean);

    return { path, segments, params };
  }

  function navigate(hash) {
    window.location.hash = hash;
  }

  function renderView() {
    const route = getRoute();
    const view = $('#view');
    view.style.animation = 'none';
    // Force reflow
    void view.offsetHeight;
    view.style.animation = '';

    const seg = route.segments;

    if (seg[0] === 'page' && seg[1]) {
      renderPage(seg[1]);
    } else if (seg[0] === 'edit' && seg[1]) {
      renderEditor(seg[1]);
    } else if (seg[0] === 'new') {
      renderNewPage(route.params.get('title') || '');
    } else if (seg[0] === 'search') {
      renderSearch(route.params.get('q') || '');
    } else if (seg[0] === 'tags') {
      renderTags();
    } else if (seg[0] === 'tag' && seg[1]) {
      renderTagResults(decodeURIComponent(seg[1]));
    } else if (seg[0] === 'history' && seg[1]) {
      renderHistory(seg[1]);
    } else if (seg[0] === 'version' && seg[1] && seg[2] !== undefined) {
      renderVersion(seg[1], parseInt(seg[2]));
    } else {
      renderHome();
    }

    updateSidebar();
    closeSidebar();
  }

  // ── View: Home ───────────────────────────────────────────────────

  function renderHome() {
    const pages = WikiEngine.getAllPages();
    const recent = WikiEngine.getRecentChanges(15);
    const tags = WikiEngine.getAllTags();
    const info = WikiEngine.getStorageInfo();

    let html = `
      <div class="home-hero">
        <h1>Welcome to IssueFlow</h1>
        <p>Your personal wiki for organizing ideas, notes, and knowledge.</p>
      </div>

      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-value">${info.pageCount}</div>
          <div class="stat-label">Pages</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">${tags.length}</div>
          <div class="stat-label">Tags</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">${info.storageUsedMB}</div>
          <div class="stat-label">MB Used</div>
        </div>
      </div>

      <div class="home-grid">
        <div>
          <div class="home-section-title">Recent Activity</div>
          <div class="card">`;

    if (recent.length === 0) {
      html += `
        <div class="empty-state">
          ${icons.empty}
          <h3>No activity yet</h3>
          <p>Create your first page to get started.</p>
          <button class="btn btn-primary" data-action="new-page">Create a Page</button>
        </div>`;
    } else {
      for (const item of recent) {
        html += `
          <div class="activity-item">
            <div class="activity-dot ${item.action}"></div>
            <span class="activity-action">${item.action}</span>
            <span class="activity-title" data-action="navigate" data-href="#/page/${item.slug}">${WikiEngine.escapeHtml(item.title)}</span>
            <span class="activity-time">${timeAgo(item.timestamp)}</span>
          </div>`;
      }
    }

    html += `</div></div><div>
      <div class="home-section-title">All Pages</div>`;

    if (pages.length === 0) {
      html += `
        <div class="card">
          <div class="empty-state">
            ${icons.empty}
            <h3>Start your wiki</h3>
            <p>Pages you create will appear here.</p>
          </div>
        </div>`;
    } else {
      for (const page of pages) {
        const tagHtml = (page.tags || []).map(t =>
          `<a class="tag" data-action="navigate" data-href="#/tag/${encodeURIComponent(t)}">${WikiEngine.escapeHtml(t)}</a>`
        ).join('');
        html += `
          <div class="card card-clickable" data-action="navigate" data-href="#/page/${page.slug}">
            <div class="card-title">${WikiEngine.escapeHtml(page.title)}</div>
            <div class="card-meta">${timeAgo(page.updatedAt)}</div>
            ${tagHtml ? `<div class="page-tags" style="margin-top:8px">${tagHtml}</div>` : ''}
          </div>`;
      }
    }

    html += `</div></div>`;
    $('#view').innerHTML = html;
  }

  // ── View: Page ───────────────────────────────────────────────────

  function renderPage(slug) {
    const page = WikiEngine.getPage(slug);

    if (!page) {
      $('#view').innerHTML = `
        <div class="empty-state">
          ${icons.empty}
          <h3>Page not found</h3>
          <p>The page "${WikiEngine.deslugify(slug)}" doesn't exist yet.</p>
          <button class="btn btn-primary" data-action="navigate" data-href="#/new?title=${encodeURIComponent(WikiEngine.deslugify(slug))}">Create It</button>
        </div>`;
      return;
    }

    const backlinks = WikiEngine.getBacklinks(slug);
    const tagsHtml = (page.tags || []).map(t =>
      `<a class="tag" data-action="navigate" data-href="#/tag/${encodeURIComponent(t)}">${icons.tag} ${WikiEngine.escapeHtml(t)}</a>`
    ).join('');

    let html = `
      <div class="page-header">
        <h1 class="page-title">${WikiEngine.escapeHtml(page.title)}</h1>
        <div class="page-actions">
          <button class="btn btn-ghost btn-sm" data-action="navigate" data-href="#/history/${slug}" title="History">
            ${icons.clock}
          </button>
          <button class="btn btn-secondary btn-sm" data-action="navigate" data-href="#/edit/${slug}" title="Edit">
            ${icons.edit} Edit
          </button>
          <button class="btn btn-danger btn-sm" data-action="delete-page" data-slug="${slug}" title="Delete">
            ${icons.trash}
          </button>
        </div>
      </div>

      <div class="page-body">
        ${WikiEngine.parse(page.content)}
      </div>

      <div class="page-footer">`;

    if (tagsHtml) {
      html += `<div class="page-tags">${tagsHtml}</div>`;
    }

    html += `
        <div class="page-meta-row">
          <span>Created ${formatDate(page.createdAt)}</span>
          <span>Last edited ${timeAgo(page.updatedAt)}</span>
        </div>`;

    if (backlinks.length > 0) {
      html += `
        <div class="backlinks">
          <div class="backlinks-title">Linked from</div>
          <div class="backlinks-list">
            ${backlinks.map(bl =>
              `<a class="tag" data-action="navigate" data-href="#/page/${bl.slug}">${WikiEngine.escapeHtml(bl.title)}</a>`
            ).join('')}
          </div>
        </div>`;
    }

    html += `</div>`;
    $('#view').innerHTML = html;

    WikiEngine.setSetting('lastVisited', slug);
  }

  // ── View: Editor ─────────────────────────────────────────────────

  function renderEditor(slug) {
    const page = WikiEngine.getPage(slug);
    if (!page) {
      navigate('#/new');
      return;
    }

    renderEditorForm(page.title, page.content, (page.tags || []).join(', '), slug);
  }

  function renderNewPage(prefillTitle) {
    renderEditorForm(prefillTitle || '', '', '', null);
  }

  function renderEditorForm(title, content, tags, editSlug) {
    const isNew = !editSlug;

    let html = `
      <div class="editor-header">
        <input type="text" class="editor-title-input" id="editor-title"
               placeholder="Page title..." value="${WikiEngine.escapeHtml(title)}" />
        <input type="text" class="editor-tags-input" id="editor-tags"
               placeholder="Tags (comma-separated)..." value="${WikiEngine.escapeHtml(tags)}" />
      </div>

      <div class="editor-tabs">
        <button class="editor-tab active" data-action="editor-tab" data-tab="write">Write</button>
        <button class="editor-tab" data-action="editor-tab" data-tab="preview">Preview</button>
      </div>

      <div class="editor-pane active" id="pane-write">
        <textarea class="editor-textarea" id="editor-content"
                  placeholder="Start writing... Use **bold**, *italic*, [[Wiki Links]], and more.">${WikiEngine.escapeHtml(content)}</textarea>
      </div>

      <div class="editor-pane" id="pane-preview">
        <div class="editor-preview page-body" id="editor-preview-content"></div>
      </div>

      <div class="editor-footer">
        <div class="editor-footer-left">
          <input type="text" class="editor-summary" id="editor-summary"
                 placeholder="Edit summary (optional)..." />
        </div>
        <div class="editor-footer-right">
          <button class="btn btn-secondary" data-action="editor-cancel">Cancel</button>
          <button class="btn btn-primary" data-action="editor-save" data-slug="${editSlug || ''}">${isNew ? 'Create Page' : 'Save Changes'}</button>
        </div>
      </div>`;

    $('#view').innerHTML = html;

    // Set up keyboard shortcuts and dirty state tracking
    const textarea = $('#editor-content');
    let isDirty = false;

    textarea.addEventListener('input', () => { isDirty = true; });
    $('#editor-title').addEventListener('input', () => { isDirty = true; });

    textarea.addEventListener('keydown', (e) => {
      // Tab inserts spaces
      if (e.key === 'Tab') {
        e.preventDefault();
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        textarea.value = textarea.value.substring(0, start) + '  ' + textarea.value.substring(end);
        textarea.selectionStart = textarea.selectionEnd = start + 2;
        isDirty = true;
        return;
      }

      // Ctrl/Cmd shortcuts
      if (e.ctrlKey || e.metaKey) {
        if (e.key === 'b') {
          e.preventDefault();
          wrapSelection(textarea, '**', '**');
        } else if (e.key === 'i') {
          e.preventDefault();
          wrapSelection(textarea, '*', '*');
        } else if (e.key === 'k') {
          e.preventDefault();
          wrapSelection(textarea, '[[', ']]');
        } else if (e.key === 's') {
          e.preventDefault();
          handleSave(editSlug);
        }
      }
    });

    // Warn before leaving with unsaved changes
    window._editorDirty = () => isDirty;
    textarea.focus();
  }

  function wrapSelection(textarea, before, after) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = textarea.value.substring(start, end);
    const replacement = before + (selected || 'text') + after;
    textarea.value = textarea.value.substring(0, start) + replacement + textarea.value.substring(end);
    textarea.selectionStart = start + before.length;
    textarea.selectionEnd = start + before.length + (selected ? selected.length : 4);
    textarea.focus();
  }

  function handleSave(editSlug) {
    const title = $('#editor-title').value.trim();
    const content = $('#editor-content').value;
    const tags = $('#editor-tags').value.split(',').map(t => t.trim()).filter(Boolean);
    const summary = $('#editor-summary') ? $('#editor-summary').value.trim() : '';

    if (!title) {
      $('#editor-title').focus();
      $('#editor-title').style.borderBottom = '2px solid var(--color-danger)';
      setTimeout(() => { $('#editor-title').style.borderBottom = ''; }, 2000);
      return;
    }

    const slug = editSlug || WikiEngine.slugify(title);

    // Check for slug collision on new pages
    if (!editSlug && WikiEngine.getPage(slug)) {
      showModal('Page Exists',
        `A page named "${title}" already exists. Would you like to edit it instead?`,
        [
          { label: 'Cancel', cls: 'btn-secondary', action: 'close-modal' },
          { label: 'Edit Existing', cls: 'btn-primary', action: 'navigate', href: `#/edit/${slug}` }
        ]);
      return;
    }

    WikiEngine.savePage(slug, title, content, tags, summary);
    window._editorDirty = null;
    navigate(`#/page/${slug}`);
  }

  // ── View: Search ─────────────────────────────────────────────────

  function renderSearch(query) {
    let html = `
      <div class="search-header">
        <input type="text" class="search-input" id="search-input"
               placeholder="Search pages..." value="${WikiEngine.escapeHtml(query)}" autofocus />
        <div class="search-count" id="search-count"></div>
      </div>
      <div class="search-results" id="search-results"></div>`;

    $('#view').innerHTML = html;

    const input = $('#search-input');
    let debounceTimer;

    function doSearch() {
      const q = input.value.trim();
      const results = q ? WikiEngine.search(q) : [];
      const countEl = $('#search-count');
      const resultsEl = $('#search-results');

      if (!q) {
        countEl.textContent = '';
        resultsEl.innerHTML = `
          <div class="empty-state">
            ${icons.search}
            <h3>Search your wiki</h3>
            <p>Find pages by title or content.</p>
          </div>`;
        return;
      }

      countEl.textContent = `${results.length} result${results.length !== 1 ? 's' : ''} found`;

      if (results.length === 0) {
        resultsEl.innerHTML = `
          <div class="empty-state">
            ${icons.search}
            <h3>No results</h3>
            <p>No pages match "${WikiEngine.escapeHtml(q)}".</p>
            <button class="btn btn-primary" data-action="navigate"
                    data-href="#/new?title=${encodeURIComponent(q)}">Create "${WikiEngine.escapeHtml(q)}"</button>
          </div>`;
        return;
      }

      resultsEl.innerHTML = results.map(r => {
        const tagsHtml = (r.tags || []).map(t =>
          `<a class="tag" data-action="navigate" data-href="#/tag/${encodeURIComponent(t)}">${WikiEngine.escapeHtml(t)}</a>`
        ).join('');
        return `
          <div class="card card-clickable" data-action="navigate" data-href="#/page/${r.slug}">
            <div class="card-title">${WikiEngine.escapeHtml(r.title)}</div>
            <div class="card-meta">${timeAgo(r.updatedAt)}</div>
            ${r.snippet ? `<div class="card-snippet">${WikiEngine.escapeHtml(r.snippet)}</div>` : ''}
            ${tagsHtml ? `<div class="page-tags" style="margin-top:8px">${tagsHtml}</div>` : ''}
          </div>`;
      }).join('');
    }

    input.addEventListener('input', () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(doSearch, 200);
    });

    // Initial search if query provided
    if (query) doSearch();
    input.focus();
  }

  // ── View: Tags ───────────────────────────────────────────────────

  function renderTags() {
    const tags = WikiEngine.getAllTags();

    let html = `<h1 class="page-title" style="margin-bottom:24px">Tags</h1>`;

    if (tags.length === 0) {
      html += `
        <div class="empty-state">
          ${icons.tag}
          <h3>No tags yet</h3>
          <p>Add tags when creating or editing pages to organize your wiki.</p>
        </div>`;
    } else {
      html += `<div class="tag-cloud">`;
      for (const tag of tags) {
        const size = Math.min(18, 13 + Math.floor(tag.count / 2));
        html += `<a class="tag" style="font-size:${size}px" data-action="navigate"
                    data-href="#/tag/${encodeURIComponent(tag.name)}">
                    ${WikiEngine.escapeHtml(tag.name)}
                    <span class="tag-count">${tag.count}</span></a>`;
      }
      html += `</div>`;
    }

    $('#view').innerHTML = html;
  }

  function renderTagResults(tagName) {
    const pages = WikiEngine.getPagesByTag(tagName);

    let html = `
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
        <button class="btn btn-ghost btn-sm" data-action="navigate" data-href="#/tags">${icons.back}</button>
        <h1 class="page-title" style="margin:0">Tag: ${WikiEngine.escapeHtml(tagName)}</h1>
      </div>`;

    if (pages.length === 0) {
      html += `<p style="color:var(--color-text-secondary)">No pages with this tag.</p>`;
    } else {
      for (const page of pages) {
        html += `
          <div class="card card-clickable" data-action="navigate" data-href="#/page/${page.slug}">
            <div class="card-title">${WikiEngine.escapeHtml(page.title)}</div>
            <div class="card-meta">${timeAgo(page.updatedAt)}</div>
          </div>`;
      }
    }

    $('#view').innerHTML = html;
  }

  // ── View: History ────────────────────────────────────────────────

  function renderHistory(slug) {
    const page = WikiEngine.getPage(slug);
    if (!page) {
      navigate('#/');
      return;
    }

    const history = WikiEngine.getHistory(slug);

    let html = `
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
        <button class="btn btn-ghost btn-sm" data-action="navigate" data-href="#/page/${slug}">${icons.back}</button>
        <h1 class="page-title" style="margin:0">History: ${WikiEngine.escapeHtml(page.title)}</h1>
      </div>

      <div class="card" style="margin-bottom:16px">
        <div class="history-item" style="border:none;padding:8px 0">
          <div class="history-info">
            <div class="history-date">${formatDate(page.updatedAt)}</div>
            <div class="history-summary">Current version</div>
          </div>
          <button class="btn btn-secondary btn-sm" data-action="navigate" data-href="#/page/${slug}">View</button>
        </div>
      </div>`;

    if (history.length === 0) {
      html += `<p style="color:var(--color-text-secondary)">No previous versions.</p>`;
    } else {
      html += `<div class="card"><div class="history-list">`;
      history.forEach((v, i) => {
        html += `
          <div class="history-item">
            <div class="history-info">
              <div class="history-date">${formatDate(v.timestamp)}</div>
              <div class="history-summary">${WikiEngine.escapeHtml(v.summary || 'Edit')}</div>
            </div>
            <div style="display:flex;gap:8px">
              <button class="btn btn-ghost btn-sm" data-action="navigate" data-href="#/version/${slug}/${i}">View</button>
              <button class="btn btn-secondary btn-sm" data-action="restore-version" data-slug="${slug}" data-index="${i}">
                ${icons.restore} Restore
              </button>
            </div>
          </div>`;
      });
      html += `</div></div>`;
    }

    $('#view').innerHTML = html;
  }

  function renderVersion(slug, index) {
    const page = WikiEngine.getPage(slug);
    if (!page) { navigate('#/'); return; }

    const history = WikiEngine.getHistory(slug);
    const version = history[index];
    if (!version) { navigate(`#/history/${slug}`); return; }

    let html = `
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:8px">
        <button class="btn btn-ghost btn-sm" data-action="navigate" data-href="#/history/${slug}">${icons.back}</button>
        <h1 class="page-title" style="margin:0">${WikiEngine.escapeHtml(page.title)}</h1>
      </div>
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
        <span class="card-meta">${formatDate(version.timestamp)} &mdash; ${WikiEngine.escapeHtml(version.summary || 'Edit')}</span>
        <button class="btn btn-secondary btn-sm" data-action="restore-version" data-slug="${slug}" data-index="${index}">
          ${icons.restore} Restore this version
        </button>
      </div>
      <div class="card">
        <div class="page-body">${WikiEngine.parse(version.content)}</div>
      </div>`;

    $('#view').innerHTML = html;
  }

  // ── Sidebar ──────────────────────────────────────────────────────

  function updateSidebar() {
    const route = getRoute();
    const currentSlug = route.segments[1] || '';

    // Page tree
    const pages = WikiEngine.getAllPages();
    const pageTree = $('#page-tree');
    if (pageTree) {
      pageTree.innerHTML = pages.map(p => `
        <a class="sidebar-item ${p.slug === currentSlug ? 'active' : ''}"
           data-action="navigate" data-href="#/page/${p.slug}">
          ${icons.page}
          <span>${WikiEngine.escapeHtml(p.title)}</span>
        </a>
      `).join('');

      if (pages.length === 0) {
        pageTree.innerHTML = '<div style="padding:4px 8px;font-size:13px;color:var(--color-text-tertiary)">No pages yet</div>';
      }
    }

    // Tags
    const tags = WikiEngine.getAllTags();
    const tagList = $('#tag-list');
    if (tagList) {
      if (tags.length > 0) {
        tagList.innerHTML = tags.slice(0, 10).map(t => `
          <a class="sidebar-item" data-action="navigate" data-href="#/tag/${encodeURIComponent(t.name)}">
            ${icons.tag}
            <span>${WikiEngine.escapeHtml(t.name)}</span>
            <span class="tag-count" style="margin-left:auto">${t.count}</span>
          </a>
        `).join('');
        if (tags.length > 10) {
          tagList.innerHTML += `<a class="sidebar-item" data-action="navigate" data-href="#/tags" style="color:var(--color-accent)">View all tags...</a>`;
        }
      } else {
        tagList.innerHTML = '<div style="padding:4px 8px;font-size:13px;color:var(--color-text-tertiary)">No tags yet</div>';
      }
    }

    // Recent
    const recent = WikiEngine.getRecentChanges(5);
    const recentList = $('#recent-list');
    if (recentList) {
      recentList.innerHTML = recent.map(r => `
        <a class="sidebar-item" data-action="navigate" data-href="#/page/${r.slug}">
          ${icons.clock}
          <span>${WikiEngine.escapeHtml(r.title)}</span>
        </a>
      `).join('');

      if (recent.length === 0) {
        recentList.innerHTML = '<div style="padding:4px 8px;font-size:13px;color:var(--color-text-tertiary)">No recent edits</div>';
      }
    }
  }

  function closeSidebar() {
    $('#sidebar').classList.remove('open');
    document.querySelector('.sidebar-backdrop').classList.remove('visible');
  }

  // ── Modal ────────────────────────────────────────────────────────

  function showModal(title, body, actions) {
    const modal = $('#modal');
    modal.innerHTML = `
      <div class="modal-title">${title}</div>
      <div class="modal-body">${body}</div>
      <div class="modal-actions">
        ${actions.map(a =>
          `<button class="btn ${a.cls}" data-action="${a.action}"
                   ${a.href ? `data-href="${a.href}"` : ''}
                   ${a.slug ? `data-slug="${a.slug}"` : ''}
                   ${a.index !== undefined ? `data-index="${a.index}"` : ''}>${a.label}</button>`
        ).join('')}
      </div>`;
    $('#modal-overlay').classList.remove('hidden');
  }

  function closeModal() {
    $('#modal-overlay').classList.add('hidden');
  }

  // ── Event Delegation ─────────────────────────────────────────────

  function init() {
    WikiEngine.seedIfEmpty();

    // Global click handler
    document.body.addEventListener('click', (e) => {
      const target = e.target.closest('[data-action]');
      if (!target) return;

      const action = target.dataset.action;

      switch (action) {
        case 'navigate':
          e.preventDefault();
          if (window._editorDirty && window._editorDirty()) {
            if (!confirm('You have unsaved changes. Leave anyway?')) return;
          }
          window._editorDirty = null;
          const href = target.dataset.href;
          if (href) navigate(href);
          closeModal();
          break;

        case 'new-page':
          e.preventDefault();
          navigate('#/new');
          break;

        case 'toggle-sidebar':
          e.preventDefault();
          $('#sidebar').classList.toggle('open');
          document.querySelector('.sidebar-backdrop').classList.toggle('visible');
          break;

        case 'close-sidebar':
          e.preventDefault();
          closeSidebar();
          break;

        case 'delete-page': {
          e.preventDefault();
          const slug = target.dataset.slug;
          const page = WikiEngine.getPage(slug);
          if (!page) return;
          showModal('Delete Page',
            `Are you sure you want to delete "<strong>${WikiEngine.escapeHtml(page.title)}</strong>"? This action cannot be undone.`,
            [
              { label: 'Cancel', cls: 'btn-secondary', action: 'close-modal' },
              { label: 'Delete', cls: 'btn-danger', action: 'confirm-delete', slug }
            ]);
          break;
        }

        case 'confirm-delete': {
          e.preventDefault();
          const slug = target.dataset.slug;
          WikiEngine.deletePage(slug);
          closeModal();
          navigate('#/');
          break;
        }

        case 'editor-tab': {
          e.preventDefault();
          const tab = target.dataset.tab;
          $$('.editor-tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tab));
          $$('.editor-pane').forEach(p => p.classList.toggle('active', p.id === `pane-${tab}`));
          if (tab === 'preview') {
            const content = $('#editor-content').value;
            $('#editor-preview-content').innerHTML = WikiEngine.parse(content);
          }
          break;
        }

        case 'editor-save': {
          e.preventDefault();
          const slug = target.dataset.slug || null;
          handleSave(slug || null);
          break;
        }

        case 'editor-cancel':
          e.preventDefault();
          if (window._editorDirty && window._editorDirty()) {
            if (!confirm('Discard unsaved changes?')) return;
          }
          window._editorDirty = null;
          window.history.back();
          break;

        case 'restore-version': {
          e.preventDefault();
          const slug = target.dataset.slug;
          const index = parseInt(target.dataset.index);
          showModal('Restore Version',
            'This will create a new version with the content from this historical version. Continue?',
            [
              { label: 'Cancel', cls: 'btn-secondary', action: 'close-modal' },
              { label: 'Restore', cls: 'btn-primary', action: 'confirm-restore', slug, index }
            ]);
          break;
        }

        case 'confirm-restore': {
          e.preventDefault();
          const slug = target.dataset.slug;
          const index = parseInt(target.dataset.index);
          WikiEngine.restoreVersion(slug, index);
          closeModal();
          navigate(`#/page/${slug}`);
          break;
        }

        case 'close-modal':
          e.preventDefault();
          closeModal();
          break;
      }
    });

    // Close modal on backdrop click (but not on modal content click)
    $('#modal-overlay').addEventListener('click', (e) => {
      if (e.target === e.currentTarget) {
        closeModal();
      }
    });

    // Sidebar search
    const sidebarSearch = $('#sidebar-search');
    if (sidebarSearch) {
      sidebarSearch.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          const q = sidebarSearch.value.trim();
          if (q) {
            navigate(`#/search?q=${encodeURIComponent(q)}`);
            sidebarSearch.value = '';
          }
        }
      });

      // Quick filter for page tree
      sidebarSearch.addEventListener('input', () => {
        const q = sidebarSearch.value.toLowerCase().trim();
        $$('#page-tree .sidebar-item').forEach(item => {
          const text = item.textContent.toLowerCase();
          item.style.display = !q || text.includes(q) ? '' : 'none';
        });
      });
    }

    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
      // Cmd/Ctrl+/ to focus search
      if ((e.ctrlKey || e.metaKey) && e.key === '/') {
        e.preventDefault();
        const search = $('#sidebar-search');
        if (search) search.focus();
      }
    });

    // Hash change listener
    window.addEventListener('hashchange', renderView);

    // Before unload warning
    window.addEventListener('beforeunload', (e) => {
      if (window._editorDirty && window._editorDirty()) {
        e.preventDefault();
        e.returnValue = '';
      }
    });

    // Initial render
    renderView();
  }

  // ── Public API ───────────────────────────────────────────────────

  return { init, navigate };
})();

// Boot
document.addEventListener('DOMContentLoaded', App.init);
