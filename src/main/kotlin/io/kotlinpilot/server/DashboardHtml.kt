package io.kotlinpilot.server

object DashboardHtml {
    fun render(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KotlinPilot | Autonomous Kotlin & Android Engineering Agent</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-base: #0a0d14;
            --bg-surface: #111726;
            --bg-card: rgba(23, 32, 54, 0.7);
            --border: rgba(255, 255, 255, 0.08);
            --border-highlight: rgba(124, 58, 237, 0.4);
            --primary: #7c3aed;
            --primary-glow: rgba(124, 58, 237, 0.25);
            --primary-light: #a78bfa;
            --accent-cyan: #06b6d4;
            --accent-green: #10b981;
            --accent-amber: #f59e0b;
            --accent-rose: #f43f5e;
            --text-main: #f3f4f6;
            --text-muted: #9ca3af;
            --text-dim: #6b7280;
            --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            --font-mono: 'JetBrains Mono', monospace;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            background-color: var(--bg-base);
            background-image: 
                radial-gradient(at 0% 0%, rgba(124, 58, 237, 0.12) 0px, transparent 50%),
                radial-gradient(at 100% 100%, rgba(6, 182, 212, 0.1) 0px, transparent 50%);
            color: var(--text-main);
            font-family: var(--font-sans);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        header {
            background: rgba(17, 23, 38, 0.85);
            backdrop-filter: blur(16px);
            border-bottom: 1px solid var(--border);
            padding: 1rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .logo-area {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }

        .logo-badge {
            background: linear-gradient(135deg, #7c3aed, #06b6d4);
            color: white;
            font-weight: 800;
            font-size: 1.1rem;
            padding: 0.4rem 0.7rem;
            border-radius: 8px;
            box-shadow: 0 0 15px var(--primary-glow);
        }

        .logo-text {
            font-size: 1.25rem;
            font-weight: 700;
            letter-spacing: -0.02em;
        }

        .logo-sub {
            font-size: 0.8rem;
            color: var(--text-muted);
            font-weight: 400;
        }

        .nav-status {
            display: flex;
            align-items: center;
            gap: 1.5rem;
        }

        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            background: rgba(16, 185, 129, 0.1);
            color: var(--accent-green);
            border: 1px solid rgba(16, 185, 129, 0.25);
            padding: 0.35rem 0.85rem;
            border-radius: 9999px;
            font-size: 0.8rem;
            font-weight: 600;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--accent-green);
            box-shadow: 0 0 8px var(--accent-green);
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.5; transform: scale(0.85); }
        }

        main {
            flex: 1;
            max-width: 1400px;
            width: 100%;
            margin: 0 auto;
            padding: 2rem;
            display: grid;
            grid-template-columns: 380px 1fr;
            gap: 1.5rem;
        }

        @media (max-width: 1024px) {
            main {
                grid-template-columns: 1fr;
            }
        }

        .card {
            background: var(--bg-card);
            backdrop-filter: blur(12px);
            border: 1px solid var(--border);
            border-radius: 14px;
            padding: 1.5rem;
            transition: all 0.2s ease;
        }

        .card:hover {
            border-color: rgba(255, 255, 255, 0.15);
        }

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1.25rem;
            padding-bottom: 0.75rem;
            border-bottom: 1px solid var(--border);
        }

        .card-title {
            font-size: 1rem;
            font-weight: 600;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .info-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 0.75rem;
            margin-bottom: 1.25rem;
        }

        .info-item {
            background: rgba(0, 0, 0, 0.25);
            border: 1px solid var(--border);
            padding: 0.75rem;
            border-radius: 8px;
        }

        .info-label {
            font-size: 0.7rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--text-dim);
            margin-bottom: 0.25rem;
        }

        .info-value {
            font-size: 0.9rem;
            font-weight: 600;
            color: var(--text-main);
            word-break: break-all;
        }

        .badge-list {
            display: flex;
            flex-wrap: wrap;
            gap: 0.5rem;
            margin-top: 0.5rem;
        }

        .badge {
            background: rgba(124, 58, 237, 0.15);
            color: var(--primary-light);
            border: 1px solid rgba(124, 58, 237, 0.3);
            padding: 0.25rem 0.6rem;
            border-radius: 6px;
            font-size: 0.75rem;
            font-weight: 500;
        }

        .badge.cyan {
            background: rgba(6, 182, 212, 0.15);
            color: var(--accent-cyan);
            border-color: rgba(6, 182, 212, 0.3);
        }

        .badge.green {
            background: rgba(16, 185, 129, 0.15);
            color: var(--accent-green);
            border-color: rgba(16, 185, 129, 0.3);
        }

        .task-input-box {
            display: flex;
            flex-direction: column;
            gap: 0.85rem;
            margin-bottom: 1.5rem;
        }

        textarea {
            width: 100%;
            min-height: 90px;
            background: rgba(0, 0, 0, 0.35);
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 0.85rem 1rem;
            color: var(--text-main);
            font-family: var(--font-sans);
            font-size: 0.95rem;
            resize: vertical;
            outline: none;
            transition: border-color 0.2s;
        }

        textarea:focus {
            border-color: var(--primary-light);
            box-shadow: 0 0 10px var(--primary-glow);
        }

        .task-controls {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 1rem;
        }

        .mode-select {
            background: rgba(0, 0, 0, 0.35);
            border: 1px solid var(--border);
            color: var(--text-main);
            padding: 0.5rem 0.75rem;
            border-radius: 8px;
            font-size: 0.85rem;
            outline: none;
        }

        .btn-primary {
            background: linear-gradient(135deg, #7c3aed, #6366f1);
            color: white;
            border: none;
            padding: 0.6rem 1.4rem;
            border-radius: 8px;
            font-weight: 600;
            font-size: 0.9rem;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            box-shadow: 0 4px 15px var(--primary-glow);
            transition: all 0.2s ease;
        }

        .btn-primary:hover {
            opacity: 0.95;
            transform: translateY(-1px);
        }

        .btn-primary:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            transform: none;
        }

        .console-container {
            background: #07090e;
            border: 1px solid var(--border);
            border-radius: 10px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            height: 480px;
        }

        .console-topbar {
            background: #0d121c;
            border-bottom: 1px solid var(--border);
            padding: 0.5rem 1rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 0.75rem;
            color: var(--text-dim);
            font-family: var(--font-mono);
        }

        .console-logs {
            flex: 1;
            overflow-y: auto;
            padding: 1rem;
            font-family: var(--font-mono);
            font-size: 0.85rem;
            line-height: 1.6;
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
        }

        .log-entry {
            display: flex;
            align-items: flex-start;
            gap: 0.75rem;
            padding: 0.25rem 0;
            animation: fadeIn 0.2s ease-in;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(4px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .log-time {
            color: var(--text-dim);
            font-size: 0.75rem;
            min-width: 65px;
        }

        .log-pill {
            font-size: 0.7rem;
            font-weight: 700;
            padding: 0.15rem 0.45rem;
            border-radius: 4px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .log-pill.TASK_CREATED { background: #374151; color: #d1d5db; }
        .log-pill.ANALYSIS_STARTED, .log-pill.ANALYSIS_COMPLETED { background: rgba(6, 182, 212, 0.2); color: #22d3ee; }
        .log-pill.PLAN_CREATED { background: rgba(124, 58, 237, 0.2); color: #c084fc; }
        .log-pill.TOOL_STARTED, .log-pill.TOOL_COMPLETED { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
        .log-pill.REVIEW_STARTED, .log-pill.REVIEW_COMPLETED { background: rgba(168, 85, 247, 0.2); color: #d8b4fe; }
        .log-pill.TASK_COMPLETED { background: rgba(16, 185, 129, 0.25); color: #34d399; }
        .log-pill.TASK_FAILED, .log-pill.ERROR_DETECTED { background: rgba(244, 63, 94, 0.25); color: #fb7185; }

        .log-msg {
            color: #e5e7eb;
            word-break: break-word;
            flex: 1;
        }

        .tab-bar {
            display: flex;
            gap: 0.5rem;
            border-bottom: 1px solid var(--border);
            margin-bottom: 1rem;
            padding-bottom: 0.5rem;
        }

        .tab-btn {
            background: transparent;
            border: none;
            color: var(--text-muted);
            font-weight: 600;
            font-size: 0.85rem;
            padding: 0.4rem 0.85rem;
            border-radius: 6px;
            cursor: pointer;
            transition: all 0.2s;
        }

        .tab-btn.active {
            background: rgba(124, 58, 237, 0.2);
            color: var(--primary-light);
        }

        .diff-view {
            background: #07090e;
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 1rem;
            font-family: var(--font-mono);
            font-size: 0.8rem;
            white-space: pre-wrap;
            max-height: 400px;
            overflow-y: auto;
            color: #9ca3af;
        }
    </style>
</head>
<body>
    <header>
        <div class="logo-area">
            <div class="logo-badge">KP</div>
            <div>
                <div class="logo-text">KotlinPilot</div>
                <div class="logo-sub">Autonomous Kotlin & Android Engineering Agent</div>
            </div>
        </div>
        <div class="nav-status">
            <div class="status-pill" id="serverStatus">
                <div class="status-dot"></div>
                <span>Server Online (v0.1.0)</span>
            </div>
        </div>
    </header>

    <main>
        <!-- Left Sidebar: Project Architecture & Status -->
        <div style="display: flex; flex-direction: column; gap: 1.5rem;">
            <div class="card">
                <div class="card-header">
                    <span class="card-title">🏛️ Repository Profile</span>
                    <button class="tab-btn" onclick="fetchProjectInfo()">↻ Refresh</button>
                </div>
                <div class="info-grid">
                    <div class="info-item">
                        <div class="info-label">Project Name</div>
                        <div class="info-value" id="projName">KotlinPilot</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">Kotlin Version</div>
                        <div class="info-value" id="kotlinVer">2.2.20</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">Gradle</div>
                        <div class="info-value" id="gradleVer">8.14</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">Compose / KMP</div>
                        <div class="info-value" id="composeKmp">-</div>
                    </div>
                </div>

                <div class="info-label">Architecture Patterns</div>
                <div class="badge-list" id="archBadges">
                    <span class="badge">Coroutines & Flow</span>
                    <span class="badge cyan">Ktor Engine</span>
                    <span class="badge green">JUnit 5</span>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <span class="card-title">🌿 Git Status</span>
                    <button class="tab-btn" onclick="fetchGitStatus()">↻ Refresh</button>
                </div>
                <div class="info-item" style="margin-bottom: 0.75rem;">
                    <div class="info-label">Current Branch</div>
                    <div class="info-value" id="gitBranch">master</div>
                </div>
                <div class="info-label">Working Tree</div>
                <pre class="diff-view" id="gitStatusText" style="max-height: 140px;">Checking status...</pre>
            </div>
        </div>

        <!-- Right Main Panel: Task Execution & Live Console -->
        <div style="display: flex; flex-direction: column; gap: 1.5rem;">
            <div class="card">
                <div class="card-header">
                    <span class="card-title">⚡ Autonomous Engineering Task</span>
                </div>
                <div class="task-input-box">
                    <textarea id="taskPrompt" placeholder="Describe the engineering task (e.g. 'Add Ktor route for health check', 'Fix compilation error in models', 'Run tests and review diff')..."></textarea>
                    <div class="task-controls">
                        <div style="display: flex; align-items: center; gap: 0.5rem;">
                            <label for="taskMode" style="font-size: 0.85rem; color: var(--text-muted);">Mode:</label>
                            <select id="taskMode" class="mode-select">
                                <option value="AUTONOMOUS">Autonomous (Full Execution)</option>
                                <option value="ASSISTED" selected>Assisted</option>
                                <option value="EXPLAIN">Explain (Dry Run)</option>
                            </select>
                        </div>
                        <button class="btn-primary" id="btnSubmitTask" onclick="submitTask()">
                            <span>🚀 Execute Agent</span>
                        </button>
                    </div>
                </div>

                <div class="tab-bar">
                    <button class="tab-btn active" id="tabConsole" onclick="showTab('console')">Live Execution Console</button>
                    <button class="tab-btn" id="tabDiff" onclick="showTab('diff')">Working Tree Diff</button>
                    <button class="tab-btn" id="tabReview" onclick="showTab('review')">Automated Review</button>
                </div>

                <!-- Live Streaming Console -->
                <div class="console-container" id="consoleView">
                    <div class="console-topbar">
                        <span id="activeTaskId">Task: Idle</span>
                        <span id="iterationStatus">Ready</span>
                    </div>
                    <div class="console-logs" id="logList">
                        <div class="log-entry">
                            <span class="log-time">--:--:--</span>
                            <span class="log-pill TASK_CREATED">READY</span>
                            <span class="log-msg">KotlinPilot agent initialized and awaiting instructions.</span>
                        </div>
                    </div>
                </div>

                <!-- Diff View -->
                <div id="diffView" style="display: none;">
                    <pre class="diff-view" id="diffContent" style="height: 480px;">No changes yet.</pre>
                </div>

                <!-- Automated Review View -->
                <div id="reviewView" style="display: none;">
                    <div class="card" style="background: rgba(0,0,0,0.3); border: none; padding: 1rem;">
                        <h4 style="margin-bottom: 0.5rem;" id="reviewTitle">No recent reviews</h4>
                        <p style="font-size: 0.9rem; color: var(--text-muted);" id="reviewSummary">Submit and complete a task to inspect automated architectural checks and findings.</p>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <script>
        let currentTaskId = null;
        let eventSource = null;

        function showTab(tab) {
            document.getElementById('tabConsole').classList.toggle('active', tab === 'console');
            document.getElementById('tabDiff').classList.toggle('active', tab === 'diff');
            document.getElementById('tabReview').classList.toggle('active', tab === 'review');

            document.getElementById('consoleView').style.display = tab === 'console' ? 'flex' : 'none';
            document.getElementById('diffView').style.display = tab === 'diff' ? 'block' : 'none';
            document.getElementById('reviewView').style.display = tab === 'review' ? 'block' : 'none';

            if (tab === 'diff') fetchGitDiff();
        }

        async function fetchProjectInfo() {
            try {
                const res = await fetch('/api/project');
                if (!res.ok) return;
                const data = await res.json();
                document.getElementById('projName').textContent = data.projectName || 'KotlinPilot';
                document.getElementById('kotlinVer').textContent = data.kotlinVersion || 'Detected';
                document.getElementById('gradleVer').textContent = data.gradleVersion || 'Detected';
                document.getElementById('composeKmp').textContent = 
                    (data.composeEnabled ? 'Compose ✓ ' : '') + (data.kmpEnabled ? 'KMP ✓' : (!data.composeEnabled && !data.kmpEnabled ? 'JVM' : ''));

                if (data.architectureHints && data.architectureHints.length > 0) {
                    const container = document.getElementById('archBadges');
                    container.innerHTML = '';
                    data.architectureHints.forEach(h => {
                        const span = document.createElement('span');
                        span.className = 'badge';
                        span.textContent = h.pattern;
                        container.appendChild(span);
                    });
                }
            } catch (e) {
                console.error('Failed to fetch project info:', e);
            }
        }

        async function fetchGitStatus() {
            try {
                const res = await fetch('/api/status');
                if (!res.ok) return;
                const data = await res.json();
                document.getElementById('gitBranch').textContent = data.branch || 'unknown';
                document.getElementById('gitStatusText').textContent = data.status || 'Clean';
            } catch (e) {
                console.error('Failed to fetch git status:', e);
            }
        }

        async function fetchGitDiff() {
            try {
                const res = await fetch('/api/diff');
                if (!res.ok) return;
                const data = await res.json();
                document.getElementById('diffContent').textContent = data.diff || 'No uncommitted changes in workspace.';
            } catch (e) {
                console.error('Failed to fetch diff:', e);
            }
        }

        async function submitTask() {
            const prompt = document.getElementById('taskPrompt').value.trim();
            if (!prompt) return;

            const mode = document.getElementById('taskMode').value;
            const btn = document.getElementById('btnSubmitTask');
            btn.disabled = true;

            try {
                const res = await fetch('/api/tasks', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ description: prompt, mode: mode })
                });

                if (!res.ok) {
                    alert('Failed to submit task: ' + (await res.text()));
                    btn.disabled = false;
                    return;
                }

                const task = await res.json();
                currentTaskId = task.id.value || task.id;
                document.getElementById('activeTaskId').textContent = 'Task: ' + currentTaskId;
                document.getElementById('iterationStatus').textContent = 'Running...';
                
                showTab('console');
                appendLog('TASK_CREATED', 'Task submitted: ' + prompt);
                listenToEvents(currentTaskId);
            } catch (e) {
                alert('Error: ' + e.message);
                btn.disabled = false;
            }
        }

        function listenToEvents(taskId) {
            if (eventSource) eventSource.close();

            eventSource = new EventSource('/api/tasks/' + taskId + '/events');

            eventSource.onmessage = function(e) {
                try {
                    const event = JSON.parse(e.data);
                    appendLog(event.type, event.message);

                    if (event.type === 'TASK_COMPLETED' || event.type === 'TASK_FAILED') {
                        document.getElementById('btnSubmitTask').disabled = false;
                        document.getElementById('iterationStatus').textContent = event.type;
                        eventSource.close();
                        fetchGitStatus();
                    }
                } catch (err) {
                    console.error('Failed to parse SSE event:', err);
                }
            };

            eventSource.onerror = function() {
                console.log('SSE connection ended or closed.');
            };
        }

        function appendLog(type, message) {
            const list = document.getElementById('logList');
            const entry = document.createElement('div');
            entry.className = 'log-entry';

            const now = new Date();
            const timeStr = now.toTimeString().split(' ')[0];

            entry.innerHTML = `
                <span class="log-time">${'$'}{timeStr}</span>
                <span class="log-pill ${'$'}{type}">${'$'}{type.replace(/_/g, ' ')}</span>
                <span class="log-msg">${'$'}{escapeHtml(message)}</span>
            `;

            list.appendChild(entry);
            list.scrollTop = list.scrollHeight;
        }

        function escapeHtml(text) {
            return text
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
        }

        // Initialize on load
        fetchProjectInfo();
        fetchGitStatus();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
