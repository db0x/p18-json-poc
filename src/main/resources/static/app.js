const uuidInput = document.getElementById('uuid');
const loadBtn = document.getElementById('loadBtn');
const saveBtn = document.getElementById('saveBtn');
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
        setStatus('Geladen (' + Math.round(performance.now() - t0) + ' ms).', true);
    } catch (e) {
        setStatus('Netzwerkfehler: ' + e.message, false);
    }
}

async function save() {
    const id = uuidInput.value.trim();
    if (!id) { setStatus('Bitte eine UUID eingeben.', false); return; }
    const t0 = performance.now();
    try {
        const res = await fetch('/api/documents/' + encodeURIComponent(id), {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: editor.getValue()
        });
        if (res.status === 404) { setStatus('Kein Dokument mit dieser UUID gefunden (' + Math.round(performance.now() - t0) + ' ms).', false); return; }
        if (res.status === 400) { setStatus('Ungültiges JSON – vom Server abgelehnt (' + Math.round(performance.now() - t0) + ' ms).', false); return; }
        if (!res.ok) { setStatus('Fehler beim Speichern: HTTP ' + res.status, false); return; }
        const data = await res.json();
        editor.setValue(JSON.stringify(data, null, 2));
        setStatus('Gespeichert (' + Math.round(performance.now() - t0) + ' ms).', true);
    } catch (e) {
        setStatus('Netzwerkfehler: ' + e.message, false);
    }
}

loadBtn.addEventListener('click', load);
saveBtn.addEventListener('click', save);
uuidInput.addEventListener('keydown', e => { if (e.key === 'Enter') load(); });
