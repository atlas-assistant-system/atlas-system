(() => {
    const PERIOD_LABELS = { DAY: 'al día', WEEK: 'por semana', MONTH: 'al mes' };
    const WEEKDAY_LABELS = {
        MONDAY: 'L', TUESDAY: 'M', WEDNESDAY: 'X', THURSDAY: 'J',
        FRIDAY: 'V', SATURDAY: 'S', SUNDAY: 'D',
    };

    const status = document.getElementById('routines-status');
    const todayList = document.getElementById('routines-today-list');
    const todayEmpty = document.getElementById('routines-today-empty');
    const routineList = document.getElementById('routines-list');
    const summaryList = document.getElementById('today-routines');
    const summaryProgress = document.getElementById('routines-progress');
    const summaryDetail = document.getElementById('routines-detail');
    const includeArchived = document.getElementById('routines-include-archived');
    const defineForm = document.getElementById('routines-define-form');
    const periodSelect = defineForm.elements.period;
    const weekdays = document.getElementById('routines-weekdays');
    const daysOfMonth = document.getElementById('routines-days-of-month');
    let started = false;

    async function call(method, path, body) {
        const response = await fetch(path, {
            method,
            headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        if (response.status === 204) return null;

        const payload = await response.json().catch(() => null);
        if (!response.ok) {
            throw new Error(errorMessage({ status: response.status, body: payload }));
        }
        return payload;
    }

    function notify(message, kind = 'info') {
        status.textContent = message;
        status.dataset.kind = kind;
        status.hidden = false;
        if (kind !== 'error') setTimeout(() => { status.hidden = true; }, 3000);
    }

    async function guard(work) {
        try {
            await work();
        } catch (error) {
            notify(frontendErrorMessage(error, 'No se pudo completar la operación.'), 'error');
        }
    }

    function quotaText(progress) {
        return progress.logged + '/' + progress.target + (progress.unit ? ' ' + progress.unit : '');
    }

    function scheduleText(routine) {
        const days = (routine.activeDays || []).map(day => WEEKDAY_LABELS[day] || day).join('');
        const monthDays = (routine.daysOfMonth || [])
            .map(day => day === 0 ? 'fin de mes' : 'día ' + day)
            .join(', ');
        let text = routine.target + (routine.unit ? ' ' + routine.unit : '')
            + ' ' + (PERIOD_LABELS[routine.period] || routine.period);
        if (days && days.length < 7) text += ' (' + days + ')';
        if (monthDays) text += ' (' + monthDays + ')';
        return text;
    }

    function progressBar(progress) {
        const ratio = Number(progress.target) === 0
            ? 0 : Math.min(1, Number(progress.logged) / Number(progress.target));
        const bar = document.createElement('div');
        bar.className = progress.met ? 'routines-bar met' : 'routines-bar';
        const fill = document.createElement('span');
        fill.style.width = Math.round(ratio * 100) + '%';
        bar.append(fill);
        return bar;
    }

    function todayCard(entry) {
        const item = document.createElement('li');
        item.className = entry.progress.met ? 'routines-card met' : 'routines-card';
        const main = document.createElement('div');
        main.className = 'routines-card-main';
        const name = document.createElement('strong');
        name.className = 'routines-card-name';
        name.textContent = entry.routine.name;
        const meta = document.createElement('span');
        meta.className = 'routines-card-meta';
        meta.textContent = PERIOD_LABELS[entry.routine.period] || entry.routine.period;
        main.append(name, meta);

        const quota = document.createElement('span');
        quota.className = entry.progress.met ? 'routines-quota met' : 'routines-quota';
        quota.textContent = quotaText(entry.progress);
        const streak = document.createElement('span');
        streak.className = 'routines-streak';
        streak.textContent = '···';
        call('GET', '/routines/' + entry.routine.id + '/streak')
            .then(value => { streak.textContent = 'racha ' + value.current + ' · mejor ' + value.best; })
            .catch(() => { streak.textContent = ''; });

        const mark = action('Marcar', 'routines-primary', async () => {
            await call('POST', '/routines/' + entry.routine.id + '/entries', {});
            notify('Marcado');
            await refresh();
        });
        const clear = action('Deshacer hoy', '', async () => {
            await call('DELETE', '/routines/' + entry.routine.id + '/entries/' + todayIso());
            notify('Desmarcado');
            await refresh();
        });
        item.append(main, quota, streak, mark, clear, progressBar(entry.progress));
        return item;
    }

    function routineCard(routine) {
        const item = document.createElement('li');
        item.className = 'routines-card' + (routine.archived ? ' archived' : '');
        const main = document.createElement('div');
        main.className = 'routines-card-main';
        const name = document.createElement('strong');
        name.className = 'routines-card-name';
        name.textContent = routine.name;
        const meta = document.createElement('span');
        meta.className = 'routines-card-meta';
        meta.textContent = scheduleText(routine);
        main.append(name, meta);
        const toggle = action(routine.archived ? 'Desarchivar' : 'Archivar', '', async () => {
            await call('POST', '/routines/' + routine.id + (routine.archived ? '/unarchive' : '/archive'));
            notify(routine.archived ? 'Desarchivada' : 'Archivada');
            await refresh();
        });
        const actions = document.createElement('div');
        actions.className = 'routines-card-actions';
        const remove = action('Borrar', 'routines-danger', () => {
            const warning = document.createElement('span');
            warning.className = 'routines-card-meta';
            warning.textContent = 'Se borrarán la rutina y todo su historial.';
            warning.setAttribute('role', 'status');
            const confirm = action('Borrar definitivamente', 'routines-danger', async () => {
                await call('DELETE', '/routines/' + routine.id);
                notify('Borrada');
                await refresh();
            });
            const cancel = action('Mejor no', '', () => {
                actions.replaceChildren(toggle, remove);
                remove.focus();
            });
            actions.replaceChildren(warning, confirm, cancel);
            confirm.focus();
        });
        actions.append(toggle, remove);
        item.append(main, actions);
        return item;
    }

    function action(text, className, work) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = className;
        button.textContent = text;
        button.addEventListener('click', () => guard(work));
        return button;
    }

    function todayIso() {
        const now = new Date();
        return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
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

    async function refreshSummary() {
        const entries = await call('GET', '/routines/today');
        const met = entries.filter(entry => entry.progress.met).length;
        summaryList.replaceChildren();
        summaryProgress.textContent = entries.length === 0 ? '—' : met + '/' + entries.length;
        summaryDetail.textContent = entries.length === 0
            ? 'Sin rutinas para hoy'
            : (met === entries.length ? 'Todo cumplido' : 'Cumplidas hoy');

        if (entries.length === 0) {
            const empty = document.createElement('li');
            empty.className = 'muted';
            empty.textContent = 'Nada para hoy';
            summaryList.appendChild(empty);
            return;
        }

        for (const entry of entries) {
            const line = document.createElement('li');
            line.className = entry.progress.met ? 'done' : '';
            line.textContent = entry.routine.name + ' · ' + quotaText(entry.progress);
            summaryList.appendChild(line);
        }
    }

    async function refresh() {
        await Promise.all([refreshToday(), refreshRoutines(), refreshSummary()]);
    }

    function listen() {
        if (started) return;
        started = true;
        const stream = new EventSource('/events/routines');
        ['progressLogged', 'dayCleared', 'routineDefined', 'routineDeleted', 'routineArchived',
            'routineUnarchived', 'routineDetailsChanged', 'routineScheduleChanged']
            .forEach(name => stream.addEventListener(name, () => guard(refresh)));
    }

    function readDaysOfMonth(raw) {
        if (!raw?.trim()) return [];
        return raw.split(',').map(part => Number(part.trim())).filter(Number.isInteger);
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
            await call('POST', '/routines', {
                name: data.get('name'),
                target: Number(data.get('target')),
                unit: data.get('unit') || null,
                period,
                activeDays: period === 'DAY' ? data.getAll('activeDays') : [],
                daysOfMonth: period === 'MONTH' ? readDaysOfMonth(data.get('daysOfMonth')) : [],
            });
            defineForm.reset();
            syncPeriodFields();
            notify('Rutina definida');
            await refresh();
        });
    });
    periodSelect.addEventListener('change', syncPeriodFields);
    includeArchived.addEventListener('change', () => guard(refreshRoutines));
    syncPeriodFields();

    window.AtlasRoutines = {
        activate() {
            guard(refresh);
            listen();
        },
        summary() {
            guard(refreshSummary);
            listen();
        },
    };
})();
