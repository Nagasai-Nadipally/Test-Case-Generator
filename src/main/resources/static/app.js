const API_BASE = '/api';

// ---------- Tabs ----------
document.querySelectorAll('.tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    tab.classList.add('active');
    document.querySelector(`.panel[data-panel="${tab.dataset.tab}"]`).classList.add('active');
  });
});

// ---------- Health check ----------
async function checkHealth() {
  const dot = document.getElementById('statusDot');
  const text = document.getElementById('statusText');
  try {
    const res = await fetch(`${API_BASE}/health`);
    const data = await res.json();
    if (data.aiConfigured) {
      dot.classList.add('up');
      text.textContent = 'engine ready';
    } else {
      dot.classList.add('down');
      text.textContent = 'GROQ_API_KEY not set';
    }
  } catch (e) {
    dot.classList.add('down');
    text.textContent = 'backend unreachable';
  }
}
checkHealth();

// ---------- Endpoint map ----------
const ENDPOINTS = {
  requirements: { url: '/generate/requirements', input: 'req-input', output: 'req-output', filename: 'req-filename' },
  openapi:      { url: '/generate/openapi',      input: 'api-input', output: 'api-output', filename: 'api-filename' },
  code:         { url: '/generate/code',         input: 'code-input', output: 'code-output', filename: 'code-filename' },
  security:     { url: '/generate/security',     input: 'sec-input', output: 'sec-output', filename: 'sec-filename' },
};

document.querySelectorAll('.run-btn[data-action]').forEach(btn => {
  btn.addEventListener('click', async () => {
    const action = btn.dataset.action;

    if (action === 'chat') {
      await runChat(btn);
      return;
    }

    const cfg = ENDPOINTS[action];
    const inputEl = document.getElementById(cfg.input);
    const outputEl = document.getElementById(cfg.output);
    const filenameEl = document.getElementById(cfg.filename);

    const text = inputEl.value.trim();
    if (!text) {
      inputEl.focus();
      return;
    }

    setLoading(btn, outputEl, true);
    try {
      const res = await fetch(`${API_BASE}${cfg.url}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ input: text })
      });
      const data = await res.json();
      if (data.success) {
        outputEl.textContent = data.output;
        outputEl.classList.remove('error');
        if (data.suggestedFileName) filenameEl.textContent = data.suggestedFileName;
      } else {
        showError(outputEl, data.error);
      }
    } catch (e) {
      showError(outputEl, 'Network error: ' + e.message);
    } finally {
      setLoading(btn, outputEl, false);
    }
  });
});

async function runChat(btn) {
  const context = document.getElementById('chat-context').value.trim();
  const question = document.getElementById('chat-question').value.trim();
  const outputEl = document.getElementById('chat-output');

  if (!question) {
    document.getElementById('chat-question').focus();
    return;
  }

  setLoading(btn, outputEl, true);
  try {
    const res = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ input: context, question })
    });
    const data = await res.json();
    if (data.success) {
      outputEl.textContent = data.output;
      outputEl.classList.remove('error');
    } else {
      showError(outputEl, data.error);
    }
  } catch (e) {
    showError(outputEl, 'Network error: ' + e.message);
  } finally {
    setLoading(btn, outputEl, false);
  }
}

function setLoading(btn, outputEl, isLoading) {
  btn.disabled = isLoading;
  if (isLoading) {
    btn.dataset.originalText = btn.textContent;
    btn.textContent = 'Generating…';
    outputEl.classList.remove('error');
    outputEl.classList.add('loading');
    outputEl.textContent = 'Calling AI engine, this can take a few seconds…';
  } else {
    btn.textContent = btn.dataset.originalText || btn.textContent;
    outputEl.classList.remove('loading');
  }
}

function showError(outputEl, message) {
  outputEl.textContent = 'Error: ' + message;
  outputEl.classList.add('error');
}

// ---------- Copy / Download ----------
document.querySelectorAll('[data-copy]').forEach(btn => {
  btn.addEventListener('click', () => {
    const el = document.getElementById(btn.dataset.copy);
    navigator.clipboard.writeText(el.textContent).then(() => {
      const original = btn.textContent;
      btn.textContent = 'Copied!';
      setTimeout(() => (btn.textContent = original), 1200);
    });
  });
});

document.querySelectorAll('[data-download]').forEach(btn => {
  btn.addEventListener('click', () => {
    const el = document.getElementById(btn.dataset.download);
    const filenameEl = document.getElementById(btn.dataset.filenameTarget);
    const filename = filenameEl ? filenameEl.textContent : 'GeneratedTest.java';
    const blob = new Blob([el.textContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  });
});
