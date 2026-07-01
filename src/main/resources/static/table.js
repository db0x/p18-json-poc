const rowsEl = document.getElementById('rows');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');
const pageInfo = document.getElementById('pageInfo');
const gotoInput = document.getElementById('gotoInput');
const gotoBtn = document.getElementById('gotoBtn');
const filterInput = document.getElementById('filterInput');
const clearFilterBtn = document.getElementById('clearFilterBtn');
const status = document.getElementById('status');

const PAGE_SIZE = 10;
const params = new URLSearchParams(location.search);
let page = Math.max(0, parseInt(params.get('page'), 10) || 0);
const highlightId = params.get('id');
let query = params.get('q') || '';
let totalPages = 0;
let hasNext = false;

function setStatus(msg, ok) {
    status.textContent = msg || '';
    status.className = ok ? 'ok' : 'err';
}

function shortId(id) { return id ? id.slice(0, 8) + '…' : ''; }
function cell(v) { return (v === undefined || v === null) ? '—' : String(v); }

function render(content) {
    rowsEl.innerHTML = '';
    if (content.length === 0) {
        const tr = document.createElement('tr');
        const msg = query ? 'Keine Treffer für den Filter.' : 'Keine Dokumente.';
        tr.innerHTML = `<td colspan="4" class="muted">${msg}</td>`;
        rowsEl.appendChild(tr);
        return;
    }
    for (const row of content) {
        const data = row.data || {};
        const tr = document.createElement('tr');

        const tdToggle = document.createElement('td');
        const btn = document.createElement('button');
        btn.className = 'expand-btn';
        btn.textContent = '▸';
        btn.title = 'Details anzeigen';
        tdToggle.appendChild(btn);

        // id links directly into the single (editor) view for that document
        const tdId = document.createElement('td');
        tdId.className = 'mono';
        const idLink = document.createElement('a');
        idLink.href = 'index.html?id=' + encodeURIComponent(row.id);
        idLink.textContent = shortId(row.id);
        idLink.title = 'In Einzelansicht öffnen: ' + row.id;
        tdId.appendChild(idLink);

        const tdName = document.createElement('td');
        tdName.textContent = cell(data.name);
        const tdCity = document.createElement('td');
        tdCity.textContent = cell(data.city);

        if (row.id === highlightId) {
            tr.classList.add('highlight');
            // bring the row into view once it is in the DOM
            setTimeout(() => tr.scrollIntoView({ block: 'center', behavior: 'smooth' }), 0);
        }

        tr.append(tdToggle, tdId, tdName, tdCity);
        rowsEl.appendChild(tr);

        // detail row with the full document, hidden until expanded
        const detail = document.createElement('tr');
        detail.className = 'detail';
        detail.style.display = 'none';
        const dtd = document.createElement('td');
        dtd.colSpan = 4;
        detail.appendChild(dtd);
        rowsEl.appendChild(detail);

        let cm = null;
        btn.addEventListener('click', () => {
            const open = detail.style.display !== 'none';
            detail.style.display = open ? 'none' : '';
            btn.textContent = open ? '▸' : '▾';
            // create the read-only highlighted view lazily, once it is visible
            if (!open && !cm) {
                cm = CodeMirror(dtd, {
                    value: JSON.stringify(data, null, 2),
                    mode: { name: 'javascript', json: true },
                    theme: 'idea',
                    lineNumbers: true,
                    readOnly: 'nocursor'
                });
            } else if (!open && cm) {
                cm.refresh();
            }
        });
    }
}

async function load() {
    const t0 = performance.now();
    try {
        let url = `/api/documents?page=${page}&size=${PAGE_SIZE}`;
        if (query) { url += `&q=${encodeURIComponent(query)}`; }
        const res = await fetch(url);
        if (!res.ok) { setStatus('Fehler beim Laden: HTTP ' + res.status + ' (' + Math.round(performance.now() - t0) + ' ms).', false); return; }
        const d = await res.json();
        page = d.page;
        totalPages = d.totalPages;
        hasNext = d.hasNext;
        render(d.content);
        pageInfo.textContent = `Seite ${totalPages === 0 ? 0 : page + 1} / ${totalPages} · ${d.totalElements} Dokumente`;
        prevBtn.disabled = page <= 0;
        nextBtn.disabled = !hasNext;
        // keep the jump control in sync with the current page and valid range
        gotoInput.max = totalPages;
        gotoInput.value = totalPages === 0 ? '' : page + 1;
        gotoInput.disabled = totalPages === 0;
        gotoBtn.disabled = totalPages === 0;
        const action = query ? 'Gefiltert' : 'Geladen';
        setStatus(action + ' (' + Math.round(performance.now() - t0) + ' ms).', true);
    } catch (e) {
        setStatus('Netzwerkfehler: ' + e.message, false);
    }
}

prevBtn.addEventListener('click', () => { if (page > 0) { page--; load(); } });
nextBtn.addEventListener('click', () => { if (hasNext) { page++; load(); } });

function gotoPage() {
    const n = parseInt(gotoInput.value, 10);
    if (isNaN(n) || totalPages === 0) { return; }
    const target = Math.min(Math.max(1, n), totalPages) - 1; // clamp to [1..totalPages], to 0-based
    if (target !== page) { page = target; load(); }
    else { gotoInput.value = page + 1; }
}

gotoBtn.addEventListener('click', gotoPage);
gotoInput.addEventListener('keydown', e => { if (e.key === 'Enter') gotoPage(); });

// keep the query in the URL so a filtered view can be shared/reloaded
function syncUrl() {
    const p = new URLSearchParams(location.search);
    if (query) { p.set('q', query); } else { p.delete('q'); }
    p.delete('page'); // a new filter always starts at page 1
    const qs = p.toString();
    history.replaceState(null, '', qs ? `?${qs}` : location.pathname);
}

function applyFilter() {
    const next = filterInput.value.trim();
    if (next === query) { return; }
    query = next;
    page = 0; // reset paging whenever the filter changes
    syncUrl();
    load();
}

let debounce;
filterInput.addEventListener('input', () => {
    clearTimeout(debounce);
    debounce = setTimeout(applyFilter, 250);
});
filterInput.addEventListener('keydown', e => { if (e.key === 'Enter') { clearTimeout(debounce); applyFilter(); } });
clearFilterBtn.addEventListener('click', () => {
    clearTimeout(debounce);
    filterInput.value = '';
    applyFilter();
    filterInput.focus();
});

filterInput.value = query; // reflect an initial ?q= from the URL
load();