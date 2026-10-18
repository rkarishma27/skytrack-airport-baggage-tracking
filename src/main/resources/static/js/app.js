/* SkyTrack - vanilla JS single-page app talking to the Spring Boot / JDBC REST API */
(() => {
  'use strict';

  // ---------------------------------------------------------------- helpers
  const $ = (s, r = document) => r.querySelector(s);
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const keyOf = s => String(s ?? '').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
  const label = s => String(s ?? '').replace(/_/g, ' ');
  const fmtDT = iso => iso ? new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false }) : '—';
  const toInput = iso => iso ? String(iso).slice(0, 16) : '';
  const store = {
    get(k) { try { return localStorage.getItem(k); } catch { return null; } },
    set(k, v) { try { localStorage.setItem(k, v); } catch { /* storage unavailable */ } }
  };

  async function api(path, opts = {}) {
    const res = await fetch('/api' + path, {
      method: opts.method || 'GET',
      headers: opts.body ? { 'Content-Type': 'application/json' } : undefined,
      body: opts.body ? JSON.stringify(opts.body) : undefined
    });
    if (res.status === 204) return null;
    let data = null;
    try { data = await res.json(); } catch { /* empty body */ }
    if (!res.ok) throw new Error((data && data.error) || `Request failed (${res.status})`);
    return data;
  }

  function toast(msg, isErr = false) {
    const el = document.createElement('div');
    el.className = 'toast' + (isErr ? ' err' : '');
    el.textContent = msg;
    $('#toasts').append(el);
    setTimeout(() => el.remove(), isErr ? 6000 : 3200);
  }

  const badge = text => text ? `<span class="badge" style="--c: var(--st-${keyOf(text)}, var(--muted))">${esc(label(text))}</span>` : '<span class="muted">—</span>';

  const I = {
    grid: '<path d="M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"/>',
    bag: '<rect x="4" y="8" width="16" height="12" rx="2.5"/><path d="M9 8V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V8M9 12v4M15 12v4"/>',
    people: '<circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5M16 5.2a3.2 3.2 0 0 1 0 5.6M18 14.8c1.7.7 3 2.3 3 5.2"/>',
    plane: '<path d="M21 15.5v-1.7l-8-5V4a1.5 1.5 0 0 0-3 0v4.8l-8 5v1.7l8-2.5V18l-2 1.5V21l3.5-1 3.5 1v-1.5L13 18v-5z"/>',
    claim: '<path d="M3 9h18M3 15h18M7 9v6M12 9v6M17 9v6M5 9V6.5h14V9"/><path d="M3 15v2.5h18V15"/>',
    globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.6 2.6 3.9 5.6 3.9 9s-1.3 6.4-3.9 9c-2.6-2.6-3.9-5.6-3.9-9S9.400 5.600 12 3z"/>',
    radar: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4.5"/><path d="M12 12l6-6"/>',
    search: '<circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.2-4.2"/>',
    edit: '<path d="M4 20h4L19 9a2.100 2.100 0 0 0-4-4L4 16zM13.500 6.500l4 4"/>',
    trash: '<path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6"/>',
    flag: '<path d="M5 21V4M5 4h11l-2 4 2 4H5"/>',
    check: '<path d="M5 12.500l4.500 4.500L19 7.500"/>',
    swap: '<path d="M7 7h12l-3-3M17 17H5l3 3"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2.500M12 19.500V22M2 12h2.500M19.500 12H22M4.900 4.900l1.800 1.800M17.300 17.300l1.800 1.800M4.900 19.100l1.800-1.800M17.300 6.700l1.800-1.800"/>',
    moon: '<path d="M20 14.500A8 8 0 0 1 9.500 4a8 8 0 1 0 10.500 10.500z"/>'
  };
  const svg = (name, extra = '') => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" ${extra}>${I[name]}</svg>`;

  // ---------------------------------------------------------------- entity config
  const FLIGHT_STATUSES = ['Scheduled', 'On Time', 'Boarding', 'Departed', 'Delayed', 'Cancelled', 'Landed'];
  let META = { baggageStatus: [], securityStatus: [], claimStatus: [], lostStatus: [], flightType: [] };

  const ENT = {
    baggage: {
      title: 'Baggage', eyebrow: 'Handling', icon: 'bag', endpoint: '/baggage', id: 'baggageId', noun: 'bag', filterKey: 'status',
      filters: () => META.baggageStatus,
      cols: [
        { key: 'baggageId', label: 'ID', render: r => `<span class="mono">#${r.baggageId}</span>` },
        { key: 'rfidTag', label: 'RFID tag', render: r => `<a class="mono" href="#/track/${encodeURIComponent(r.rfidTag)}">${esc(r.rfidTag)}</a>` },
        { key: 'passengerName', label: 'Passenger', render: r => esc(r.passengerName || '—') },
        { key: 'flightNumber', label: 'Flight', render: r => `<span class="mono">${esc(r.flightNumber || '—')}</span>` },
        { key: 'weight', label: 'Weight', render: r => r.weight == null ? '—' : `<span class="mono">${Number(r.weight).toFixed(2)} kg</span>` },
        { key: 'color', label: 'Colour', render: r => esc(r.color || '—') },
        { key: 'securityStatus', label: 'Security', render: r => badge(r.securityStatus) },
        { key: 'baggageStatus', label: 'Status', render: r => badge(r.baggageStatus) }
      ],
      fields: [
        { key: 'rfidTag', label: 'RFID tag', type: 'text', required: true, max: 30, placeholder: 'e.g. RFID-000123' },
        { key: 'passengerId', label: 'Passenger', type: 'ref', ref: 'passengers', required: true },
        { key: 'flightId', label: 'Flight', type: 'ref', ref: 'flights', required: true },
        { key: 'weight', label: 'Weight (kg)', type: 'number', step: '0.01', min: 0, max: 999.99 },
        { key: 'color', label: 'Colour', type: 'text', max: 30 },
        { key: 'securityStatus', label: 'Security status', type: 'select', options: () => META.securityStatus, def: 'Pending' },
        { key: 'baggageStatus', label: 'Baggage status', type: 'select', options: () => META.baggageStatus, def: 'Checked_In' }
      ],
      extra: r => `<button class="icon-btn" data-act="status" title="Change status" aria-label="Change status">${svg('swap')}</button>`
    },
    passengers: {
      title: 'Passengers', eyebrow: 'People', icon: 'people', endpoint: '/passengers', id: 'passengerId', noun: 'passenger',
      cols: [
        { key: 'passengerId', label: 'ID', render: r => `<span class="mono">#${r.passengerId}</span>` },
        { key: 'firstName', label: 'Name', render: r => `<b>${esc(r.firstName)} ${esc(r.lastName || '')}</b>` },
        { key: 'passportNumber', label: 'Passport', render: r => `<span class="mono">${esc(r.passportNumber || '—')}</span>` },
        { key: 'nationality', label: 'Nationality', render: r => esc(r.nationality || '—') },
        { key: 'mobileNumber', label: 'Mobile', render: r => `<span class="mono">${esc(r.mobileNumber || '—')}</span>` },
        { key: 'email', label: 'Email', render: r => esc(r.email || '—') }
      ],
      fields: [
        { key: 'firstName', label: 'First name', type: 'text', required: true, max: 50 },
        { key: 'lastName', label: 'Last name', type: 'text', max: 50 },
        { key: 'passportNumber', label: 'Passport number', type: 'text', max: 20 },
        { key: 'nationality', label: 'Nationality', type: 'combo', list: 'countries', max: 50 },
        { key: 'mobileNumber', label: 'Mobile number', type: 'tel', max: 30 },
        { key: 'email', label: 'Email', type: 'email', max: 100 }
      ]
    },
    flights: {
      title: 'Flights', eyebrow: 'Schedule', icon: 'plane', endpoint: '/flights', id: 'flightId', noun: 'flight',
      cols: [
        { key: 'flightNumber', label: 'Flight', render: r => `<b class="mono">${esc(r.flightNumber)}</b>` },
        { key: 'airline', label: 'Airline', render: r => esc(r.airline) },
        { key: 'departure', label: 'Route', render: r => `${esc(r.departure)} <span class="muted">→</span> ${esc(r.arrival)}` },
        { key: 'departureTime', label: 'Departs', render: r => `<span class="mono">${fmtDT(r.departureTime)}</span>` },
        { key: 'flightType', label: 'Type', render: r => badge(r.flightType) },
        { key: 'flightStatus', label: 'Status', render: r => badge(r.flightStatus) }
      ],
      fields: [
        { key: 'flightNumber', label: 'Flight number', type: 'text', required: true, max: 10, placeholder: 'e.g. AI302' },
        { key: 'airline', label: 'Airline', type: 'text', required: true, max: 50 },
        { key: 'departure', label: 'Departure airport / city', type: 'text', required: true, max: 50 },
        { key: 'arrival', label: 'Arrival airport / city', type: 'text', required: true, max: 50 },
        { key: 'departureTime', label: 'Departure time', type: 'datetime', required: true },
        { key: 'flightType', label: 'Flight type', type: 'select', options: () => META.flightType, def: 'Domestic' },
        { key: 'flightStatus', label: 'Flight status', type: 'combo', options: FLIGHT_STATUSES, max: 20, def: 'Scheduled' }
      ]
    },
    claims: {
      title: 'Baggage Claim', eyebrow: 'Arrivals hall', icon: 'claim', endpoint: '/claims', id: 'claimId', noun: 'claim', filterKey: 'status',
      filters: () => META.claimStatus,
      cols: [
        { key: 'claimId', label: 'ID', render: r => `<span class="mono">#${r.claimId}</span>` },
        { key: 'rfidTag', label: 'Bag', render: r => r.rfidTag ? `<a class="mono" href="#/track/${encodeURIComponent(r.rfidTag)}">${esc(r.rfidTag)}</a>` : '—' },
        { key: 'passengerName', label: 'Passenger', render: r => esc(r.passengerName || '—') },
        { key: 'beltNumber', label: 'Belt', render: r => `<span class="mono">${esc(r.beltNumber || '—')}</span>` },
        { key: 'currentLocation', label: 'Location', render: r => esc(r.currentLocation || '—') },
        { key: 'collectionTime', label: 'Collected', render: r => `<span class="mono">${fmtDT(r.collectionTime)}</span>` },
        { key: 'claimStatus', label: 'Claim', render: r => badge(r.claimStatus) },
        { key: 'lostStatus', label: 'Lost?', render: r => badge(r.lostStatus === 'Yes' ? 'Lost' : null) || '' }
      ],
      fields: [
        { key: 'baggageId', label: 'Baggage', type: 'ref', ref: 'baggage', required: true, full: true },
        { key: 'beltNumber', label: 'Belt number', type: 'text', max: 10 },
        { key: 'currentLocation', label: 'Current location', type: 'text', max: 100 },
        { key: 'claimStatus', label: 'Claim status', type: 'select', options: () => META.claimStatus, def: 'Pending' },
        { key: 'lostStatus', label: 'Lost status', type: 'select', options: () => META.lostStatus, def: 'No' },
        { key: 'collectionTime', label: 'Collection time', type: 'datetime', full: true }
      ],
      extra: r => r.claimStatus === 'Pending' && r.lostStatus !== 'Yes'
        ? `<button class="btn sm" data-act="collect" title="Mark collected">${svg('check')} Collect</button><button class="btn sm" data-act="lost" title="Report lost">${svg('flag')} Lost</button>`
        : (r.lostStatus === 'Yes' ? '<span class="badge" style="--c: var(--st-lost)">Lost</span>' : '')
    },
    countries: {
      title: 'Countries', eyebrow: 'Reference', icon: 'globe', endpoint: '/countries', id: 'countryCode', noun: 'country', idEditable: true,
      cols: [
        { key: 'countryCode', label: 'Code', render: r => `<b class="mono">${esc(r.countryCode)}</b>` },
        { key: 'nationality', label: 'Nationality', render: r => esc(r.nationality || '—') }
      ],
      fields: [
        { key: 'countryCode', label: 'Country code (2 letters)', type: 'text', required: true, max: 2, placeholder: 'IN', lockOnEdit: true },
        { key: 'nationality', label: 'Nationality', type: 'text', max: 30, placeholder: 'Indian' }
      ]
    }
  };

  const NAV = [
    ['dashboard', 'Dashboard', 'grid'], ['baggage', 'Baggage', 'bag'], ['track', 'Track a bag', 'radar'], ['sep'],
    ['flights', 'Flights', 'plane'], ['passengers', 'Passengers', 'people'], ['claims', 'Baggage claim', 'claim'], ['countries', 'Countries', 'globe']
  ];

  // ---------------------------------------------------------------- shell
  function renderNav(active) {
    $('#nav').innerHTML = NAV.map(n => n[0] === 'sep' ? '<div class="sep"></div>'
      : `<a href="#/${n[0]}" class="${n[0] === active ? 'active' : ''}" ${n[0] === active ? 'aria-current="page"' : ''}>${svg(n[2])}<span>${n[1]}</span></a>`).join('');
  }

  function setTheme(t) {
    document.documentElement.dataset.theme = t;
    store.set('skytrack-theme', t);
    $('#themeBtn').innerHTML = (t === 'dark' ? svg('sun') + '<span>Light mode</span>' : svg('moon') + '<span>Dark mode</span>');
  }

  function tickClock() {
    const d = new Date();
    $('#clockTime').textContent = d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    $('#clockDate').textContent = d.toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
  }

  function closeMenu() { $('#sidebar').classList.remove('open'); $('#scrim').classList.remove('show'); }

  function head(eyebrow, title) {
    $('#eyebrow').textContent = eyebrow;
    $('#pageTitle').textContent = title;
    document.title = `${title} · SkyTrack`;
  }

  // ---------------------------------------------------------------- dashboard
  function donut(items) {
    const total = items.reduce((s, i) => s + i.value, 0);
    if (!total) return '<p class="muted">No data yet.</p>';
    let acc = 0;
    const segs = items.map(i => {
      const pct = i.value / total * 100;
      const s = `<circle cx="21" cy="21" r="15.9155" pathLength="100" style="stroke: var(--st-${keyOf(i.label)}, var(--muted))" stroke-dasharray="${Math.max(pct - 0.6, 0.1)} ${100 - Math.max(pct - 0.6, 0.1)}" stroke-dashoffset="${25 - acc}"><title>${esc(label(i.label))}: ${i.value}</title></circle>`;
      acc += pct;
      return s;
    }).join('');
    return `<div class="donut-wrap">
      <svg class="donut" viewBox="0 0 42 42" role="img" aria-label="Baggage by status">
        <circle cx="21" cy="21" r="15.9155" style="stroke: var(--surface-2)"/>${segs}
        <text class="big" x="21" y="23">${total}</text><text class="small" x="21" y="27.500">bags</text>
      </svg>
      <div class="legend">${items.map(i => `<div style="--c: var(--st-${keyOf(i.label)}, var(--muted))"><i></i>${esc(label(i.label))}<b>${i.value}</b></div>`).join('')}</div>
    </div>`;
  }

  function bars(items, colorByKey = true) {
    if (!items.length) return '<p class="muted">No data yet.</p>';
    const max = Math.max(...items.map(i => i.value), 1);
    return `<div class="bars">${items.map(i => `<div class="bar" style="--c: ${colorByKey ? `var(--st-${keyOf(i.label)}, var(--accent))` : 'var(--accent)'}">
      <span title="${esc(label(i.label))}">${esc(label(i.label))}</span><span class="track"><span class="fill" style="display:block;width:${(i.value / max * 100).toFixed(1)}%"></span></span><span>${i.value}</span></div>`).join('')}</div>`;
  }

  async function dashboard() {
    head('Terminal overview', 'Dashboard');
    $('#view').innerHTML = '<div class="kpis">' + '<div class="skeleton"></div>'.repeat(6) + '</div>';
    const d = await api('/dashboard');
    const t = d.totals;
    const kpi = (lbl, num, sub, color) => `<div class="card kpi" style="--k: ${color}"><div class="label">${lbl}</div><div class="num">${num}</div><div class="sub">${sub}</div></div>`;
    $('#view').innerHTML = `
      <div class="kpis">
        ${kpi('Bags in system', t.bags, `${t.totalWeight} kg total · avg ${t.avgWeight} kg`, 'var(--accent)')}
        ${kpi('Passengers', t.passengers, `${t.countries} countries on file`, 'var(--st-checked_in)')}
        ${kpi('Flights', t.flights, 'scheduled &amp; operated', 'var(--st-departed)')}
        ${kpi('Pending claims', t.pendingClaims, 'awaiting pickup', 'var(--st-pending)')}
        ${kpi('Lost bags', t.lost, t.lost ? 'needs attention' : 'all accounted for', 'var(--st-lost)')}
        ${kpi('Security failed', t.securityFailed, 'flagged on screening', 'var(--st-failed)')}
      </div>
      <div class="grid-3">
        <section class="card"><div class="card-head"><h2>Baggage by status</h2></div><div class="card-body">${donut(d.byStatus)}</div></section>
        <section class="card"><div class="card-head"><h2>Security screening</h2></div><div class="card-body">${bars(d.bySecurity)}</div></section>
        <section class="card"><div class="card-head"><h2>Flight status</h2></div><div class="card-body">${bars(d.flightsByStatus)}</div></section>
      </div>
      <div class="grid-2">
        <section class="card"><div class="card-head"><h2>Busiest flights</h2><span class="muted">bags checked</span></div><div class="card-body">${bars(d.topFlights, false)}</div></section>
        <section class="card"><div class="card-head"><h2>Latest check-ins</h2><a class="btn sm" href="#/baggage">View all</a></div>
          <div class="card-body"><div class="board">${d.recent.length ? d.recent.map(r => `
            <div class="board-row"><span class="mono">#${r.baggageId}</span>
              <a class="mono" href="#/track/${encodeURIComponent(r.rfidTag)}">${esc(r.rfidTag)}</a>
              <span class="hide-sm">${esc(r.passengerName || '—')}</span>
              <span class="mono hide-sm">${esc(r.flightNumber || '—')}</span>${badge(r.status)}</div>`).join('') : '<p class="muted">No baggage yet.</p>'}</div></div></section>
      </div>`;
  }

  // ---------------------------------------------------------------- generic CRUD page
  async function crudPage(name) {
    const cfg = ENT[name];
    head(cfg.eyebrow, cfg.title);
    const st = { rows: [], q: '', filter: '', sort: null, dir: 1, page: 1, size: 12 };
    $('#view').innerHTML = `
      <div class="toolbar">
        <label class="search" aria-label="Search">${svg('search')}<input id="q" type="search" placeholder="Search ${cfg.title.toLowerCase()}…" autocomplete="off"></label>
        ${cfg.filters ? '<div class="chips" id="chips"></div>' : ''}
        <span class="spacer"></span>
        <button class="btn primary" id="addBtn">${svg('plus')} Add ${cfg.noun}</button>
      </div>
      <div class="card table-card"><div class="table-scroll" id="tbl"></div><div class="pager" id="pager"></div></div>`;

    const load = async () => {
      const p = new URLSearchParams();
      if (st.q) p.set('q', st.q);
      if (st.filter) p.set('status', st.filter);
      try { st.rows = await api(cfg.endpoint + (p.toString() ? '?' + p : '')); } catch (e) { toast(e.message, true); st.rows = []; }
      st.page = 1;
      draw();
    };

    const drawChips = () => {
      if (!cfg.filters) return;
      $('#chips').innerHTML = ['', ...cfg.filters()].map(f => `<button class="chip ${st.filter === f ? 'on' : ''}" data-f="${esc(f)}">${f ? esc(label(f)) : 'All'}</button>`).join('');
    };

    const draw = () => {
      let rows = st.rows.slice();
      if (st.sort) {
        rows.sort((a, b) => {
          const x = a[st.sort], y = b[st.sort];
          if (x == null) return 1; if (y == null) return -1;
          return (typeof x === 'number' ? x - y : String(x).localeCompare(String(y), undefined, { numeric: true })) * st.dir;
        });
      }
      const pages = Math.max(1, Math.ceil(rows.length / st.size));
      st.page = Math.min(st.page, pages);
      const slice = rows.slice((st.page - 1) * st.size, st.page * st.size);
      drawChips();
      if (!rows.length) {
        $('#tbl').innerHTML = `<div class="empty"><b>Nothing here</b>${st.q || st.filter ? 'No records match your search.' : `No ${cfg.title.toLowerCase()} yet — add the first one.`}</div>`;
        $('#pager').innerHTML = '';
        return;
      }
      $('#tbl').innerHTML = `<table><thead><tr>${cfg.cols.map(c => `<th class="sortable" data-s="${c.key}">${c.label}${st.sort === c.key ? `<span class="arr">${st.dir > 0 ? '▲' : '▼'}</span>` : ''}</th>`).join('')}<th></th></tr></thead>
        <tbody>${slice.map(r => `<tr data-id="${esc(r[cfg.id])}">${cfg.cols.map(c => `<td>${c.render(r)}</td>`).join('')}
          <td><div class="row-actions">${cfg.extra ? cfg.extra(r) : ''}
            <button class="icon-btn" data-act="edit" title="Edit" aria-label="Edit">${svg('edit')}</button>
            <button class="icon-btn del" data-act="del" title="Delete" aria-label="Delete">${svg('trash')}</button></div></td></tr>`).join('')}</tbody></table>`;
      $('#pager').innerHTML = `<span>${rows.length} record${rows.length === 1 ? '' : 's'}</span><div>
        <button class="btn sm" data-p="-1" ${st.page <= 1 ? 'disabled' : ''}>Prev</button><span style="align-self:center">${st.page} / ${pages}</span>
        <button class="btn sm" data-p="1" ${st.page >= pages ? 'disabled' : ''}>Next</button></div>`;
    };

    let timer;
    $('#q').addEventListener('input', e => { clearTimeout(timer); timer = setTimeout(() => { st.q = e.target.value.trim(); load(); }, 250); });
    $('#view').addEventListener('click', async e => {
      const chip = e.target.closest('.chip'); if (chip) { st.filter = chip.dataset.f; load(); return; }
      const th = e.target.closest('th.sortable'); if (th) { st.dir = st.sort === th.dataset.s ? -st.dir : 1; st.sort = th.dataset.s; draw(); return; }
      const pg = e.target.closest('[data-p]'); if (pg) { st.page += Number(pg.dataset.p); draw(); return; }
      if (e.target.closest('#addBtn')) { if (await openForm(name, null)) load(); return; }
      const btn = e.target.closest('[data-act]'); if (!btn) return;
      const id = btn.closest('tr').dataset.id;
      const row = st.rows.find(r => String(r[cfg.id]) === id);
      try {
        if (btn.dataset.act === 'edit') { if (await openForm(name, row)) load(); }
        else if (btn.dataset.act === 'del') {
          if (await confirmBox(`Delete ${cfg.noun}?`, `This permanently removes this ${cfg.noun} from the database.`)) {
            await api(`${cfg.endpoint}/${encodeURIComponent(id)}`, { method: 'DELETE' }); toast(`${cfg.noun[0].toUpperCase() + cfg.noun.slice(1)} deleted`); load();
          }
        } else if (btn.dataset.act === 'status') {
          const s = await pickStatus(row.baggageStatus);
          if (s && s !== row.baggageStatus) { await api(`/baggage/${id}/status`, { method: 'PATCH', body: { baggageStatus: s } }); toast(`Status → ${label(s)}`); load(); }
        } else if (btn.dataset.act === 'collect') {
          await api(`/claims/${id}/collect`, { method: 'POST' }); toast('Marked as collected'); load();
        } else if (btn.dataset.act === 'lost') {
          if (await confirmBox('Report bag as lost?', 'The claim is flagged lost and the baggage status becomes “Lost”.', 'Report lost')) {
            await api(`/claims/${id}/report-lost`, { method: 'POST' }); toast('Reported as lost'); load();
          }
        }
      } catch (err) { toast(err.message, true); }
    });
    await load();
  }

  // ---------------------------------------------------------------- dialogs
  const refCache = {};
  async function refOptions(kind) {
    const make = {
      flights: r => [r.flightId, `${r.flightNumber} · ${r.departure} → ${r.arrival}`],
      passengers: r => [r.passengerId, `${r.firstName} ${r.lastName || ''} (#${r.passengerId})`],
      baggage: r => [r.baggageId, `${r.rfidTag} · ${r.passengerName || 'unknown'}`],
      countries: r => [r.countryCode, r.nationality]
    }[kind];
    const rows = await api('/' + kind);
    return rows.map(make);
  }

  function openForm(name, row) {
    const cfg = ENT[name];
    const dlg = $('#formDialog');
    return (async () => {
      $('#formTitle').textContent = `${row ? 'Edit' : 'Add'} ${cfg.noun}`;
      $('#formError').textContent = '';
      let html = '';
      for (const f of cfg.fields) {
        const v = row ? row[f.key] : (f.def ?? '');
        const id = 'f_' + f.key;
        const req = f.required ? ' <span class="req">*</span>' : '';
        const lock = row && f.lockOnEdit ? 'readonly' : '';
        let control;
        if (f.type === 'ref') {
          const opts = await refOptions(f.ref);
          control = `<select id="${id}" ${f.required ? 'required' : ''}><option value="">Select…</option>${opts.map(o => `<option value="${esc(o[0])}" ${String(v) === String(o[0]) ? 'selected' : ''}>${esc(o[1])}</option>`).join('')}</select>`;
        } else if (f.type === 'select') {
          control = `<select id="${id}">${f.options().map(o => `<option value="${esc(o)}" ${o === v ? 'selected' : ''}>${esc(label(o))}</option>`).join('')}</select>`;
        } else if (f.type === 'combo') {
          const list = f.options || (await refOptions(f.list)).map(o => o[1]).filter(Boolean);
          control = `<input id="${id}" list="${id}_l" value="${esc(v)}" maxlength="${f.max || 100}" autocomplete="off"><datalist id="${id}_l">${list.map(o => `<option value="${esc(o)}">`).join('')}</datalist>`;
        } else if (f.type === 'datetime') {
          control = `<input id="${id}" type="datetime-local" value="${esc(toInput(v))}" ${f.required ? 'required' : ''}>`;
        } else if (f.type === 'number') {
          control = `<input id="${id}" type="number" step="${f.step || 1}" min="${f.min ?? ''}" max="${f.max ?? ''}" value="${esc(v)}">`;
        } else {
          control = `<input id="${id}" type="${f.type}" value="${esc(v)}" maxlength="${f.max || 100}" placeholder="${esc(f.placeholder || '')}" ${f.required ? 'required' : ''} ${lock}>`;
        }
        html += `<label class="field ${f.full ? 'full' : ''}" for="${id}"><span>${f.label}${req}</span>${control}</label>`;
      }
      $('#formBody').innerHTML = html;

      return new Promise(resolve => {
        const form = $('#entityForm');
        const onClose = () => { form.removeEventListener('submit', onSubmit); dlg.removeEventListener('close', onClose); resolve(saved); };
        let saved = false;
        const onSubmit = async e => {
                    e.preventDefault();
          const body = {};
          for (const f of cfg.fields) {
            let v = $('#f_' + f.key).value.trim();
            if (f.required && !v) { $('#formError').textContent = `${f.label} is required.`; $('#f_' + f.key).focus(); return; }
            if (v === '') v = null;
            else if (f.type === 'ref' && name !== 'countries') v = Number(v);
            else if (f.type === 'number') v = Number(v);
            body[f.key] = v;
          }
          const btn = $('#formSave'); btn.disabled = true;
          try {
            const key = row ? row[cfg.id] : null;
            await api(row ? `${cfg.endpoint}/${encodeURIComponent(key)}` : cfg.endpoint, { method: row ? 'PUT' : 'POST', body });
            toast(row ? 'Changes saved' : `${cfg.noun[0].toUpperCase() + cfg.noun.slice(1)} added`);
            saved = true; dlg.close();
          } catch (err) { $('#formError').textContent = err.message; }
          finally { btn.disabled = false; }
        };
        const onClick = e => { if (e.target.closest('[data-close]')) dlg.close(); };
        form.addEventListener('submit', onSubmit);
        dlg.addEventListener('click', onClick);
        dlg.addEventListener('close', () => dlg.removeEventListener('click', onClick), { once: true });
        dlg.addEventListener('close', onClose);
        dlg.showModal();
      });
    })().catch(err => { toast(err.message, true); return false; });
  }

  function confirmBox(title, text, okLabel = 'Delete') {
    const dlg = $('#confirmDialog');
    $('#confirmTitle').textContent = title; $('#confirmText').textContent = text; $('#confirmOk').textContent = okLabel;
    dlg.returnValue = ''; // otherwise Esc would re-use the previous answer
    return new Promise(resolve => { dlg.addEventListener('close', () => resolve(dlg.returnValue === 'ok'), { once: true }); dlg.showModal(); });
  }

  function pickStatus(current) {
    const dlg = $('#confirmDialog');
    $('#confirmTitle').textContent = 'Update baggage status';
    $('#confirmText').innerHTML = `<div class="status-pick">${META.baggageStatus.map(s => `<button type="button" class="chip ${s === current ? 'on' : ''}" data-s="${esc(s)}">${esc(label(s))}</button>`).join('')}</div>`;
    $('#confirmOk').style.display = 'none';
    return new Promise(resolve => {
      let chosen = null;
      const pick = e => { const b = e.target.closest('[data-s]'); if (b) { chosen = b.dataset.s; dlg.close(); } };
      dlg.addEventListener('click', pick);
      dlg.addEventListener('close', () => { dlg.removeEventListener('click', pick); $('#confirmOk').style.display = ''; resolve(chosen); }, { once: true });
      dlg.showModal();
    });
  }

  // ---------------------------------------------------------------- tracker
  async function tracker(tag) {
    head('Real-time lookup', 'Track a bag');
    $('#view').innerHTML = `
      <div class="card track-hero">
        <label class="search" aria-label="RFID tag">${svg('search')}<input id="tagInput" placeholder="Enter RFID tag or baggage ID" value="${esc(tag || '')}" autocomplete="off"></label>
        <button class="btn primary" id="tagGo">Track</button>
      </div>
      <div id="trackOut">${tag ? '' : '<div class="empty card"><b>Where is my bag?</b>Enter an RFID tag to see its full journey, owner, flight and claim belt.</div>'}</div>`;
    const go = () => { const v = $('#tagInput').value.trim(); if (v) location.hash = '#/track/' + encodeURIComponent(v); };
    $('#tagGo').onclick = go;
    $('#tagInput').onkeydown = e => { if (e.key === 'Enter') go(); };
    if (!tag) { $('#tagInput').focus(); return; }

    try {
      const r = await api('/baggage/track/' + encodeURIComponent(tag));
      const b = r.baggage, f = r.flight, p = r.passenger, c = r.claim;
      const steps = [['Checked_In', 'Checked in'], ['In Transit', 'In transit'], ['Arrived', 'Arrived'], ['Collected', 'Collected']];
      const idx = Math.max(0, steps.findIndex(s => s[0] === b.baggageStatus));
      const lost = b.baggageStatus === 'Lost';
      $('#trackOut').innerHTML = `
        ${lost ? '<div class="alert">This bag is marked as LOST. Contact the baggage service desk.</div>' : ''}
        ${b.securityStatus === 'Failed' ? '<div class="alert">This bag failed security screening and is on hold.</div>' : ''}
        <section class="card tag-card">
          <div><p class="muted" style="font-size:12px;letter-spacing:.16em;text-transform:uppercase">Baggage tag · #${b.baggageId}</p>
            <div class="rfid">${esc(b.rfidTag)}</div>
            <div class="tag-meta">
              <div><small>Passenger</small><b>${esc(b.passengerName || '—')}</b></div>
              <div><small>Flight</small><b class="mono">${esc(b.flightNumber || '—')}</b></div>
              <div><small>Weight</small><b class="mono">${b.weight == null ? '—' : Number(b.weight).toFixed(2) + ' kg'}</b></div>
              <div><small>Colour</small><b>${esc(b.color || '—')}</b></div>
            </div></div>
          <div style="text-align:right;display:grid;gap:8px;align-content:start">${badge(b.baggageStatus)}${badge(b.securityStatus)}</div>
        </section>
        <section class="card" style="margin-bottom:18px"><div class="card-head"><h2>Journey</h2></div><div class="card-body">
          <div class="stepper">${steps.map((s, i) => `<div class="step ${!lost && i <= idx ? 'done' : ''} ${!lost && i === idx ? 'now' : ''}"><i></i>${s[1]}</div>`).join('')}</div>
          ${lost ? `<div class="stepper" style="grid-template-columns:1fr"><div class="step lost now"><i></i>Lost — last known: ${esc(c?.currentLocation || 'unknown')}</div></div>` : ''}
        </div></section>
        <div class="info-grid">
          <section class="card"><div class="card-head"><h2>Passenger</h2></div><div class="card-body kv">
            <div><span>Name</span><span>${esc(p ? `${p.firstName} ${p.lastName || ''}` : '—')}</span></div>
            <div><span>Passport</span><span class="mono">${esc(p?.passportNumber || '—')}</span></div>
            <div><span>Mobile</span><span class="mono">${esc(p?.mobileNumber || '—')}</span></div>
            <div><span>Email</span><span>${esc(p?.email || '—')}</span></div></div></section>
          <section class="card"><div class="card-head"><h2>Flight</h2></div><div class="card-body kv">
            <div><span>Flight</span><span class="mono">${esc(f?.flightNumber || '—')}</span></div>
            <div><span>Airline</span><span>${esc(f?.airline || '—')}</span></div>
            <div><span>Route</span><span>${f ? `${esc(f.departure)} → ${esc(f.arrival)}` : '—'}</span></div>
            <div><span>Departs</span><span class="mono">${fmtDT(f?.departureTime)}</span></div>
            <div><span>Status</span><span>${badge(f?.flightStatus)}</span></div></div></section>
          <section class="card"><div class="card-head"><h2>Claim</h2></div><div class="card-body kv">
            ${c ? `<div><span>Belt</span><span class="mono">${esc(c.beltNumber || '—')}</span></div>
            <div><span>Location</span><span>${esc(c.currentLocation || '—')}</span></div>
            <div><span>Claim</span><span>${badge(c.claimStatus)}</span></div>
            <div><span>Collected</span><span class="mono">${fmtDT(c.collectionTime)}</span></div>` : '<div><span>No claim record yet</span><span></span></div>'}</div></section>
        </div>`;
    } catch (e) {
      $('#trackOut').innerHTML = `<div class="empty card"><b>No match</b>${esc(e.message)}</div>`;
    }
  }

  // ---------------------------------------------------------------- router
  async function route() {
    closeMenu();
    const [, name = 'dashboard', arg] = location.hash.split('/');
    const page = ENT[name] || name === 'track' ? name : 'dashboard';
    renderNav(page);
    const view = $('#view');
    const fresh = view.cloneNode(false); // drops listeners from the previous page
    view.replaceWith(fresh);
    try {
      if (page === 'dashboard') await dashboard();
      else if (page === 'track') await tracker(arg ? decodeURIComponent(arg) : '');
      else await crudPage(page);
    } catch (e) {
      $('#view').innerHTML = `<div class="empty card"><b>Can't reach the server</b>${esc(e.message)}</div>`;
      toast(e.message, true);
    }
    window.scrollTo({ top: 0 });
  }

  async function init() {
    setTheme(store.get('skytrack-theme') || (matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'));
    $('#themeBtn').addEventListener('click', () => setTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'));
    $('#menuBtn').addEventListener('click', () => { $('#sidebar').classList.add('open'); $('#scrim').classList.add('show'); });
    $('#scrim').addEventListener('click', closeMenu);
    tickClock(); setInterval(tickClock, 1000);
    try {
      META = await api('/meta');
      const t = await api('/dashboard');
      $('#dbState').className = 'db-state ok';
      $('#dbState').lastElementChild.textContent = `MySQL connected · ${t.totals.bags} bags`;
    } catch (e) {
      $('#dbState').className = 'db-state bad';
      $('#dbState').lastElementChild.textContent = 'Database unreachable';
    }
    window.addEventListener('hashchange', route);
    route();
  }
  init();
})();
