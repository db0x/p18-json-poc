const uuidInput = document.getElementById('uuid');
const loadBtn = document.getElementById('loadBtn');
const saveBtn = document.getElementById('saveBtn');
const deleteBtn = document.getElementById('deleteBtn');
const tableBtn = document.getElementById('tableBtn');
const status = document.getElementById('status');

const editor = CodeMirror.fromTextArea(document.getElementById('editor'), {
    mode: { name: 'javascript', json: true },
    theme: 'idea',
    lineNumbers: true,
    tabSize: 2,
    indentUnit: 2,
    matchBrackets: true
});

function setStatus(msg, ok) {
    status.textContent = msg;
    status.className = ok ? 'ok' : 'err';
}

async function load() {
    const id = uuidInput.value.trim();
    if (!id) { setStatus('Bitte eine UUID eingeben.', false); return; }
    const t0 = performance.now();
    try {
        const res = await fetch('/api/documents/' + encodeURIComponent(id));
        if (res.status === 404) { setStatus('Kein Dokument mit dieser UUID gefunden (' + Math.round(performance.now() - t0) + ' ms).', false); return; }
        if (!res.ok) { setStatus('Fehler beim Laden: HTTP ' + res.status, false); return; }
        const data = await res.json();
        editor.setValue(JSON.stringify(data, null, 2));
        saveBtn.disabled = false;
        deleteBtn.disabled = false;
        tableBtn.disabled = false;
        setStatus('Geladen (' + Math.round(performance.now() - t0) + ' ms).', true);
    } catch (e) {
        setStatus('Netzwerkfehler: ' + e.message, false);
    }
}

async function push(t0) {
    const res = await fetch('/api/documents', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: editor.getValue()
    });
    if (res.status === 400) { setStatus('Ungültiges JSON – vom Server abgelehnt (' + Math.round(performance.now() - t0) + ' ms).', false); return; }
    if (!res.ok) { setStatus('Fehler beim Anlegen: HTTP ' + res.status, false); return; }
    const newId = await res.json();
    uuidInput.value = newId;
    await load();
    setStatus('Neu angelegt (' + Math.round(performance.now() - t0) + ' ms).', true);
}

async function save() {
    const id = uuidInput.value.trim();
    const t0 = performance.now();
    try {
        if (!id) { await push(t0); return; }
        const res = await fetch('/api/documents/' + encodeURIComponent(id), {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: editor.getValue()
        });
        if (res.status === 404) { await push(t0); return; }
        if (res.status === 400) { setStatus('Ungültiges JSON – vom Server abgelehnt (' + Math.round(performance.now() - t0) + ' ms).', false); return; }
        if (!res.ok) { setStatus('Fehler beim Speichern: HTTP ' + res.status, false); return; }
        const data = await res.json();
        editor.setValue(JSON.stringify(data, null, 2));
        setStatus('Gespeichert (' + Math.round(performance.now() - t0) + ' ms).', true);
    } catch (e) {
        setStatus('Netzwerkfehler: ' + e.message, false);
    }
}

async function deleteDoc() {
    const id = uuidInput.value.trim();
    if (!id) { setStatus('Kein geladenes Dokument zum Löschen.', false); return; }
    if (!confirm('Dokument ' + id + ' wirklich löschen?')) return;
    const t0 = performance.now();
    try {
        const res = await fetch('/api/documents/' + encodeURIComponent(id), { method: 'DELETE' });
        if (res.status === 404) { setStatus('Kein Dokument mit dieser UUID gefunden (' + Math.round(performance.now() - t0) + ' ms).', false); return; }
        if (!res.ok) { setStatus('Fehler beim Löschen: HTTP ' + res.status, false); return; }
        editor.setValue('');
        saveBtn.disabled = true;
        deleteBtn.disabled = true;
        tableBtn.disabled = true;
        setStatus('Gelöscht (' + Math.round(performance.now() - t0) + ' ms).', true);
    } catch (e) {
        setStatus('Netzwerkfehler: ' + e.message, false);
    }
}

async function showInTable() {
    const id = uuidInput.value.trim();
    if (!id) return;
    try {
        const res = await fetch('/api/documents/' + encodeURIComponent(id) + '/page');
        if (res.status === 404) { setStatus('Dokument nicht gefunden.', false); return; }
        if (!res.ok) { setStatus('Fehler: HTTP ' + res.status, false); return; }
        const d = await res.json();
        location.href = 'table.html?page=' + d.page + '&id=' + encodeURIComponent(id);
    } catch (e) {
        setStatus('Netzwerkfehler: ' + e.message, false);
    }
}

loadBtn.addEventListener('click', load);
saveBtn.addEventListener('click', save);
deleteBtn.addEventListener('click', deleteDoc);
tableBtn.addEventListener('click', showInTable);
uuidInput.addEventListener('keydown', e => { if (e.key === 'Enter') load(); });

// open directly on a document when coming from the table view (index.html?id=...)
const preselectedId = new URLSearchParams(location.search).get('id');
if (preselectedId) {
    uuidInput.value = preselectedId;
    load();
}
