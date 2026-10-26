/**
 * Timetable DataGrid and Matrix Display Component
 */
const TimetableGrid = {
    currentGridData: [],
    filterQuery: '',
    activeView: 'grid', // 'grid', 'xit', 'yir', 'zrt'

    renderGrid(data) {
        this.currentGridData = data || [];
        const tbody = document.getElementById('grid-tbody');
        if (!tbody) return;

        tbody.innerHTML = '';
        const filtered = this.currentGridData.filter(item => {
            if (!this.filterQuery) return true;
            const q = this.filterQuery.toLowerCase();
            return (item.code && item.code.toLowerCase().includes(q)) ||
                   (item.name && item.name.toLowerCase().includes(q)) ||
                   (item.campus && item.campus.toLowerCase().includes(q)) ||
                   (item.roomCode && item.roomCode.toLowerCase().includes(q));
        });

        if (filtered.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:#777; padding:12px;">No examinations found.</td></tr>`;
            document.getElementById('row-count-display').textContent = `0 of ${this.currentGridData.length} records`;
            return;
        }

        const fragment = document.createDocumentFragment();
        filtered.forEach((item, index) => {
            const tr = document.createElement('tr');

            const isAssigned = item.periodIndex >= 0;
            const periodText = isAssigned ? `<span style="font-weight:600; color:#1e293b;">Period ${item.period}</span> <small style="color:#64748b;">(${item.day}, ${item.session})</small>` : `<span style="color:#e11d48; font-weight:600;">Unassigned</span>`;
            const roomText = isAssigned ? `<span style="font-weight:600; color:#1e293b;">${item.roomCode}</span> <small style="color:#64748b;">(${item.roomCapacity} seats)</small>` : `<span style="color:#94a3b8;">-</span>`;

            tr.innerHTML = `
                <td style="color:#94a3b8; font-weight:500;">${index + 1}</td>
                <td><strong style="color:#4f46e5;">${item.code}</strong></td>
                <td style="font-weight:500;">${item.name}</td>
                <td style="font-family:var(--font-mono); font-weight:600;">${item.studentCount}</td>
                <td>${periodText}</td>
                <td>${roomText}</td>
                <td><span class="table-badge" style="margin:0;">${item.campus}</span></td>
                <td style="font-family:var(--font-mono); font-weight:600;">${item.degree} <small style="color:#64748b;">(W:${item.weightedDegree})</small></td>
                <td>${isAssigned ? '<span class="status-badge ok">✓ Scheduled</span>' : '<span class="status-badge warn">Pending</span>'}</td>
            `;
            fragment.appendChild(tr);
        });

        tbody.appendChild(fragment);
        document.getElementById('row-count-display').textContent = `${filtered.length} of ${this.currentGridData.length} courses`;
    },

    setFilter(query) {
        this.filterQuery = query;
        this.renderGrid(this.currentGridData);
    },

    renderMatrixModal(title, description, columns, rows) {
        document.getElementById('matrix-modal-title').textContent = title;
        document.getElementById('matrix-modal-desc').textContent = description;

        const table = document.getElementById('matrix-modal-table');
        table.innerHTML = '';

        // Table Header
        const thead = document.createElement('thead');
        const headerTr = document.createElement('tr');
        columns.forEach(col => {
            const th = document.createElement('th');
            th.textContent = col;
            headerTr.appendChild(th);
        });
        thead.appendChild(headerTr);
        table.appendChild(thead);

        // Table Body
        const tbody = document.createElement('tbody');
        rows.forEach(row => {
            const tr = document.createElement('tr');
            row.forEach((cell, cellIdx) => {
                const td = document.createElement('td');
                td.textContent = cell;
                if (typeof cell === 'number' && (cell === 1 || cell === 0) && cellIdx >= 3) {
                    td.className = 'center-cell' + (cell === 1 ? ' active-cell' : '');
                } else if (typeof cell === 'number') {
                    td.className = 'num-cell';
                }
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);

        // Show modal
        document.getElementById('matrix-modal').style.display = 'flex';
    },

    closeMatrixModal() {
        document.getElementById('matrix-modal').style.display = 'none';
    }
};

window.TimetableGrid = TimetableGrid;
