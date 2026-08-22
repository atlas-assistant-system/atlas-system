(() => {
    const dateLabel = document.getElementById('nutrition-date');
    const headline = document.getElementById('nutrition-headline');
    const weekStrip = document.getElementById('nutrition-week');
    const gauge = document.getElementById('nutrition-gauge');
    const macroList = document.getElementById('nutrition-macros');
    const noPlan = document.getElementById('nutrition-no-plan');
    const intakeList = document.getElementById('nutrition-intakes');
    const intakesEmpty = document.getElementById('nutrition-intakes-empty');
    const intakesLabel = document.getElementById('nutrition-intakes-label');
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
    let started = false;
    let selected = null;

    const GAUGE = {
        width: 320, height: 96, from: [16, 78], peak: [160, 10], to: [304, 78],
        headroom: 1.18,
    };
    const WEEKDAYS = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

    const MONTHS = [
        'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
        'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
    ];

    const MACROS = [
        { key: 'protein', label: 'Proteínas' },
        { key: 'carbs', label: 'Carbos' },
        { key: 'fat', label: 'Grasas' },
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

    function kcal(value) {
        return Number(value).toLocaleString('es-ES') + ' kcal';
    }

    function number(value) {
        return Number(value).toLocaleString('es-ES');
    }

    function isoOf(date) {
        return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
            .toISOString()
            .slice(0, 10);
    }

    function dateOf(iso) {
        return new Date(iso + 'T00:00:00');
    }

    /** La semana del espejo empieza en lunes, como la tira de la agenda. */
    function mondayOf(iso) {
        const date = dateOf(iso);
        date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
        return date;
    }

    function weekOf(iso) {
        const monday = mondayOf(iso);
        return WEEKDAYS.map((label, index) => {
            const day = new Date(monday);
            day.setDate(monday.getDate() + index);
            return { label, iso: isoOf(day), number: day.getDate() };
        });
    }

    function dayText(day) {
        const date = new Date(day + 'T00:00:00');
        return date.getDate() + ' ' + MONTHS[date.getMonth()].slice(0, 3);
    }

    function longDayText(day) {
        const date = new Date(day + 'T00:00:00');
        return date.getDate() + ' de ' + MONTHS[date.getMonth()];
    }

    function macroColumn(day, macro) {
        const item = document.createElement('li');
        item.className = 'nutrition-macro';

        const name = document.createElement('span');
        name.className = 'nutrition-macro-name';
        name.textContent = macro.label;

        const amount = document.createElement('span');
        amount.className = 'nutrition-macro-amount';
        amount.textContent = day.targetMacros
            ? day.consumedMacros[macro.key] + ' / ' + day.targetMacros[macro.key] + ' g'
            : day.consumedMacros[macro.key] + ' g';

        const share = day.targetMacros && day.targetMacros[macro.key] > 0
            ? 100 * day.consumedMacros[macro.key] / day.targetMacros[macro.key]
            : 0;

        const bar = document.createElement('div');
        bar.className = 'nutrition-bar';
        const fill = document.createElement('span');
        fill.style.width = Math.min(100, share) + '%';
        if (share > 100) bar.dataset.status = 'EXCEEDED';
        bar.append(fill);

        item.append(name, amount, bar);
        return item;
    }

    function weekDot(summary, iso, today) {
        const dot = document.createElement('span');
        dot.className = 'nutrition-week-dot';
        if (!summary) {
            dot.dataset.state = iso > today ? 'FUTURE' : 'EMPTY';
        } else if (summary.withinRange) {
            dot.dataset.state = 'WITHIN';
        } else {
            dot.dataset.state = summary.overBudget ? 'OVER' : 'UNDER';
        }
        return dot;
    }

    function weekCell(day, byDate, today) {
        const item = document.createElement('li');
        item.className = 'nutrition-week-day';

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'nutrition-week-button';
        button.setAttribute('aria-label', longDayText(day.iso));
        if (day.iso === selected) button.dataset.selected = 'true';
        if (day.iso === today) button.dataset.today = 'true';
        if (day.iso > today) button.disabled = true;
        button.addEventListener('click', () => guard(() => show(day.iso)));

        const label = document.createElement('span');
        label.className = 'nutrition-week-label';
        label.textContent = day.label;

        const number = document.createElement('span');
        number.className = 'nutrition-week-number';
        number.textContent = day.number;

        button.append(label, number, weekDot(byDate.get(day.iso), day.iso, today));
        item.append(button);
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
        calories.textContent = kcal(intake.calories);

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
        calories.textContent = kcal(day.consumedCalories);

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
     * El arco va de cero al limite superior del rango, con dos marcas en el rango mismo. La
     * escala la fija el limite y no lo consumido, para que pasarse se vea como pasarse.
     */
    function renderGauge(day) {
        gauge.replaceChildren();
        if (day.targetCalories === null) return;

        const { width, height, from, peak, to } = GAUGE;
        const canvas = svg('svg', {
            viewBox: `0 0 ${width} ${height}`,
            role: 'img',
            'aria-label': `${number(day.consumedCalories)} de ${number(day.targetCalories)} kcal`,
        });
        const shape = `M ${from[0]} ${from[1]} Q ${peak[0]} ${peak[1]} ${to[0]} ${to[1]}`;

        const track = svg('path', { class: 'nutrition-gauge-track', d: shape });
        const progress = svg('path', { class: 'nutrition-gauge-progress', d: shape });
        if (day.overBudget) progress.dataset.status = 'EXCEEDED';
        canvas.append(track, progress);
        gauge.append(canvas);

        const scale = day.upperCalories * GAUGE.headroom;
        const share = value => Math.max(0, Math.min(1, value / scale));
        const length = track.getTotalLength();
        progress.style.strokeDasharray = length;
        progress.style.strokeDashoffset = length * (1 - share(day.consumedCalories));

        for (const bound of [day.lowerCalories, day.upperCalories]) {
            const at = track.getPointAtLength(length * share(bound));
            canvas.append(
                svg('line', {
                    class: 'nutrition-gauge-tick',
                    x1: at.x, x2: at.x, y1: at.y - 5, y2: at.y + 5,
                }),
                text(at.x, GAUGE.from[1] + 22, number(bound)));
        }
    }

    function text(x, y, content) {
        const node = svg('text', { class: 'nutrition-gauge-label', x, y, 'text-anchor': 'middle' });
        node.textContent = content;
        return node;
    }

    function renderDay(day) {
        selected = day.date;
        dateLabel.textContent = longDayText(day.date);
        intakesLabel.textContent = day.date === isoOf(new Date()) ? 'Hoy' : longDayText(day.date);
        noPlan.hidden = day.targetCalories !== null;

        headline.textContent = day.targetCalories === null
            ? number(day.consumedCalories)
            : number(day.consumedCalories) + ' / ' + number(day.targetCalories);
        headline.dataset.status = day.overBudget ? 'EXCEEDED' : '';

        renderGauge(day);
        replace(macroList, MACROS.map(macro => macroColumn(day, macro)));
        replace(intakeList, day.intakes.map(intakeRow));
        intakesEmpty.hidden = day.intakes.length > 0;
    }

    async function renderWeek() {
        const today = isoOf(new Date());
        const week = weekOf(selected || today);
        const summaries = await read(
            '/nutrition/days?from=' + week[0].iso + '&to=' + week[6].iso);
        const byDate = new Map(summaries.map(summary => [summary.date, summary]));

        replace(weekStrip, week.map(day => weekCell(day, byDate, today)));
    }

    async function show(iso) {
        selected = iso;
        await refresh();
    }

    function renderPlan(plan) {
        planSummary.textContent = plan
            ? plan.goalLabel + ': ' + plan.startWeight + ' → ' + plan.targetWeight
                + ' kg · ' + kcal(plan.dailyCalories) + ' al día'
            : 'Sin plan.';
    }

    async function refreshDay() {
        const today = isoOf(new Date());
        renderDay(await read(
            !selected || selected === today ? '/nutrition/today' : '/nutrition/days/' + selected));
    }

    async function refreshPlan() {
        renderPlan(await readOptional('/nutrition/plan'));
    }

    async function refreshDays() {
        const days = await read('/nutrition/days');
        replace(dayList, days.map(dayRow));
        daysEmpty.hidden = days.length > 0;
    }

    async function refresh() {
        await refreshDay();
        await Promise.all([renderWeek(), refreshPlan(), refreshDays()]);
    }

    async function refreshSummary() {
        const day = await read('/nutrition/today');
        if (summaryEaten) summaryEaten.textContent = kcal(day.consumedCalories);
        if (summaryQuota) {
            summaryQuota.textContent = day.targetCalories === null ? '—' : kcal(day.targetCalories);
        }
        if (summaryRemaining) {
            summaryRemaining.textContent = day.targetCalories === null
                ? kcal(day.consumedCalories)
                : kcal(Math.abs(day.remainingCalories));
            summaryRemaining.classList.toggle('negative', Boolean(day.overBudget));
        }
        if (summaryGoal) {
            summaryGoal.textContent = day.targetCalories === null
                ? 'Sin plan'
                : (day.overBudget ? 'De más hoy' : 'Te quedan hoy');
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
        calories: grams(fields.calories.value),
        protein: grams(fields.protein.value),
        carbs: grams(fields.carbs.value),
        fat: grams(fields.fat.value),
        note: fields.note.value.trim(),
    }));

    // Fijar el plan dos veces es redefinirlo: el comando archiva el anterior por su cuenta.
    onSubmit(planForm, fields => write('POST', '/nutrition/plan', {
        startWeight: weight(fields.startWeight.value),
        targetWeight: weight(fields.targetWeight.value),
        calories: grams(fields.calories.value),
        protein: grams(fields.protein.value),
        carbs: grams(fields.carbs.value),
        fat: grams(fields.fat.value),
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
            'intakeRecorded', 'intakeCorrected', 'intakeDeleted']
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
