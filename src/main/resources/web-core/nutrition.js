(() => {
    const dateLabel = document.getElementById('nutrition-date');
    const headline = document.getElementById('nutrition-headline');
    const consumedLabel = document.getElementById('nutrition-consumed');
    const targetLabel = document.getElementById('nutrition-target');
    const macroList = document.getElementById('nutrition-macros');
    const noPlan = document.getElementById('nutrition-no-plan');
    const intakeList = document.getElementById('nutrition-intakes');
    const intakesEmpty = document.getElementById('nutrition-intakes-empty');
    const dayList = document.getElementById('nutrition-days');
    const daysEmpty = document.getElementById('nutrition-days-empty');
    const intakeForm = document.getElementById('nutrition-intake-form');
    const planForm = document.getElementById('nutrition-plan-form');
    const planSummary = document.getElementById('nutrition-plan-summary');
    const liveCalories = document.getElementById('nutrition-live-calories');
    const summaryRemaining = document.getElementById('nutrition-remaining');
    const summaryGoal = document.getElementById('nutrition-goal');
    const summaryEaten = document.getElementById('nutrition-eaten');
    const summaryQuota = document.getElementById('nutrition-quota');
    const summaryWeight = document.getElementById('nutrition-home-weight');
    const weightHeadline = document.getElementById('nutrition-weight-headline');
    const weightNow = document.getElementById('nutrition-weight-now');
    const weightTarget = document.getElementById('nutrition-weight-target');
    const weightTrend = document.getElementById('nutrition-weight-trend');
    const weightEmpty = document.getElementById('nutrition-weight-empty');
    const weighInForm = document.getElementById('nutrition-weighin-form');
    const chart = document.getElementById('nutrition-chart');
    let started = false;

    const CHART = { width: 640, height: 180, padding: 14 };

    const MONTHS = [
        'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
        'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
    ];

    const MACROS = [
        { key: 'protein', label: 'Proteína' },
        { key: 'carbs', label: 'Carbos' },
        { key: 'fat', label: 'Grasa' },
    ];

    async function read(path) {
        const response = await fetch(path);
        if (!response.ok) throw new Error('No se pudo leer ' + path);
        return response.json();
    }

    /** El plan puede no existir todavia: un 404 aqui es un estado normal, no un fallo. */
    async function readOptional(path) {
        const response = await fetch(path);
        if (response.status === 404) return null;
        if (!response.ok) throw new Error('No se pudo leer ' + path);
        return response.json();
    }

    async function write(method, path, body) {
        const response = await fetch(path, {
            method,
            headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(errorMessage({ status: response.status, body: payload }));
        }
        await refresh();
    }

    /** El peso se teclea en kilos y viaja como cadena: "78,4" y "78.4" valen igual. */
    function weight(value) {
        return value.trim().replace(',', '.');
    }

    function grams(value) {
        return Number(value) || 0;
    }

    function kilos(value) {
        return Number(value).toLocaleString('es-ES', { maximumFractionDigits: 1 }) + ' kg';
    }

    function signedKilos(value) {
        const number = Number(value);
        return (number > 0 ? '+' : '') + kilos(number);
    }

    function kcal(value) {
        return Number(value).toLocaleString('es-ES') + ' kcal';
    }

    function dayText(day) {
        const date = new Date(day + 'T00:00:00');
        return date.getDate() + ' ' + MONTHS[date.getMonth()].slice(0, 3);
    }

    function longDayText(day) {
        const date = new Date(day + 'T00:00:00');
        return date.getDate() + ' de ' + MONTHS[date.getMonth()];
    }

    function macroRow(day, macro) {
        const item = document.createElement('li');
        item.className = 'nutrition-macro';

        const head = document.createElement('div');
        head.className = 'nutrition-macro-head';

        const name = document.createElement('span');
        name.className = 'nutrition-macro-name';
        name.textContent = macro.label;

        const amount = document.createElement('span');
        amount.className = 'nutrition-macro-amount';
        amount.textContent = day.target
            ? day.consumed[macro.key] + ' / ' + day.target[macro.key] + ' g'
            : day.consumed[macro.key] + ' g';

        head.append(name, amount);

        const bar = document.createElement('div');
        bar.className = 'nutrition-bar';
        const fill = document.createElement('span');
        const share = day.target && day.target[macro.key] > 0
            ? 100 * day.consumed[macro.key] / day.target[macro.key]
            : 0;
        fill.style.width = Math.min(100, share) + '%';
        if (share > 100) bar.dataset.status = 'EXCEEDED';
        bar.append(fill);

        item.append(head, bar);
        return item;
    }

    function removeButton(label, onClick) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'nutrition-remove';
        button.title = label;
        button.setAttribute('aria-label', label);
        button.textContent = '×';
        button.addEventListener('click', () => guard(onClick));

        return button;
    }

    function intakeRow(intake) {
        const item = document.createElement('li');
        item.className = 'nutrition-intake';

        const what = document.createElement('span');
        what.className = 'nutrition-intake-what';
        what.textContent = intake.note || 'Sin nota';

        const detail = document.createElement('span');
        detail.className = 'nutrition-intake-detail';
        detail.textContent = MACROS.map(macro => intake.macros[macro.key] + ' g').join(' · ');

        const calories = document.createElement('span');
        calories.className = 'nutrition-intake-calories';
        calories.textContent = kcal(intake.macros.calories);

        item.append(what, detail, calories, removeButton(
            'Borrar ' + (intake.note || 'el consumo'),
            () => write('DELETE', '/nutrition/intakes/' + intake.id)));

        return item;
    }

    function dayRow(day) {
        const item = document.createElement('li');
        item.className = day.overBudget ? 'nutrition-day over' : 'nutrition-day';

        const when = document.createElement('span');
        when.className = 'nutrition-day-when';
        when.textContent = dayText(day.date);

        const calories = document.createElement('span');
        calories.className = 'nutrition-day-calories';
        calories.textContent = kcal(day.consumed.calories);

        const bar = document.createElement('div');
        bar.className = 'nutrition-bar';
        const fill = document.createElement('span');
        fill.style.width = Math.min(100, day.caloriePercentage) + '%';
        if (day.overBudget) bar.dataset.status = 'EXCEEDED';
        bar.append(fill);

        item.append(when, calories, bar);
        return item;
    }

    function replace(list, rows) {
        list.replaceChildren(...rows);
    }

    function svg(name, attributes) {
        const node = document.createElementNS('http://www.w3.org/2000/svg', name);
        for (const [key, value] of Object.entries(attributes)) node.setAttribute(key, value);
        return node;
    }

    /**
     * La serie se dibuja a mano en SVG. La escala vertical la fijan la propia serie y las dos
     * lineas de referencia, para que partida y objetivo siempre queden dentro del cuadro.
     */
    function renderChart(series, progress) {
        chart.replaceChildren();
        if (!progress || series.length === 0) return;

        const start = Number(progress.startWeight);
        const target = Number(progress.targetWeight);
        const values = series.map(reading => Number(reading.weight));
        const low = Math.min(start, target, ...values);
        const high = Math.max(start, target, ...values);
        const span = high - low || 1;

        const { width, height, padding } = CHART;
        const usable = height - 2 * padding;
        const y = value => padding + usable * (high - value) / span;
        const x = index => series.length === 1
            ? width / 2
            : padding + (width - 2 * padding) * index / (series.length - 1);

        const canvas = svg('svg', {
            viewBox: `0 0 ${width} ${height}`,
            preserveAspectRatio: 'none',
            role: 'img',
            'aria-label': `Evolucion del peso, de ${kilos(values[0])} a ${kilos(values.at(-1))}`,
        });

        canvas.append(
            svg('line', { class: 'nutrition-chart-start', x1: 0, x2: width, y1: y(start), y2: y(start) }),
            svg('line', { class: 'nutrition-chart-target', x1: 0, x2: width, y1: y(target), y2: y(target) }),
            svg('polyline', {
                class: 'nutrition-chart-line',
                points: values.map((value, index) => `${x(index)},${y(value)}`).join(' '),
            }),
            svg('circle', {
                class: 'nutrition-chart-last',
                cx: x(values.length - 1), cy: y(values.at(-1)), r: 3.5,
            }));

        chart.append(canvas);
    }

    function renderProgress(progress, series) {
        weightEmpty.hidden = Boolean(progress);
        renderChart(series, progress);

        if (!progress) {
            weightHeadline.textContent = '—';
            weightNow.textContent = '—';
            weightTarget.textContent = '—';
            weightTrend.textContent = '';
            return;
        }

        weightHeadline.textContent = progress.reached
            ? 'Objetivo alcanzado'
            : kilos(Math.abs(progress.remaining));
        weightHeadline.classList.toggle('reached', progress.reached);
        weightNow.textContent = kilos(progress.currentWeight);
        weightTarget.textContent = kilos(progress.targetWeight);
        weightTrend.textContent = Number(progress.trendPerWeek) === 0
            ? ''
            : signedKilos(progress.trendPerWeek) + ' por semana';
    }

    function renderDay(day) {
        dateLabel.textContent = longDayText(day.date);
        consumedLabel.textContent = kcal(day.consumed.calories);
        noPlan.hidden = Boolean(day.target);

        if (day.target) {
            headline.textContent = kcal(Math.abs(day.remaining.calories));
            headline.classList.toggle('negative', day.overBudget);
            targetLabel.textContent = kcal(day.target.calories);
        } else {
            headline.textContent = kcal(day.consumed.calories);
            headline.classList.remove('negative');
            targetLabel.textContent = '—';
        }

        replace(macroList, MACROS.map(macro => macroRow(day, macro)));
        replace(intakeList, day.intakes.map(intakeRow));
        intakesEmpty.hidden = day.intakes.length > 0;
    }

    function renderPlan(plan) {
        planSummary.textContent = plan
            ? plan.goalLabel + ': ' + plan.startWeight + ' → ' + plan.targetWeight
                + ' kg · ' + kcal(plan.dailyMacros.calories) + ' al día'
            : 'Sin plan.';
    }

    async function refreshDay() {
        renderDay(await read('/nutrition/today'));
    }

    async function refreshPlan() {
        renderPlan(await readOptional('/nutrition/plan'));
    }

    async function refreshWeight() {
        const [progress, series] = await Promise.all([
            readOptional('/nutrition/progress'),
            read('/nutrition/weigh-ins'),
        ]);
        renderProgress(progress, series);
    }

    async function refreshDays() {
        const days = await read('/nutrition/days');
        replace(dayList, days.map(dayRow));
        daysEmpty.hidden = days.length > 0;
    }

    async function refresh() {
        await Promise.all([refreshDay(), refreshPlan(), refreshDays(), refreshWeight()]);
    }

    async function refreshSummary() {
        const day = await read('/nutrition/today');
        if (summaryEaten) summaryEaten.textContent = kcal(day.consumed.calories);
        if (summaryQuota) summaryQuota.textContent = day.target ? kcal(day.target.calories) : '—';
        if (summaryRemaining) {
            summaryRemaining.textContent = day.target
                ? kcal(Math.abs(day.remaining.calories))
                : kcal(day.consumed.calories);
            summaryRemaining.classList.toggle('negative', Boolean(day.overBudget));
        }
        if (summaryGoal) {
            summaryGoal.textContent = day.target
                ? (day.overBudget ? 'De más hoy' : 'Te quedan hoy')
                : 'Sin plan';
        }
        if (summaryWeight) {
            const progress = await readOptional('/nutrition/progress');
            summaryWeight.textContent = progress
                ? (progress.reached
                    ? 'Peso objetivo alcanzado'
                    : kilos(Math.abs(progress.remaining)) + ' para el objetivo')
                : '';
        }
    }

    function guard(work) {
        return work().catch(() => {});
    }

    function onSubmit(form, save) {
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const error = form.querySelector('.nutrition-form-error');
            error.textContent = '';
            try {
                await save(form.elements);
                form.reset();
                renderLiveCalories();
            } catch (failure) {
                error.textContent = failure.message;
            }
        });
    }

    function renderLiveCalories() {
        const fields = intakeForm.elements;
        liveCalories.textContent = 4 * grams(fields.protein.value)
            + 4 * grams(fields.carbs.value)
            + 9 * grams(fields.fat.value);
    }

    intakeForm.addEventListener('input', renderLiveCalories);

    onSubmit(intakeForm, fields => write('POST', '/nutrition/intakes', {
        protein: grams(fields.protein.value),
        carbs: grams(fields.carbs.value),
        fat: grams(fields.fat.value),
        note: fields.note.value.trim(),
    }));

    // Fijar el plan dos veces es redefinirlo: el comando archiva el anterior por su cuenta.
    onSubmit(planForm, fields => write('POST', '/nutrition/plan', {
        startWeight: weight(fields.startWeight.value),
        targetWeight: weight(fields.targetWeight.value),
        protein: grams(fields.protein.value),
        carbs: grams(fields.carbs.value),
        fat: grams(fields.fat.value),
    }));

    onSubmit(weighInForm, fields => write('POST', '/nutrition/weigh-ins', {
        weight: weight(fields.weight.value),
    }));

    function listen() {
        if (started) return;
        started = true;

        const events = new EventSource('/events/nutrition');
        const reload = () => {
            guard(refresh);
            guard(refreshSummary);
        };
        ['planDefined', 'planAdjusted', 'planArchived',
            'intakeRecorded', 'intakeCorrected', 'intakeDeleted',
            'weighInRecorded', 'weighInCorrected', 'weighInDeleted']
            .forEach(name => events.addEventListener(name, reload));
    }

    window.AtlasNutrition = {
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
