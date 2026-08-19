const PERIOD_LABELS = { DAY: 'al dia', WEEK: 'por semana', MONTH: 'al mes' };
const WEEKDAY_LABELS = {
    MONDAY: 'L', TUESDAY: 'M', WEDNESDAY: 'X', THURSDAY: 'J', FRIDAY: 'V', SATURDAY: 'S', SUNDAY: 'D'
};

const status = document.getElementById('status');
const todayList = document.getElementById('today-list');
const todayEmpty = document.getElementById('today-empty');
const routineList = document.getElementById('routine-list');
const includeArchived = document.getElementById('include-archived');
const defineForm = document.getElementById('define-form');
const periodSelect = defineForm.elements.period;
const weekdays = document.getElementById('weekdays');
const daysOfMonth = document.getElementById('days-of-month');

async function call(method, path, body) {
    const response = await fetch(path, {
        method,
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body)
    });

    if (response.status === 204) {
        return null;
    }

    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(payload && payload.message ? payload.message : 'Error ' + response.status);
    }

    return payload;
}

function notify(message, kind) {
    status.textContent = message;
    status.dataset.kind = kind || 'info';
    status.hidden = false;

    if (kind !== 'error') {
        setTimeout(() => { status.hidden = true; }, 3000);
    }
}

async function guard(work) {
    try {
        await work();
    } catch (error) {
        notify(error.message, 'error');
    }
}

function quotaText(progress) {
    const unit = progress.unit ? ' ' + progress.unit : '';

    return progress.logged + '/' + progress.target + unit;
}

function scheduleText(routine) {
    const unit = routine.unit ? ' ' + routine.unit : '';
    const period = PERIOD_LABELS[routine.period] || routine.period;
    const days = (routine.activeDays || []).map(day => WEEKDAY_LABELS[day] || day).join('');
    const monthDays = (routine.daysOfMonth || [])
        .map(day => (day === 0 ? 'fin de mes' : 'dia ' + day))
        .join(', ');

    let text = routine.target + unit + ' ' + period;
    if (days && days.length < 7) {
        text += ' (' + days + ')';
    }
    if (monthDays) {
        text += ' (' + monthDays + ')';
    }

    return text;
}

function progressBar(progress) {
    const ratio = Number(progress.target) === 0
        ? 0
        : Math.min(1, Number(progress.logged) / Number(progress.target));
    const bar = document.createElement('div');
    bar.className = progress.met ? 'bar met' : 'bar';
    const fill = document.createElement('span');
    fill.style.width = Math.round(ratio * 100) + '%';
    bar.append(fill);

    return bar;
}

function todayCard(entry) {
    const item = document.createElement('li');
    item.className = entry.progress.met ? 'card met' : 'card';

    const main = document.createElement('div');
    main.className = 'card-main';
    const name = document.createElement('div');
    name.className = 'card-name';
    name.textContent = entry.routine.name;
    const meta = document.createElement('div');
    meta.className = 'card-meta';
    meta.textContent = PERIOD_LABELS[entry.routine.period] || entry.routine.period;
    main.append(name, meta);

    const quota = document.createElement('span');
    quota.className = entry.progress.met ? 'quota met' : 'quota';
    quota.textContent = quotaText(entry.progress);

    const streak = document.createElement('span');
    streak.className = 'streak';
    streak.textContent = '···';
    call('GET', '/routines/' + entry.routine.id + '/streak')
        .then(value => { streak.textContent = 'racha ' + value.current + ' (mejor ' + value.best + ')'; })
        .catch(() => { streak.textContent = ''; });

    const mark = document.createElement('button');
    mark.className = 'primary';
    mark.textContent = 'Marcar';
    mark.addEventListener('click', () => guard(async () => {
        await call('POST', '/routines/' + entry.routine.id + '/entries', {});
        notify('Marcado');
        await refresh();
    }));

    const clear = document.createElement('button');
    clear.textContent = 'Deshacer hoy';
    clear.addEventListener('click', () => guard(async () => {
        await call('DELETE', '/routines/' + entry.routine.id + '/entries/' + todayIso());
        notify('Desmarcado');
        await refresh();
    }));

    item.append(main, quota, streak, mark, clear, progressBar(entry.progress));

    return item;
}

function routineCard(routine) {
    const item = document.createElement('li');
    item.className = routine.archived ? 'card archived' : 'card';

    const main = document.createElement('div');
    main.className = 'card-main';
    const name = document.createElement('div');
    name.className = 'card-name';
    name.textContent = routine.name;
    const meta = document.createElement('div');
    meta.className = 'card-meta';
    meta.textContent = scheduleText(routine);
    main.append(name, meta);

    const toggle = document.createElement('button');
    toggle.textContent = routine.archived ? 'Desarchivar' : 'Archivar';
    toggle.addEventListener('click', () => guard(async () => {
        await call('POST', '/routines/' + routine.id + (routine.archived ? '/unarchive' : '/archive'));
        notify(routine.archived ? 'Desarchivada' : 'Archivada');
        await refresh();
    }));

    const remove = document.createElement('button');
    remove.className = 'danger';
    remove.textContent = 'Borrar';
    remove.addEventListener('click', () => guard(async () => {
        if (!window.confirm('Borrar "' + routine.name + '" y todo su historial?')) {
            return;
        }

        await call('DELETE', '/routines/' + routine.id);
        notify('Borrada');
        await refresh();
    }));

    item.append(main, toggle, remove);

    return item;
}

function todayIso() {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;

    return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

async function refreshToday() {
    const entries = await call('GET', '/routines/today');
    todayList.replaceChildren(...entries.map(todayCard));
    todayEmpty.hidden = entries.length > 0;
}

async function refreshRoutines() {
    const routines = await call('GET', '/routines?includeArchived=' + includeArchived.checked);
    routineList.replaceChildren(...routines.map(routineCard));
}

async function refresh() {
    await Promise.all([refreshToday(), refreshRoutines()]);
}

function readDaysOfMonth(raw) {
    if (!raw || !raw.trim()) {
        return [];
    }

    return raw.split(',')
        .map(part => Number(part.trim()))
        .filter(day => Number.isInteger(day));
}

function syncPeriodFields() {
    weekdays.hidden = periodSelect.value !== 'DAY';
    daysOfMonth.hidden = periodSelect.value !== 'MONTH';
}

defineForm.addEventListener('submit', event => {
    event.preventDefault();

    guard(async () => {
        const data = new FormData(defineForm);
        const period = data.get('period');
        const body = {
            name: data.get('name'),
            target: Number(data.get('target')),
            unit: data.get('unit') || null,
            period,
            activeDays: period === 'DAY' ? data.getAll('activeDays') : [],
            daysOfMonth: period === 'MONTH' ? readDaysOfMonth(data.get('daysOfMonth')) : []
        };

        await call('POST', '/routines', body);
        defineForm.reset();
        syncPeriodFields();
        notify('Rutina definida');
        await refresh();
    });
});

periodSelect.addEventListener('change', syncPeriodFields);
includeArchived.addEventListener('change', () => guard(refreshRoutines));

document.querySelectorAll('.nav-link').forEach(link => link.addEventListener('click', () => {
    document.querySelectorAll('.nav-link').forEach(other => other.classList.toggle('active', other === link));
    document.querySelectorAll('.view').forEach(view => {
        view.hidden = view.id !== 'view-' + link.dataset.view;
    });
}));

document.getElementById('today-date').textContent = new Date().toLocaleDateString('es-ES', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
});

syncPeriodFields();
guard(refresh);

const stream = new EventSource('/events');
['progressLogged', 'dayCleared', 'routineDefined', 'routineDeleted', 'routineArchived', 'routineUnarchived',
    'routineDetailsChanged', 'routineScheduleChanged']
    .forEach(name => stream.addEventListener(name, () => guard(refresh)));
