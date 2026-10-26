/**
 * Academic Examination Timetabling Application Logic (Web Edition)
 * Optimization Engine with Conflict Matrix Generation & Multi-File Upload Support
 */
document.addEventListener('DOMContentLoaded', async () => {
    // Initialize i18n
    await I18n.init();
    I18n.applyTranslations();

    // ── DOM Elements ──────────────────────────────────────────────────────────
    const langSelect              = document.getElementById('lang-select');
    const btnInitialize           = document.getElementById('btn-initialize');
    const btnInitialGenerate      = document.getElementById('btn-initial-generate');
    const btnImproveGenerate      = document.getElementById('btn-improve-generate');
    const btnXit                  = document.getElementById('btn-xit');
    const btnYir                  = document.getElementById('btn-yir');
    const btnZrt                  = document.getElementById('btn-zrt');
    const btnCalculate            = document.getElementById('btn-calculate');
    const outputCanvas            = document.getElementById('output-canvas');
    const searchInput             = document.getElementById('table-search');

    // Conflict matrix
    const btnGenerateConflict     = document.getElementById('btn-generate-conflict');
    const btnExportConflict       = document.getElementById('btn-export-conflict');
    const statNumCourses          = document.getElementById('stat-num-courses');
    const statNumStudents         = document.getElementById('stat-num-students');
    const statConflictEdges       = document.getElementById('stat-conflict-edges');
    const statConflictDensity     = document.getElementById('stat-conflict-density');
    const statMaxClash            = document.getElementById('stat-max-clash');
    const conflictMatrixTable     = document.getElementById('conflict-matrix-table');

    // Inline Upload Panel
    const uploadInlineCard        = document.getElementById('upload-inline-card');
    const uploadPanelBody         = document.getElementById('upload-panel-body');
    const btnToggleUploadPanel    = document.getElementById('btn-toggle-upload-panel');
    const uploadPanelArrow        = document.getElementById('upload-panel-arrow');
    const btnScrollToUpload       = document.getElementById('btn-scroll-to-upload');
    const btnToggleSampleGuide    = document.getElementById('btn-toggle-sample-guide');
    const sampleGuideBox          = document.getElementById('sample-guide-box');
    const btnLoadSampleAll        = document.getElementById('btn-load-sample-all');
    const btnClearUploaded        = document.getElementById('btn-clear-uploaded');
    const btnApplyUploadedDataset = document.getElementById('btn-apply-uploaded-dataset');
    const bulkDropzone            = document.getElementById('bulk-dropzone');
    const bulkFilesInput          = document.getElementById('bulk-files-input');
    const uploadStatusSummary     = document.getElementById('upload-status-summary');

    // Modals
    const btnSaveSetting          = document.getElementById('btn-save-setting');
    const btnCloseMatrix          = document.getElementById('btn-close-matrix');
    const btnExportMatrix         = document.getElementById('btn-export-matrix');

    // Inline Settings Panel
    const btnToggleSettingsPanel  = document.getElementById('btn-toggle-settings-panel');
    const settingsPanelBody       = document.getElementById('settings-panel-body');
    const settingsPanelArrow      = document.getElementById('settings-panel-arrow');

    // Metrics
    const metricFeasible          = document.getElementById('metric-feasible');
    const metricHard              = document.getElementById('metric-hard');
    const metricSoft              = document.getElementById('metric-soft');
    const metricTime              = document.getElementById('metric-time');

    // ── 7-File In-Memory Storage ──────────────────────────────────────────────
    const fileTypes = ['campus', 'coincidence', 'course', 'distance', 'roominfo', 'student', 'timeslot'];
    const uploadedFiles = { campus: '', coincidence: '', course: '', distance: '', roominfo: '', student: '', timeslot: '' };

    // ── Sample Data ───────────────────────────────────────────────────────────
    const BENCHMARK_SAMPLES = {
        campus:      `subject\tcampus\nG0001\tG\nG0002\tG\nG0003\tG\nG0004\tG\nG0005\tG\nG0006\tG\nG0007\tG\nG0008\tG\nG0009\tG\nG0161\tG\nP0001\tP\nP0002\tP\nP0003\tP\nP0004\tP\nP0005\tP\nP0006\tP\nP0007\tP\n`,
        coincidence: `coincidence. same subject but different faculty\nG0002\tP0002\nG0008\tP0004\nG0036\tP0003\nG0061\tP0005\nG0082\tP0070\nG0083\tP0066\nG0085\tP0080\nG0116\tP0013\nG0155\tP0074\n`,
        course:      `subject\ttotal of student\nG0001\t710\nG0002\t689\nG0003\t2\nG0004\t3\nG0005\t1\nG0006\t1\nG0007\t1\nG0008\t585\nG0009\t240\nG0161\t120\nP0001\t320\nP0002\t210\n`,
        distance:    `room1\troom2\tdistance matrix\nDBP\tDBP\t0\nASTAKA\tASTAKA\t0\nVDK01\tVDK01\t0\nVBK02\tVDK01\t3\nVBK03\tVDK01\t4\nVBK04\tVDK01\t1\n`,
        roominfo:    `block\tcampus\tcapacity of room\troom name\nD\tG\t1019\tDBP\nA\tG\t355\tASTAKA\nV\tG\t75\tVDK01\nZ\tG\t40\tZDK02\nP\tP\t600\tDSG\n`,
        student:     `student id\tsubject\nPB15124\tG0004\nPB15124\tG0006\nPB15124\tG0007\nPB16022\tG0004\nPB16022\tG0009\nPB16052\tG0161\nPB16052\tG0011\nPB16052\tG0010\nPB16052\tG0009\nPB17001\tG0001\nPB17001\tG0002\nPB17002\tG0001\nPB17002\tG0008\nPB17003\tP0001\nPB17003\tP0002\n`,
        timeslot:    `subject\ttimetaken for exam\nG0001\t90\nG0002\t90\nG0003\t180\nG0004\t180\nG0005\t180\nG0006\t180\nG0007\t180\nG0008\t180\n`
    };

    // ── Init: Reset server to default dataset on every page load ─────────────
    // This ensures a browser refresh always starts fresh with the built-in
    // benchmark dataset, discarding any previously uploaded custom dataset.
    try {
        await fetch('/api/dataset/select', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ datasetId: 'UMP-20182019-1-GAMBANG' })
        });
    } catch (e) {
        console.warn('Could not reset to default dataset:', e.message);
    }

    // ── Init: load conflict matrix & grid ─────────────────────────────────────
    await loadConflictMatrix();
    await loadCurrentGrid();


    // ── Language ──────────────────────────────────────────────────────────────
    if (langSelect) {
        langSelect.addEventListener('change', async (e) => {
            await I18n.setLanguage(e.target.value);
        });
    }

    // ── Upload Panel: Toggle collapse ─────────────────────────────────────────
    if (btnToggleUploadPanel && uploadPanelBody && uploadPanelArrow) {
        btnToggleUploadPanel.addEventListener('click', () => {
            const hidden = uploadPanelBody.style.display === 'none';
            uploadPanelBody.style.display = hidden ? 'block' : 'none';
            uploadPanelArrow.innerHTML = hidden ? '&#9650; Hide Panel' : '&#9660; Show Panel';
        });
    }

    // ── Upload Panel: Scroll-to from Conflict Matrix card ─────────────────────
    if (btnScrollToUpload && uploadInlineCard) {
        btnScrollToUpload.addEventListener('click', () => {
            if (uploadPanelBody && uploadPanelBody.style.display === 'none') {
                uploadPanelBody.style.display = 'block';
                if (uploadPanelArrow) uploadPanelArrow.innerHTML = '&#9650; Hide Panel';
            }
            uploadInlineCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    }

    // ── Upload Panel: View Schema Guidelines ──────────────────────────────────
    if (btnToggleSampleGuide && sampleGuideBox) {
        btnToggleSampleGuide.addEventListener('click', () => {
            const hidden = sampleGuideBox.style.display === 'none' || !sampleGuideBox.style.display;
            sampleGuideBox.style.display = hidden ? 'block' : 'none';
        });
    }

    // ── Upload Panel: Individual file choosers ────────────────────────────────
    fileTypes.forEach(type => {
        const input = document.getElementById(`file-input-${type}`);
        if (input) {
            input.addEventListener('change', (e) => {
                const file = e.target.files[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = (evt) => setFileLoaded(type, evt.target.result, file.name);
                reader.readAsText(file);
            });
        }
    });

    // ── Upload Panel: Drag & Drop zone ────────────────────────────────────────
    if (bulkDropzone && bulkFilesInput) {
        bulkDropzone.addEventListener('click', () => bulkFilesInput.click());

        bulkDropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            bulkDropzone.style.borderColor = 'var(--primary)';
        });
        bulkDropzone.addEventListener('dragleave', () => {
            bulkDropzone.style.borderColor = '#cbd5e1';
        });
        bulkDropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            bulkDropzone.style.borderColor = '#cbd5e1';
            if (e.dataTransfer.files) handleMultipleFiles(e.dataTransfer.files);
        });

        bulkFilesInput.addEventListener('change', (e) => {
            if (e.target.files) handleMultipleFiles(e.target.files);
        });
    }

    function handleMultipleFiles(files) {
        Array.from(files).forEach(file => {
            const name = file.name.toLowerCase();
            let type = null;
            if      (name.includes('campus'))      type = 'campus';
            else if (name.includes('coincidence')) type = 'coincidence';
            else if (name.includes('course'))      type = 'course';
            else if (name.includes('distance'))    type = 'distance';
            else if (name.includes('room'))        type = 'roominfo';
            else if (name.includes('student'))     type = 'student';
            else if (name.includes('timeslot'))    type = 'timeslot';
            if (type) {
                const reader = new FileReader();
                reader.onload = (evt) => setFileLoaded(type, evt.target.result, file.name);
                reader.readAsText(file);
            }
        });
    }

    function setFileLoaded(type, content, filename) {
        uploadedFiles[type] = content;
        const chk    = document.getElementById(`chk-${type}`);
        const status = document.getElementById(`status-${type}`);
        const row    = document.getElementById(`row-file-${type}`);
        if (chk)    chk.checked = true;
        if (row)    row.classList.add('uploaded-row');
        if (status) {
            const lines = content.trim().split('\n').length;
            status.textContent = `✓ ${filename} (${lines} lines)`;
            status.className = 'file-status-text ok';
        }
        updateChecklistSummary();
    }

    function updateChecklistSummary() {
        const count = fileTypes.filter(t => uploadedFiles[t] && uploadedFiles[t].trim().length > 0).length;
        const hasCourse  = uploadedFiles.course  && uploadedFiles.course.trim().length > 0;
        const hasStudent = uploadedFiles.student && uploadedFiles.student.trim().length > 0;
        const essentialReady = hasCourse && hasStudent;
        if (uploadStatusSummary) {
            if (essentialReady) {
                uploadStatusSummary.textContent = `${count} of 7 files loaded — Course & Student files present. Ready to apply!`;
                uploadStatusSummary.style.color = 'var(--success)';
            } else {
                const missing = [];
                if (!hasCourse)  missing.push('Course');
                if (!hasStudent) missing.push('Student');
                uploadStatusSummary.textContent = `${count} of 7 files loaded. Missing required: ${missing.join(' & ')} file(s).`;
                uploadStatusSummary.style.color = '#334155';
            }
        }
    }

    // ── Upload Panel: Load Sample Dataset ────────────────────────────────────
    if (btnLoadSampleAll) {
        btnLoadSampleAll.addEventListener('click', () => {
            fileTypes.forEach(t => setFileLoaded(t, BENCHMARK_SAMPLES[t], `sample_${t}.txt`));
            appendLog('[UPLOAD] Sample benchmark dataset loaded into all 7 file slots.', 'system');
        });
    }

    // ── Upload Panel: Clear All ───────────────────────────────────────────────
    if (btnClearUploaded) {
        btnClearUploaded.addEventListener('click', () => {
            fileTypes.forEach(t => {
                uploadedFiles[t] = '';
                const chk    = document.getElementById(`chk-${t}`);
                const status = document.getElementById(`status-${t}`);
                const row    = document.getElementById(`row-file-${t}`);
                const input  = document.getElementById(`file-input-${t}`);
                if (chk)    chk.checked = false;
                if (row)    row.classList.remove('uploaded-row');
                if (status) { status.textContent = 'Not loaded'; status.className = 'file-status-text'; }
                if (input)  input.value = '';
            });
            updateChecklistSummary();
        });
    }

    // ── Upload Panel: Apply & Generate Conflict Matrix ────────────────────────
    if (btnApplyUploadedDataset) {
        btnApplyUploadedDataset.addEventListener('click', async () => {
            if (!uploadedFiles.course || !uploadedFiles.student) {
                alert('Please load at least the Course file and the Student file before applying.');
                return;
            }
            setBusy(true);
            appendLog('\n>>> [DATASET] Uploading and compiling dataset on server...', 'system');
            try {
                const payload = {
                    name: 'Custom Institution Dataset',
                    semester: 'Semester 1',
                    session: '2024/2025',
                    campus: 'Multi-Campus',
                    totalPeriods: 30,
                    campusFile:        uploadedFiles.campus,
                    coincidenceFile:   uploadedFiles.coincidence,
                    courseFile:        uploadedFiles.course,
                    distanceMatrixFile: uploadedFiles.distance,
                    roomInfoFile:      uploadedFiles.roominfo,
                    studentFile:       uploadedFiles.student,
                    timeslotFile:      uploadedFiles.timeslot
                };
                const resp = await fetch('/api/dataset/upload', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const res = await resp.json();
                if (res.status === 'success') {
                    appendLog('[DATASET] Institution dataset loaded successfully!', 'system');
                    appendLog(`  ├─ Total Courses : ${res.dataset.numExams}`, 'system');
                    appendLog(`  ├─ Total Rooms   : ${res.dataset.numRooms}`, 'system');
                    appendLog(`  ├─ Total Students: ${res.dataset.totalStudents}`, 'system');
                    appendLog(`  └─ Conflict Edges: ${res.dataset.conflictEdges} (Density: ${res.dataset.conflictDensity})`, 'system');
                    resetMetrics();
                    await loadConflictMatrix();
                    await loadCurrentGrid();
                } else {
                    appendLog(`[ERROR] Dataset upload failed: ${res.error}`, 'system');
                }
            } catch (err) {
                appendLog(`[ERROR] Upload request failed: ${err.message}`, 'system');
            } finally {
                setBusy(false);
            }
        });
    }

    // ── Conflict Matrix: Generate & Export ────────────────────────────────────
    if (btnGenerateConflict) {
        btnGenerateConflict.addEventListener('click', async () => {
            setBusy(true);
            appendLog('\n>>> [CONFLICT MATRIX] Computing symmetric clash matrix [Course.length][Course.length]...', 'system');
            try {
                await loadConflictMatrix();
                appendLog('[CONFLICT MATRIX] Matrix generated successfully.', 'system');
            } catch (e) {
                appendLog(`[ERROR] ${e.message}`, 'system');
            } finally {
                setBusy(false);
            }
        });
    }

    if (btnExportConflict) {
        btnExportConflict.addEventListener('click', () => {
            window.location.href = '/api/conflict-matrix/export';
        });
    }

    // ── Initialize ────────────────────────────────────────────────────────────
    if (btnInitialize) {
        btnInitialize.addEventListener('click', async () => {
            setBusy(true);
            try {
                const resp = await fetch('/api/solver/initialize', { method: 'POST' });
                const data = await resp.json();
                clearLogs();
                appendLog('======================================================================', 'system');
                appendLog('[SYSTEM INITIALIZATION] Timetable Workspace Initialized', 'system');
                appendLog(`  Dataset : ${data.dataset.name}`, 'system');
                appendLog(`  Courses : ${data.dataset.numExams}  |  Venues : ${data.dataset.numRooms}`, 'system');
                appendLog(`  Periods : ${data.dataset.totalPeriods}  |  Students : ${data.dataset.totalStudents}`, 'system');
                appendLog(`  Conflict Density: ${data.dataset.conflictDensity} (${data.dataset.conflictEdges} edges)`, 'system');
                appendLog('======================================================================\n', 'system');
                resetMetrics();
                await loadConflictMatrix();
                await loadCurrentGrid();
            } catch (err) {
                appendLog(`[ERROR] Initialization failed: ${err.message}`, 'system');
            } finally {
                setBusy(false);
            }
        });
    }

    // ── Initial Phase ─────────────────────────────────────────────────────────
    if (btnInitialGenerate) {
        btnInitialGenerate.addEventListener('click', async () => {
            const selected = document.querySelector('input[name="initial_algo"]:checked');
            if (!selected) return;
            const algo = selected.value;
            setBusy(true);
            appendLog(`\n>>> [INITIAL PHASE] Running ${algo} constructive heuristic...`, 'initial');
            try {
                const resp = await fetch('/api/solver/initial', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ algorithm: algo })
                });
                const data = await resp.json();
                appendLog(`[INITIAL] ${algo} complete.`, 'initial');
                appendLog(`  Feasibility     : ${data.feasible ? '✓ FEASIBLE' : '✗ INFEASIBLE'}`, 'initial');
                appendLog(`  Hard Violations : ${data.hardViolations}`, 'initial');
                if (data.hardViolations > 0) {
                    appendLog(`    ├─ H1 Clash: ${data.h1 ?? 0} | H2 Unassigned: ${data.h2 ?? 0} | H3 RoomCap: ${data.h3 ?? 0} | H4 DblBooking: ${data.h4 ?? 0}`, 'initial');
                    appendLog(`    └─ H5 Campus: ${data.h5 ?? 0} | H6 SplitSync: ${data.h6 ?? 0} | H7 LargeEarly: ${data.h7 ?? 0}`, 'initial');
                }
                appendLog(`  Soft Penalty (Composite) : ${data.softPenalty?.toFixed(4)}`, 'initial');
                appendLog(`    ├─ S1 Spreading (Carter)    : ${data.s1 != null ? (typeof data.s1 === 'number' ? data.s1.toFixed(4) : data.s1) : data.softPenalty?.toFixed(4)}`, 'initial');
                appendLog(`    ├─ S2 Building Distance     : ${data.s2 ?? 0}`, 'initial');
                appendLog(`    ├─ S3 Room Splitting        : ${data.s3 ?? 0}`, 'initial');
                appendLog(`    └─ S4 Uniform Duration      : ${data.s4 ?? 0}`, 'initial');
                appendLog(`  Compute Time    : ${data.timeMs} ms`, 'initial');
                updateMetrics(data.feasible, data.hardViolations, data.softPenalty, data.timeMs);
                await loadCurrentGrid();
            } catch (err) {
                appendLog(`[ERROR] Initial phase failed: ${err.message}`, 'initial');
            } finally {
                setBusy(false);
            }
        });
    }

    // ── Improvement Phase ─────────────────────────────────────────────────────
    if (btnImproveGenerate) {
        btnImproveGenerate.addEventListener('click', async () => {
            const selected = document.querySelector('input[name="improve_algo"]:checked');
            if (!selected) return;
            const algo = selected.value;
            const maxIter = document.getElementById('setting-max-iter')?.value || 10000;
            const lahcL   = document.getElementById('setting-lahc-lfa')?.value || 500;
            const schcL   = document.getElementById('setting-schc-ls')?.value  || 500;
            setBusy(true);
            appendLog(`\n>>> [IMPROVEMENT] Running ${algo} metaheuristic (${maxIter} iterations)...`, 'improve');
            try {
                const resp = await fetch('/api/solver/improve', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ algorithm: algo, maxIterations: parseInt(maxIter), lahcLfa: parseInt(lahcL), schcLs: parseInt(schcL) })
                });
                const data = await resp.json();
                appendLog(`[IMPROVEMENT] ${algo} complete.`, 'improve');
                appendLog(`  Feasibility     : ${data.feasible ? '✓ FEASIBLE' : '✗ INFEASIBLE'}`, 'improve');
                appendLog(`  Hard Violations : ${data.hardViolations}`, 'improve');
                appendLog(`  Soft Penalty    : ${data.improvedCost?.toFixed(4)}`, 'improve');
                appendLog(`    ├─ S1 Spreading (Carter)    : ${data.s1 != null ? (typeof data.s1 === 'number' ? data.s1.toFixed(4) : data.s1) : data.improvedCost?.toFixed(4)}`, 'improve');
                appendLog(`    ├─ S2 Building Distance     : ${data.s2 ?? 0}`, 'improve');
                appendLog(`    ├─ S3 Room Splitting        : ${data.s3 ?? 0}`, 'improve');
                appendLog(`    └─ S4 Uniform Duration      : ${data.s4 ?? 0}`, 'improve');
                appendLog(`  Compute Time    : ${data.timeMs} ms`, 'improve');
                updateMetrics(data.feasible, data.hardViolations, data.improvedCost, data.timeMs);
                await loadCurrentGrid();
            } catch (err) {
                appendLog(`[ERROR] Improvement phase failed: ${err.message}`, 'improve');
            } finally {
                setBusy(false);
            }
        });
    }

    // ── Timetable Matrix Views ────────────────────────────────────────────────
    if (btnXit) btnXit.addEventListener('click', () => openMatrixView('/api/timetable/xit', 'Xit Matrix'));
    if (btnYir) btnYir.addEventListener('click', () => openMatrixView('/api/timetable/yir', 'Yir Matrix'));
    if (btnZrt) btnZrt.addEventListener('click', () => openMatrixView('/api/timetable/zrt', 'Zrt Matrix'));

    async function openMatrixView(endpoint, label) {
        setBusy(true);
        try {
            const resp = await fetch(endpoint);
            const data = await resp.json();
            if (!data.columns || data.columns.length === 0) {
                alert('No timetable solution yet. Please run Initial Phase first.');
                return;
            }
            TimetableGrid.renderMatrixModal(data.title, data.description, data.columns, data.rows);
        } catch (e) {
            alert(`Error fetching ${label}: ` + e.message);
        } finally {
            setBusy(false);
        }
    }

    // ── Calculate ─────────────────────────────────────────────────────────────
    if (btnCalculate) {
        btnCalculate.addEventListener('click', async () => {
            setBusy(true);
            appendLog('\n>>> [EVALUATE] Calculating full constraint evaluation...', 'calc');
            try {
                const resp = await fetch('/api/solver/calculate', { method: 'POST' });
                const data = await resp.json();
                
                if (data.status === 'no_solution') {
                    appendLog(`[EVALUATE] ${data.message}`, 'calc');
                    setBusy(false);
                    return;
                }

                const ev = data.evaluation;
                appendLog(`[EVALUATE] Feasible: ${ev.feasible}`, 'calc');
                appendLog(`  Hard Violations : ${ev.hardViolations}`, 'calc');
                appendLog(`  Soft Penalty (Composite) : ${(ev.softPenalty ?? ev.compositePenaltyPerStudent ?? ev.penaltyPerStudent)?.toFixed(4)}`, 'calc');
                appendLog(`    ├─ S1 Spreading (Carter)    : ${ev.s1 != null ? (typeof ev.s1 === 'number' ? ev.s1.toFixed(4) : ev.s1) : ev.penaltyPerStudent?.toFixed(4)}`, 'calc');
                appendLog(`    ├─ S2 Building Distance     : ${ev.s2 ?? 0}`, 'calc');
                appendLog(`    ├─ S3 Room Splitting        : ${ev.s3 ?? 0}`, 'calc');
                appendLog(`    └─ S4 Uniform Duration      : ${ev.s4 ?? 0}`, 'calc');
                appendLog(`  Student Clashes : ${ev.studentClashes}`, 'calc');
                updateMetrics(ev.feasible, ev.hardViolations, (ev.softPenalty ?? ev.compositePenaltyPerStudent ?? ev.penaltyPerStudent), 0);
                await loadCurrentGrid();
            } catch (err) {
                appendLog(`[ERROR] Evaluation failed: ${err.message}`, 'calc');
            } finally {
                setBusy(false);
            }
        });
    }

    // ── Inline Settings Panel (Toggle) ────────────────────────────────────────
    if (btnToggleSettingsPanel && settingsPanelBody && settingsPanelArrow) {
        btnToggleSettingsPanel.addEventListener('click', () => {
            const hidden = settingsPanelBody.style.display === 'none';
            settingsPanelBody.style.display = hidden ? 'block' : 'none';
            settingsPanelArrow.innerHTML = hidden ? '&#9650; Hide Panel' : '&#9660; Show Panel';
        });
    }

    // ── Settings: Save Button ─────────────────────────────────────────────────
    if (btnSaveSetting) {
        btnSaveSetting.addEventListener('click', async () => {
            const enabledConstraints = [];
            for (let i = 0; i < 7; i++) {
                const chk = document.getElementById(`chk-h${i+1}`);
                enabledConstraints.push(chk ? chk.checked : true);
            }
            const enabledSoftConstraints = [];
            for (let i = 0; i < 4; i++) {
                const chk = document.getElementById(`chk-s${i+1}`);
                enabledSoftConstraints.push(chk ? chk.checked : true);
            }
            const payload = {
                maxIterations: parseInt(document.getElementById('setting-max-iter').value),
                lahcListLength: parseInt(document.getElementById('setting-lahc-lfa').value),
                schcStepLength: parseInt(document.getElementById('setting-schc-ls').value),
                enabledConstraints: enabledConstraints,
                enabledSoftConstraints: enabledSoftConstraints
            };
            try {
                const resp = await fetch('/api/settings', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const result = await resp.json();
                const activeHard = result.enabledConstraints ? result.enabledConstraints.filter(Boolean).length : 7;
                const activeSoft = result.enabledSoftConstraints ? result.enabledSoftConstraints.filter(Boolean).length : 4;
                appendLog(`[SETTINGS] Saved. Active Hard Constraints: ${activeHard}/7. Active Soft Constraints: ${activeSoft}/4. Max iterations: ${result.maxIterations}.`, 'system');
            } catch (e) {
                console.error('Failed to save settings', e);
                appendLog('[SETTINGS] Error: Failed to save settings.', 'system');
            }
        });
    }

    // ── Matrix Modal Close & Export ───────────────────────────────────────────
    if (btnCloseMatrix) {
        btnCloseMatrix.addEventListener('click', () => {
            document.getElementById('matrix-modal').style.display = 'none';
        });
    }
    if (btnExportMatrix) {
        btnExportMatrix.addEventListener('click', () => {
            window.location.href = '/api/timetable/export-matrix';
        });
    }

    // ── Search Filter ─────────────────────────────────────────────────────────
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            TimetableGrid.setFilter(e.target.value);
        });
    }

    // ── Helper: Load Conflict Matrix from Server ──────────────────────────────
    async function loadConflictMatrix() {
        try {
            const resp = await fetch('/api/conflict-matrix');
            const data = await resp.json();
            if (statNumCourses)    statNumCourses.textContent    = data.numCourses   ?? '-';
            if (statConflictEdges) statConflictEdges.textContent = data.totalConflictEdges ?? '-';
            if (statConflictDensity) statConflictDensity.textContent = data.conflictDensity ?? '-';
            if (statMaxClash)      statMaxClash.textContent      = data.maxClash != null ? (data.maxClash + ' students') : '-';

            try {
                const sumResp = await fetch('/api/dataset/current');
                const sum = await sumResp.json();
                if (statNumStudents) statNumStudents.textContent = sum.totalStudents ?? '-';
            } catch (_) { /* ignore */ }

            renderConflictMatrixGrid(data.courseCodes, data.matrix);
        } catch (e) {
            console.warn('Conflict matrix not available yet:', e.message);
        }
    }

    function renderConflictMatrixGrid(courseCodes, matrix) {
        if (!conflictMatrixTable) return;
        conflictMatrixTable.innerHTML = '';
        if (!courseCodes || !matrix || courseCodes.length === 0) return;

        const thead = document.createElement('thead');
        const headerTr = document.createElement('tr');
        const cornerTh = document.createElement('th');
        cornerTh.textContent = 'Course';
        headerTr.appendChild(cornerTh);
        courseCodes.forEach(code => {
            const th = document.createElement('th');
            th.textContent = code;
            headerTr.appendChild(th);
        });
        thead.appendChild(headerTr);
        conflictMatrixTable.appendChild(thead);

        const tbody = document.createElement('tbody');
        const frag  = document.createDocumentFragment();
        matrix.forEach((row, ri) => {
            const tr = document.createElement('tr');
            const hdr = document.createElement('td');
            hdr.className = 'matrix-header-col';
            hdr.textContent = courseCodes[ri];
            tr.appendChild(hdr);
            row.forEach((clash, ci) => {
                const td = document.createElement('td');
                td.textContent = clash;
                if (ri === ci) {
                    td.style.background = '#e2e8f0';
                    td.style.color = '#94a3b8';
                } else if (clash > 0) {
                    td.className = 'clash-active';
                } else {
                    td.className = 'clash-zero';
                }
                tr.appendChild(td);
            });
            frag.appendChild(tr);
        });
        tbody.appendChild(frag);
        conflictMatrixTable.appendChild(tbody);
    }

    // ── Helper: Load Schedule Grid from Server ────────────────────────────────
    async function loadCurrentGrid() {
        try {
            const resp = await fetch('/api/timetable/grid');
            const data = await resp.json();
            TimetableGrid.renderGrid(data.grid);
            const label = document.getElementById('current-dataset-label');
            if (label && data.datasetName) label.textContent = data.datasetName;
        } catch (e) {
            console.warn('Timetable grid not available yet:', e.message);
        }
    }

    // ── Utility Functions ─────────────────────────────────────────────────────
    function appendLog(text, type = 'system') {
        if (!outputCanvas) return;
        const div = document.createElement('div');
        div.className = `log-entry ${type}`;
        div.textContent = text;
        outputCanvas.appendChild(div);
        outputCanvas.scrollTop = outputCanvas.scrollHeight;
    }

    function clearLogs() {
        if (outputCanvas) outputCanvas.innerHTML = '';
    }

    function updateMetrics(feasible, hard, soft, timeMs) {
        if (metricFeasible) {
            metricFeasible.textContent = feasible ? 'FEASIBLE' : 'INFEASIBLE';
            metricFeasible.className = 'kpi-val ' + (feasible ? 'feasible' : 'infeasible');
        }
        if (metricHard) metricHard.textContent = hard;
        if (metricSoft) metricSoft.textContent = typeof soft === 'number' ? soft.toFixed(4) : soft;
        if (metricTime && timeMs > 0) metricTime.textContent = timeMs + ' ms';
    }

    function resetMetrics() {
        if (metricFeasible) { metricFeasible.textContent = '-'; metricFeasible.className = 'kpi-val'; }
        if (metricHard)  metricHard.textContent  = '-';
        if (metricSoft)  metricSoft.textContent  = '-';
        if (metricTime)  metricTime.textContent  = '-';
    }

    function setBusy(isBusy) {
        const btns = [btnInitialize, btnInitialGenerate, btnImproveGenerate, btnCalculate, btnGenerateConflict];
        btns.forEach(b => { if (b) b.disabled = isBusy; });
        document.body.style.cursor = isBusy ? 'wait' : 'default';
    }
});
