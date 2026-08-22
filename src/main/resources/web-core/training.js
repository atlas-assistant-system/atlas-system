(() => {
    const headline = document.getElementById('training-headline');
    const subhead = document.getElementById('training-subhead');
    const setList = document.getElementById('training-sets');
    const setsEmpty = document.getElementById('training-sets-empty');
    const startBar = document.getElementById('training-start');
    const exerciseList = document.getElementById('training-exercises');
    const workoutList = document.getElementById('training-workouts');
    const exerciseForm = document.getElementById('training-exercise-form');
    const setForm = document.getElementById('training-set-form');
    const setFormExercise = document.getElementById('training-set-exercise');
    let started = false;

    /** Solo un entreno abierto a la vez en pantalla; si hay dos hoy, se ve el ultimo. */
    let current = null;
    let exercises = new Map();

    const METRICS = [
        { key: 'LOAD', label: 'Carga' },
        { key: 'REPS', label: 'Repeticiones' },
        { key: 'TIME', label: 'Tiempo' },
        { key: 'DISTANCE', label: 'Distancia' },
    ];

    async function read(path) {
        const response = await fetch(path);
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

    /** El peso se teclea en kilos: "72,5" y "72.5" valen igual. */
    function load(value) {
        return (value || '').trim().replace(',', '.') || '0';
    }

    function count(value) {
        return Number(value) || 0;
    }

    function number(value) {
        return Number(value).toLocaleString('es-ES');
    }

    /**
     * Lo que se lee de una serie depende de lo que mida el ejercicio: en carga son kilos y
     * reps, en tiempo segundos. Es la misma decision que toma Metric en el dominio, aqui
     * solo para escribirla.
     */
    function effortText(effort, metric) {
        if (!effort) return '—';

        if (metric === 'TIME') return effort.seconds + ' s';
        if (metric === 'DISTANCE') return number(effort.meters) + ' m';
        if (metric === 'REPS' || Number(effort.load) === 0) return effort.reps + ' reps';

        return effort.load + ' kg × ' + effort.reps;
    }

    function metricOf(exerciseId) {
        return exercises.get(exerciseId)?.metric || 'LOAD';
    }

    function nameOf(exerciseId) {
        return exercises.get(exerciseId)?.name || 'Ejercicio';
    }

    function button(label, className, onClick) {
        const node = document.createElement('button');
        node.type = 'button';
        node.className = className;
        node.title = label;
        node.setAttribute('aria-label', label);
        node.addEventListener('click', () => guard(onClick));
        return node;
    }

    function removeButton(label, onClick) {
        const node = button(label, 'training-remove', onClick);
        node.textContent = '×';
        return node;
    }

    function setRow(set, index, isNext) {
        const item = document.createElement('li');
        item.className = 'training-set';
        item.dataset.state = set.actual ? 'DONE' : 'PENDING';
        if (isNext) item.dataset.next = 'true';

        const position = document.createElement('span');
        position.className = 'training-set-index';
        position.textContent = index + 1;

        const effort = document.createElement('span');
        effort.className = 'training-set-effort';
        effort.textContent = effortText(set.actual || set.planned, metricOf(set.exerciseId));

        const planned = document.createElement('span');
        planned.className = 'training-set-planned';
        planned.textContent = set.actual && set.planned
            ? 'plan ' + effortText(set.planned, metricOf(set.exerciseId))
            : '';

        const actions = document.createElement('span');
        actions.className = 'training-set-actions';
        actions.append(removeButton('Quitar la serie', () => write(
            'DELETE', '/training/logs/' + current.id + '/sets/' + set.id)));

        item.append(position, effort, planned, actions);
        item.addEventListener('click', event => {
            if (event.target.closest('.training-remove')) return;
            fillSetForm(set);
        });

        return item;
    }

    /** Al tocar una serie, el formulario se prepara para ella: rellenar es el gesto normal. */
    function fillSetForm(set) {
        const fields = setForm.elements;
        const source = set.actual || set.planned;
        setForm.dataset.setId = set.id;
        setFormExercise.value = set.exerciseId;
        fields.load.value = source ? source.load : '';
        fields.reps.value = source ? source.reps : '';
        fields.seconds.value = source ? source.seconds : '';
        fields.meters.value = source ? source.meters : '';
        fields.load.focus();
    }

    function renderSets(log) {
        const rows = [];
        let lastExercise = null;
        const nextPending = log.sets.find(set => !set.actual);

        log.sets.forEach((set, index) => {
            if (set.exerciseId !== lastExercise) {
                const heading = document.createElement('p');
                heading.className = 'training-exercise-name';
                heading.textContent = nameOf(set.exerciseId);
                rows.push(heading);
                lastExercise = set.exerciseId;
            }

            rows.push(setRow(set, index, nextPending && set.id === nextPending.id));
        });

        setList.replaceChildren(...rows);
        setsEmpty.hidden = log.sets.length > 0;
    }

    function renderToday(logs) {
        current = logs.length > 0 ? logs[logs.length - 1] : null;
        startBar.hidden = current !== null;
        setForm.hidden = current === null;

        if (!current) {
            headline.textContent = 'Sin entreno hoy';
            subhead.textContent = 'Elige una rutina o empieza uno libre.';
            setList.replaceChildren();
            setsEmpty.hidden = true;
            return;
        }

        const done = current.sets.filter(set => set.actual).length;
        headline.textContent = done + ' / ' + current.sets.length;
        subhead.textContent = current.sets.length === 0
            ? 'Entreno libre: anade las series segun las hagas.'
            : 'series hechas';

        renderSets(current);
    }

    function renderStartBar(workouts) {
        const buttons = workouts.map(workout => {
            const node = document.createElement('button');
            node.type = 'button';
            node.textContent = workout.name;
            node.addEventListener('click', () => guard(
                () => write('POST', '/training/logs', { workoutId: workout.id })));
            return node;
        });

        const free = document.createElement('button');
        free.type = 'button';
        free.textContent = 'Entreno libre';
        free.addEventListener('click', () => guard(() => write('POST', '/training/logs', {})));

        startBar.replaceChildren(...buttons, free);
    }

    function renderExercises(list) {
        exercises = new Map(list.map(exercise => [exercise.id, exercise]));

        exerciseList.replaceChildren(...list.map(exercise => {
            const item = document.createElement('li');
            item.className = 'training-item';

            const name = document.createElement('span');
            name.className = 'training-item-name';
            name.textContent = exercise.name;

            const metric = document.createElement('span');
            metric.className = 'training-item-detail';
            metric.textContent = exercise.metricLabel;

            item.append(name, metric, removeButton(
                'Archivar ' + exercise.name,
                () => write('DELETE', '/training/exercises/' + exercise.id)));

            return item;
        }));

        setFormExercise.replaceChildren(...list.map(exercise => {
            const option = document.createElement('option');
            option.value = exercise.id;
            option.textContent = exercise.name;
            return option;
        }));
    }

    function renderWorkouts(workouts) {
        workoutList.replaceChildren(...workouts.map(workout => {
            const item = document.createElement('li');
            item.className = 'training-item';

            const name = document.createElement('span');
            name.className = 'training-item-name';
            name.textContent = workout.name;

            const size = document.createElement('span');
            size.className = 'training-item-detail';
            size.textContent = workout.plan.length + ' ejercicios';

            item.append(name, size, removeButton(
                'Archivar ' + workout.name,
                () => write('DELETE', '/training/workouts/' + workout.id)));

            return item;
        }));
    }

    async function refresh() {
        const [list, workouts] = await Promise.all([
            read('/training/exercises'),
            read('/training/workouts'),
        ]);

        renderExercises(list);
        renderWorkouts(workouts);
        renderStartBar(workouts);
        renderToday(await read('/training/today'));
    }

    function guard(work) {
        return work().catch(() => {});
    }

    function onSubmit(form, save) {
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const error = form.querySelector('.training-form-error');
            error.textContent = '';
            try {
                await save(form.elements);
                form.reset();
            } catch (failure) {
                error.textContent = failure.message;
            }
        });
    }

    onSubmit(exerciseForm, fields => write('POST', '/training/exercises', {
        name: fields.name.value.trim(),
        metric: fields.metric.value,
    }));

    /*
     * Una serie del guion se rellena (PUT sobre su id); una fuera del guion se anade (POST).
     * Es la misma pantalla porque para quien entrena es el mismo gesto.
     */
    onSubmit(setForm, fields => {
        const effort = {
            load: load(fields.load.value),
            reps: count(fields.reps.value),
            seconds: count(fields.seconds.value),
            meters: count(fields.meters.value),
        };
        const setId = setForm.dataset.setId;
        setForm.dataset.setId = '';

        return setId
            ? write('PUT', '/training/logs/' + current.id + '/sets/' + setId, effort)
            : write('POST', '/training/logs/' + current.id + '/sets',
                { ...effort, exerciseId: fields.exerciseId.value });
    });

    function listen() {
        if (started) return;
        started = true;

        const events = new EventSource('/events/training');
        const reload = () => guard(refresh);
        ['exerciseDefined', 'exerciseRenamed', 'exerciseArchived', 'exerciseUnarchived',
            'workoutDefined', 'workoutRenamed', 'workoutPlanChanged', 'workoutArchived',
            'workoutStarted', 'setRecorded', 'setRemoved', 'workoutDiscarded']
            .forEach(name => events.addEventListener(name, reload));
    }

    for (const metric of METRICS) {
        const option = document.createElement('option');
        option.value = metric.key;
        option.textContent = metric.label;
        exerciseForm.elements.metric.append(option);
    }

    window.AtlasTraining = {
        activate() {
            guard(refresh);
            listen();
        },
    };
})();
