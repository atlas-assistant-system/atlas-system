const state = {
    view: 'inicio',
    period: 'MONTH',
    anchor: todayIso(),
    previousAnchor: null,
    nextAnchor: null,
    next: null,
    openId: null,
    authenticated: false,
    authentication: null,
    authBusy: false,
    cameraReady: false,
    human: null,
    recognition: null,
    gesture: { candidate: null, frames: 0, missing: 0, latched: null },
    pointer: { target: null },
    voice: { recognition: null, target: null, listening: false },
};

let eventSource = null;

const DURATIONS = [30, 60, 90, 120];
const LEAD_TIMES = [10, 30, 60, 1440];
const MONTH_INITIALS = ['E', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D'];
const MODEL_VERSION = 'human-faceres-3.3.6';
const MIN_CONFIDENCE = 0.6;
const INTERACTIVE = 'button:not(:disabled), input:not(:disabled), textarea:not(:disabled), '
    + 'select:not(:disabled), .clickable, .day, .item';
// ponytail: pinned CDN keeps face recognition out of the Agenda build; self-host it if offline use is required.
const HUMAN_MODELS = 'https://cdn.jsdelivr.net/npm/@vladmandic/human@3.3.6/models/';

function todayIso() {
    return isoDate(new Date());
}

function isoDate(date) {
    return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate());
}

function isoDateTime(date) {
    return isoDate(date) + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
}

function pad(value) {
    return String(value).padStart(2, '0');
}

function capitalize(text) {
    return text.charAt(0).toUpperCase() + text.slice(1);
}

function formatLeadTime(minutes) {
    if (minutes % 1440 === 0) {
        return (minutes / 1440) + ' d';
    }
    if (minutes % 60 === 0) {
        return (minutes / 60) + ' h';
    }

    return minutes + ' min';
}

const ERROR_MESSAGES = {
    'Appointment.NotFound': 'Esa cita ya no existe.',
    'Appointment.ReminderNotFound': 'Ese aviso ya no existe.',
    'Appointment.TitleRequired': 'Hace falta un título.',
    'Appointment.TitleTooLong': 'El título es demasiado largo.',
    'Appointment.DescriptionTooLong': 'La descripción es demasiado larga.',
    'Appointment.TimeSlotRequired': 'Hace falta una franja horaria.',
    'Appointment.InvalidTimeSlot': 'La hora de fin tiene que ir después de la de inicio.',
    'Appointment.CannotScheduleInThePast': 'No puedes agendar algo en el pasado.',
    'Appointment.InvalidReminderLeadTime': 'Esa antelación no es válida.',
    'Appointment.Overlaps': 'Esa franja ya está ocupada por otra cita.',
    'Appointment.NotCancelled': 'Esta cita no está cancelada.',
    'Appointment.CannotRestorePastAppointment': 'Su hora ya pasó: reprográmala en vez de recuperarla.',
    'Appointment.CannotModifyCancelled': 'Una cita cancelada no se puede modificar.',
    'Appointment.CannotModifyPastAppointment': 'Una cita que ya pasó no se puede modificar.',
    'Appointment.CannotAddReminderToPastAppointment': 'No puedes avisar de algo que ya pasó.',
    'Appointment.DuplicateReminderLeadTime': 'Ya tienes un aviso con esa antelación.',
    'Appointment.TooManyReminders': 'Una cita admite como mucho 5 avisos.',
};

function errorMessage(response) {
    const code = response.body && response.body.code;

    return ERROR_MESSAGES[code] || 'No se pudo completar la operación.';
}

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    if (text !== undefined) {
        node.textContent = text;
    }

    return node;
}

function button(className, text, onClick) {
    const node = el('button', className, text);
    node.type = 'button';
    node.addEventListener('click', onClick);

    return node;
}

async function api(path, options) {
    const response = await fetch(path, options);
    const body = response.status === 204 ? null : await response.json().catch(() => null);

    return { status: response.status, body };
}

function send(method, path, payload) {
    return api(path, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
}

async function startCamera() {
    if (!window.Human || !window.Human.Human || !navigator.mediaDevices?.getUserMedia) {
        setAuthFeedback('No se pudo cargar el reconocimiento facial. Comprueba la conexión y recarga.', true);
        return;
    }

    state.authBusy = true;
    updatePresenceActions();
    try {
        const mirror = document.getElementById('mirror');
        mirror.srcObject = await navigator.mediaDevices.getUserMedia({
            audio: false,
            video: { facingMode: 'user', width: { ideal: 960 }, height: { ideal: 720 } },
        });
        await mirror.play();
        document.getElementById('access-message').textContent = 'Cargando modelos faciales…';
        state.human = new window.Human.Human({
            backend: 'webgl',
            cacheSensitivity: 0.01,
            debug: false,
            modelBasePath: HUMAN_MODELS,
            filter: { enabled: true, equalization: true },
            face: {
                enabled: true,
                detector: { rotation: true, return: false, maxDetected: 1 },
                mesh: { enabled: true },
                iris: { enabled: true },
                description: { enabled: true },
                emotion: { enabled: true },
                antispoof: { enabled: true },
                liveness: { enabled: true },
            },
            body: { enabled: false },
            hand: { enabled: true, maxDetected: 1, landmarks: true },
            object: { enabled: false },
            segmentation: { enabled: false },
            gesture: { enabled: true },
        });
        await state.human.load();
        await state.human.warmup();
        state.cameraReady = true;
        detectFaces();
    } catch (error) {
        setAuthFeedback(error.message || 'Autoriza el uso de la cámara para continuar.', true);
    } finally {
        state.authBusy = false;
        updatePresenceActions();
        renderAuthenticationStatus();
    }
}

async function detectFaces() {
    if (!state.cameraReady) {
        return;
    }
    try {
        state.recognition = await state.human.detect(document.getElementById('mirror'));
        renderAuthenticationStatus();
        trackHandGesture(state.recognition);
    } catch (_) {
        document.getElementById('access-message').textContent = 'No se pudo procesar la imagen.';
    }
    setTimeout(detectFaces, 80);
}

function faceStatus(result) {
    if (!result || result.face.length !== 1) {
        return { ready: false, reason: result && result.face.length > 1
            ? 'Debe aparecer una sola persona.' : 'Mira hacia la cámara.' };
    }

    const face = result.face[0];
    const confidence = face.faceScore || face.boxScore || 0;
    if (confidence < MIN_CONFIDENCE || Math.min(face.box[2], face.box[3]) < 160) {
        return { ready: false, reason: 'Acércate un poco y mira hacia la cámara.' };
    }
    if (!face.embedding || face.embedding.length === 0) {
        return { ready: false, reason: 'Calculando la firma facial…' };
    }
    if ((face.real || 0) < MIN_CONFIDENCE || (face.live || 0) < MIN_CONFIDENCE) {
        return { ready: false, reason: 'No se ha podido confirmar que sea un rostro real.' };
    }

    return { ready: true, face, reason: 'Rostro preparado.' };
}

function trackHandGesture(result) {
    const observed = recognizedHandGesture(result);
    trackPointer(result, observed);
    const tracking = state.gesture;
    if (!observed) {
        tracking.missing++;
        if (tracking.missing >= 3) {
            tracking.candidate = null;
            tracking.frames = 0;
            tracking.latched = null;
        }
        return;
    }

    tracking.missing = 0;
    if (tracking.candidate !== observed.type) {
        tracking.candidate = observed.type;
        tracking.frames = 1;
        return;
    }
    tracking.frames++;
    if (tracking.frames < 3 || tracking.latched === observed.type) {
        return;
    }

    tracking.latched = observed.type;
    publishGesture(observed);
}

function publishGesture(observed) {
    if (!state.authenticated) {
        if (observed.type === 'VICTORY' && !state.authBusy && state.cameraReady
            && state.authentication
            && state.authentication.enrolledProfiles > 0) {
            authenticate();
        } else if (observed.type === 'PINCH' || observed.type === 'POINT') {
            applyGesture(observed.type);
        }
        return;
    }

    send('POST', '/presence/interactions/gestures', {
        ...observed,
        observedAt: new Date().toISOString(),
    }).then(response => {
        if (observed.type === 'VICTORY' && response.status === 204) {
            setAuthenticated(false);
            refreshAuthentication();
        } else if (response.status === 204) {
            applyGesture(observed.type);
        } else if (response.status === 401) {
            refreshAuthentication();
        }
    }).catch(() => {});
}

function trackPointer(result, observed) {
    const hand = result && result.hand && result.hand[0];
    const index = hand && hand.annotations && hand.annotations.index;
    const point = index && index[index.length - 1];
    const mapped = point && cameraPoint(point);
    const pointer = document.getElementById('gesture-pointer');
    if (!mapped) {
        pointer.classList.remove('visible');
        pointer.classList.remove('recognized');
        pointer.dataset.gesture = 'TRACKING';
        setPointerTarget(null);
        return;
    }

    pointer.style.transform = `translate(${mapped.x}px, ${mapped.y}px)`;
    pointer.classList.add('visible');
    pointer.classList.toggle('recognized', Boolean(observed));
    pointer.dataset.gesture = observed ? observed.type.replace('PALM_', '') : 'TRACKING';
    const hit = document.elementFromPoint(mapped.x, mapped.y);
    setPointerTarget(hit && hit.closest(INTERACTIVE));
}

function cameraPoint(point) {
    const video = document.getElementById('mirror');
    if (!video.videoWidth || !video.videoHeight) {
        return null;
    }

    let x = Array.isArray(point) ? point[0] : point.x;
    let y = Array.isArray(point) ? point[1] : point.y;
    if (!Number.isFinite(x) || !Number.isFinite(y)) {
        return null;
    }
    if (x <= 1 && y <= 1) {
        x *= video.videoWidth;
        y *= video.videoHeight;
    }

    const scale = Math.max(innerWidth / video.videoWidth, innerHeight / video.videoHeight);
    const offsetX = (innerWidth - video.videoWidth * scale) / 2;
    const offsetY = (innerHeight - video.videoHeight * scale) / 2;

    return {
        x: Math.max(0, Math.min(innerWidth, innerWidth - (offsetX + x * scale))),
        y: Math.max(0, Math.min(innerHeight, offsetY + y * scale)),
    };
}

function setPointerTarget(target) {
    if (state.pointer.target === target) {
        return;
    }
    if (state.pointer.target) {
        state.pointer.target.classList.remove('gesture-target');
    }
    state.pointer.target = target;
    if (target) {
        target.classList.add('gesture-target');
    }
}

function recognizedHandGesture(result) {
    const hand = result && result.hand && result.hand[0];
    if (!hand || (hand.score || 0) < MIN_CONFIDENCE) {
        return null;
    }

    const signals = (result.gesture || [])
        .filter(value => value.hand === 0)
        .map(value => value.gesture);
    const fingersUp = signals.filter(value => /^(thumb|index|middle|ring|pinky) up$/.test(value));
    let type = null;
    if (hand.label === 'pinch' || hand.label === 'pinchtip') {
        type = 'PINCH';
    } else if (signals.includes('thumbs up')) {
        type = 'THUMBS_UP';
    } else if (signals.includes('victory')) {
        type = 'VICTORY';
    } else if (hand.label === 'point' || (fingersUp.length === 1 && fingersUp[0] === 'index up')) {
        type = 'POINT';
    } else if (hand.label === 'fist') {
        type = 'FIST';
    } else if (fingersUp.length >= 4) {
        type = staticPalmGesture(hand);
    }

    return type ? {
        type,
        handIndex: Number.isInteger(hand.id) ? hand.id : 0,
        confidence: Number((hand.score || hand.fingerScore || 0).toFixed(3)),
    } : null;
}

function staticPalmGesture(hand) {
    const box = hand.boxRaw;
    if (!box || box.length < 4) {
        return 'OPEN_PALM';
    }

    const dx = 1 - (box[0] + box[2] / 2) - 0.5;
    const dy = box[1] + box[3] / 2 - 0.5;
    const horizontal = Math.abs(dx);
    const vertical = Math.abs(dy);
    if (Math.max(horizontal, vertical) < 0.2) {
        return 'OPEN_PALM';
    }

    if (horizontal > vertical) {
        return dx > 0 ? 'PALM_RIGHT' : 'PALM_LEFT';
    }
    return dy > 0 ? 'PALM_DOWN' : 'PALM_UP';
}

function applyGesture(type) {
    if (type === 'POINT') {
        if (state.pointer.target && typeof state.pointer.target.focus === 'function') {
            state.pointer.target.focus({ preventScroll: true });
        }
        return;
    }
    if (type === 'PINCH') {
        activatePointerTarget();
        return;
    }
    if (type === 'PALM_LEFT') {
        switchView('agenda');
        return;
    }
    if (type === 'PALM_RIGHT') {
        switchView('inicio');
        return;
    }
    if (type === 'PALM_UP' || type === 'PALM_DOWN') {
        if (!adjustFocusedControl(type)) {
            scrollByGesture(type);
        }
        return;
    }
    if (type === 'FIST') {
        if (state.voice.listening) {
            stopVoiceInput();
        } else if (!document.getElementById('panel').hidden) {
            closePanel();
        } else if (state.view !== 'inicio') {
            switchView('inicio');
        }
        return;
    }
    if (type === 'THUMBS_UP') {
        const form = document.activeElement && document.activeElement.closest('form');
        if (form) {
            form.requestSubmit();
        }
    }
}

function activatePointerTarget() {
    const target = state.pointer.target;
    if (!target) {
        setInteractionStatus('No hay ningún control seleccionado.');
        return;
    }
    if (isTextField(target)) {
        target.focus();
        startVoiceInput(target);
        return;
    }
    target.focus({ preventScroll: true });
    target.click();
}

function isTextField(target) {
    return target instanceof HTMLTextAreaElement
        || target instanceof HTMLInputElement && ['text', 'search', 'email', 'tel', 'url'].includes(target.type);
}

function adjustFocusedControl(type) {
    const control = document.activeElement;
    const increment = type === 'PALM_UP' ? 1 : -1;
    if (control instanceof HTMLSelectElement) {
        control.selectedIndex = Math.max(0, Math.min(control.options.length - 1,
            control.selectedIndex + increment));
    } else if (control instanceof HTMLInputElement && ['date', 'time', 'number'].includes(control.type)) {
        increment > 0 ? control.stepUp() : control.stepDown();
    } else {
        return false;
    }
    control.dispatchEvent(new Event('change', { bubbles: true }));
    setInteractionStatus(control.value);
    return true;
}

function scrollByGesture(type) {
    let container = !document.getElementById('panel').hidden ? document.getElementById('panel') : state.pointer.target;
    while (container && container !== document.body
        && container.scrollHeight <= container.clientHeight) {
        container = container.parentElement;
    }
    if (!container || container === document.body) {
        container = document.querySelector(state.view === 'agenda' ? '#month-grid' : '#view-inicio');
    }
    const distance = Math.max(240, container.clientHeight * 0.7);
    container.scrollBy({ top: type === 'PALM_UP' ? distance : -distance, behavior: 'smooth' });
}

let interactionStatusHandle = null;

function setInteractionStatus(text) {
    const status = document.getElementById('interaction-status');
    status.textContent = text;
    status.classList.add('visible');
    clearTimeout(interactionStatusHandle);
    interactionStatusHandle = setTimeout(() => status.classList.remove('visible'), 2500);
}

function startVoiceInput(target) {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) {
        setInteractionStatus('El dictado no está disponible en este navegador.');
        return;
    }
    stopVoiceInput();

    const recognition = new Recognition();
    recognition.lang = 'es-ES';
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    state.voice = { recognition, target, listening: true };
    recognition.onstart = () => setInteractionStatus('Escuchando…');
    recognition.onresult = event => appendDictation(target, event.results[0][0].transcript);
    recognition.onerror = event => {
        if (event.error !== 'aborted') {
            setInteractionStatus('No se pudo reconocer la voz.');
        }
    };
    recognition.onend = () => {
        if (state.voice.recognition === recognition) {
            state.voice = { recognition: null, target: null, listening: false };
        }
    };
    try {
        recognition.start();
    } catch (_) {
        state.voice = { recognition: null, target: null, listening: false };
        setInteractionStatus('No se pudo iniciar el dictado.');
    }
}

function appendDictation(target, transcript) {
    const start = target.selectionStart ?? target.value.length;
    const end = target.selectionEnd ?? target.value.length;
    const prefix = start > 0 && !/\s$/.test(target.value.slice(0, start)) ? ' ' : '';
    let value = target.value.slice(0, start) + prefix + transcript.trim() + target.value.slice(end);
    if (target.maxLength > 0) {
        value = value.slice(0, target.maxLength);
    }
    target.value = value;
    target.selectionStart = target.selectionEnd = Math.min(value.length, start + prefix.length + transcript.trim().length);
    target.dispatchEvent(new Event('input', { bubbles: true }));
    setInteractionStatus('Texto reconocido.');
}

function stopVoiceInput() {
    if (state.voice.recognition) {
        state.voice.recognition.abort();
    }
    state.voice = { recognition: null, target: null, listening: false };
}

function tickClock() {
    const now = new Date();
    document.getElementById('clock').textContent = pad(now.getHours()) + ':' + pad(now.getMinutes());
    document.getElementById('clock-date').textContent = capitalize(new Intl.DateTimeFormat('es-ES', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(now));
    renderNextCountdown();
}

async function refreshInicio() {
    const upcoming = await api('/appointments/upcoming?limit=1');
    state.next = upcoming.status === 200 && upcoming.body.length > 0 ? upcoming.body[0] : null;
    renderNextCountdown();

    const today = await api('/appointments?period=DAY&anchor=' + todayIso() + '&pageSize=100');
    const list = document.getElementById('today-list');
    list.replaceChildren();

    if (today.status !== 200 || today.body.items.length === 0) {
        list.appendChild(el('li', 'muted', 'Nada para hoy'));
        return;
    }

    for (const item of today.body.items) {
        const line = el('li', item.status === 'CANCELLED' ? 'clickable cancelled' : 'clickable');
        line.textContent = item.start.slice(11, 16) + ' · ' + item.title;
        line.addEventListener('click', () => openPanel(item.id));
        list.appendChild(line);
    }
}

function renderNextCountdown() {
    const when = document.getElementById('next-when');
    const title = document.getElementById('next-title');

    if (!state.next) {
        when.textContent = 'Nada previsto';
        title.textContent = '';
        return;
    }

    const start = new Date(state.next.start);
    const minutes = Math.round((start - Date.now()) / 60000);
    title.textContent = state.next.title;

    if (minutes <= 0) {
        when.textContent = 'Ahora';
    } else if (minutes < 60) {
        when.textContent = 'En ' + minutes + ' min';
    } else if (state.next.start.slice(0, 10) === todayIso()) {
        when.textContent = 'A las ' + state.next.start.slice(11, 16);
    } else {
        when.textContent = capitalize(new Intl.DateTimeFormat('es-ES', { weekday: 'long' }).format(start))
            + ' a las ' + state.next.start.slice(11, 16);
    }
}

async function refreshAgenda() {
    const response = await api('/appointments?period=' + state.period + '&anchor=' + state.anchor
        + '&pageSize=200&includeCancelled=true');
    if (response.status !== 200) {
        return;
    }

    const page = response.body;
    state.previousAnchor = page.previousAnchor;
    state.nextAnchor = page.nextAnchor;

    const byDay = groupByDay(page.items);
    const anchorDate = new Date(page.anchor + 'T00:00');

    if (state.period === 'MONTH') {
        renderMonthTitle(anchorDate);
        renderMonthGrid(anchorDate, byDay, await loadDailyCounts(page.anchor));
        renderYearStrip(anchorDate);
    } else {
        const monday = startOfWeek(anchorDate);
        renderWeekTitle(monday);
        renderWeekGrid(monday, byDay);
        document.getElementById('year-strip').replaceChildren();
    }
}

function startOfWeek(date) {
    const monday = new Date(date);
    monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));

    return monday;
}

function groupByDay(items) {
    const byDay = new Map();
    for (const item of items) {
        const day = item.start.slice(0, 10);
        if (!byDay.has(day)) {
            byDay.set(day, []);
        }
        byDay.get(day).push(item);
    }

    return byDay;
}

async function loadDailyCounts(anchor) {
    const response = await api('/appointments/counts/by-day?month=' + anchor.slice(0, 7) + '&includeCancelled=true');
    const counts = new Map();

    if (response.status === 200) {
        for (const entry of response.body) {
            counts.set(entry.day, entry.count);
        }
    }

    return counts;
}

function renderMonthTitle(anchorDate) {
    document.getElementById('month-title').textContent = capitalize(new Intl.DateTimeFormat('es-ES', {
        month: 'long', year: 'numeric',
    }).format(anchorDate));
}

function renderWeekTitle(anchorDate) {
    const end = new Date(anchorDate);
    end.setDate(end.getDate() + 6);
    const format = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' });
    document.getElementById('month-title').textContent = format.format(anchorDate) + ' – ' + format.format(end);
}

function renderMonthGrid(anchorDate, byDay, countsByDay) {
    const grid = document.getElementById('month-grid');
    grid.className = 'month-grid';
    grid.replaceChildren();

    const year = anchorDate.getFullYear();
    const month = anchorDate.getMonth();
    const offset = (new Date(year, month, 1).getDay() + 6) % 7;
    const cursor = new Date(year, month, 1 - offset);

    for (let cell = 0; cell < 42; cell++) {
        const iso = isoDate(cursor);
        const cellNode = el('div', 'day');

        if (cursor.getMonth() !== month) {
            cellNode.classList.add('outside');
        }
        if (iso === todayIso()) {
            cellNode.classList.add('today');
        }

        const number = cursor.getDate() === 1
            ? cursor.getDate() + ' ' + new Intl.DateTimeFormat('es-ES', { month: 'short' }).format(cursor)
            : String(cursor.getDate());
        cellNode.appendChild(el('p', 'num', number));

        appendDayItems(cellNode, byDay.get(iso) || [], countsByDay.get(iso), 3);

        cellNode.addEventListener('click', onDayPicked(iso));
        grid.appendChild(cellNode);
        cursor.setDate(cursor.getDate() + 1);
    }
}

function renderWeekGrid(anchorDate, byDay) {
    const grid = document.getElementById('month-grid');
    grid.className = 'week-grid';
    grid.replaceChildren();

    const cursor = new Date(anchorDate);
    for (let cell = 0; cell < 7; cell++) {
        const iso = isoDate(cursor);
        const cellNode = el('div', 'day');

        if (iso === todayIso()) {
            cellNode.classList.add('today');
        }

        cellNode.appendChild(el('p', 'num', String(cursor.getDate())));
        appendDayItems(cellNode, byDay.get(iso) || [], undefined, 12);

        cellNode.addEventListener('click', onDayPicked(iso));
        grid.appendChild(cellNode);
        cursor.setDate(cursor.getDate() + 1);
    }
}

function appendDayItems(cellNode, items, total, limit) {
    for (const item of items.slice(0, limit)) {
        const line = el('p', item.status === 'CANCELLED' ? 'item cancelled' : 'item');
        line.textContent = item.start.slice(11, 16) + ' ' + item.title;
        line.addEventListener('click', event => {
            event.stopPropagation();
            openPanel(item.id);
        });
        cellNode.appendChild(line);
    }

    const count = total === undefined ? items.length : total;
    if (count > limit) {
        cellNode.appendChild(el('p', 'more', '+' + (count - limit)));
    }
}

function onDayPicked(iso) {
    return () => {
        document.getElementById('add-date').value = iso;
        document.getElementById('add-title').focus();
        updateAddHint();
    };
}

async function renderYearStrip(anchorDate) {
    const strip = document.getElementById('year-strip');
    const response = await api('/appointments/counts/by-month?year=' + anchorDate.getFullYear());
    strip.replaceChildren();

    if (response.status !== 200) {
        return;
    }

    const counts = new Map();
    let peak = 1;
    for (const entry of response.body) {
        const month = Number(entry.month.slice(5, 7));
        counts.set(month, entry.count);
        peak = Math.max(peak, entry.count);
    }

    for (let month = 1; month <= 12; month++) {
        const count = counts.get(month) || 0;
        const mark = button('month-mark', MONTH_INITIALS[month - 1], () => {
            state.anchor = anchorDate.getFullYear() + '-' + pad(month) + '-01';
            refreshAgenda();
        });
        mark.style.opacity = String(0.45 + 0.55 * (count / peak));
        mark.title = count + ' citas';

        if (month === anchorDate.getMonth() + 1) {
            mark.classList.add('active');
        }

        strip.appendChild(mark);
    }
}

function formValues() {
    const date = document.getElementById('add-date').value;
    const time = document.getElementById('add-time').value;
    const duration = Number(document.getElementById('add-duration').value);

    if (!date || !time) {
        return null;
    }

    const start = date + 'T' + time;
    const end = new Date(new Date(start).getTime() + duration * 60000);

    return { date, start, end: isoDateTime(end) };
}

let hintHandle = null;

function scheduleAddHint() {
    clearTimeout(hintHandle);
    hintHandle = setTimeout(updateAddHint, 250);
}

async function updateAddHint() {
    const hint = document.getElementById('add-hint');
    const values = formValues();
    hint.replaceChildren();

    if (!values) {
        return;
    }

    const overlapping = await api(
        '/appointments/overlapping?start=' + values.start + '&end=' + values.end);
    if (overlapping.status === 200 && overlapping.body.length > 0) {
        hint.appendChild(el('span', 'warn', 'Choca con ' + overlapping.body.map(item => item.title).join(', ')));
    }

    const slots = await api('/appointments/free-slots?day=' + values.date
        + '&from=08:00&to=22:00&minDurationMinutes=' + document.getElementById('add-duration').value);
    if (slots.status !== 200 || slots.body.length === 0) {
        return;
    }

    hint.appendChild(el('span', 'muted', 'Libre:'));
    for (const slot of slots.body.slice(0, 5)) {
        const time = slot.start.slice(11, 16);
        hint.appendChild(button('chip', time, () => {
            document.getElementById('add-time').value = time;
            updateAddHint();
        }));
    }
}

async function submitAppointment(allowOverlap) {
    const values = formValues();
    const feedback = document.getElementById('add-feedback');
    if (!values) {
        return;
    }

    const leadTime = document.getElementById('add-reminder').value;
    const response = await send('POST', '/appointments', {
        title: document.getElementById('add-title').value.trim(),
        start: values.start,
        end: values.end,
        reminderLeadTimesMinutes: leadTime ? [Number(leadTime)] : [],
        allowOverlap,
    });

    feedback.hidden = false;
    feedback.replaceChildren();

    if (response.status === 201) {
        document.getElementById('add-form').reset();
        document.getElementById('add-date').value = todayIso();
        document.getElementById('add-hint').replaceChildren();
        feedback.hidden = true;
        return;
    }

    if (response.status === 409 && response.body && response.body.code === 'Appointment.Overlaps') {
        feedback.append(errorMessage(response));
        feedback.appendChild(button('', 'Añadir igualmente', () => submitAppointment(true)));
        return;
    }

    feedback.textContent = errorMessage(response);
}

async function openPanel(id) {
    const response = await api('/appointments/' + id);
    if (response.status !== 200) {
        if (response.status === 404 && state.openId === id) {
            closePanel();
        }
        return;
    }

    state.openId = id;
    renderPanel(response.body);
}

function closePanel() {
    state.openId = null;
    const panel = document.getElementById('panel');
    panel.hidden = true;
    panel.replaceChildren();
}

function renderPanel(appointment) {
    const panel = document.getElementById('panel');
    panel.hidden = false;
    panel.replaceChildren();

    const header = el('header', 'panel-header');
    header.appendChild(el('p', 'label', appointment.status === 'CANCELLED' ? 'Cancelada' : 'Cita'));
    header.appendChild(button('panel-close', '×', closePanel));
    panel.appendChild(header);

    panel.appendChild(el('h2', 'panel-title', appointment.title));
    panel.appendChild(el('p', 'panel-when', panelWhen(appointment)));

    if (appointment.description) {
        panel.appendChild(el('p', 'panel-description', appointment.description));
    }

    if (appointment.status === 'CANCELLED') {
        panel.appendChild(restoreSection(appointment));
        panel.appendChild(deleteAction(appointment));
        return;
    }

    panel.appendChild(detailsSection(appointment));
    panel.appendChild(rescheduleSection(appointment));
    panel.appendChild(remindersSection(appointment));

    const actions = el('div', 'panel-actions');
    actions.appendChild(button('danger', 'Cancelar cita', () => cancelAppointment(appointment.id)));
    panel.appendChild(actions);
    panel.appendChild(deleteAction(appointment));
}

function deleteAction(appointment) {
    const actions = el('div', 'panel-actions');

    const ask = button('quiet', 'Eliminar', () => {
        actions.replaceChildren();
        actions.appendChild(el('p', 'muted-note', 'Se borra para siempre, sin recuperación.'));
        actions.appendChild(button('danger', 'Eliminar definitivamente', async () => {
            const response = await api('/appointments/' + appointment.id, { method: 'DELETE' });
            if (response.status === 204) {
                closePanel();
                refreshAll();
                return;
            }
            reportPanel(actions, response, null);
        }));
        actions.appendChild(button('quiet', 'Mejor no', () => renderPanel(appointment)));
    });

    actions.appendChild(ask);

    return actions;
}

function restoreSection(appointment) {
    const section = el('section', 'panel-section');
    section.appendChild(el('p', 'muted-note', 'Esta cita está cancelada. Puedes recuperarla con su franja y sus avisos.'));

    const restore = async allowOverlap => {
        const response = await send('POST', '/appointments/' + appointment.id + '/restore', { allowOverlap });

        if (response.status === 409 && response.body && response.body.code === 'Appointment.Overlaps') {
            reportPanel(section, response, null, button('', 'Recuperar igualmente', () => restore(true)));
            return;
        }

        reportPanel(section, response, () => openPanel(appointment.id));
    };

    section.appendChild(button('', 'Recuperar cita', () => restore(false)));

    return section;
}

function panelWhen(appointment) {
    const start = new Date(appointment.start);
    const day = capitalize(new Intl.DateTimeFormat('es-ES', {
        weekday: 'long', day: 'numeric', month: 'long',
    }).format(start));

    return day + ' · ' + appointment.start.slice(11, 16) + ' – ' + appointment.end.slice(11, 16);
}

function detailsSection(appointment) {
    const section = el('section', 'panel-section');
    section.appendChild(el('p', 'label', 'Detalles'));

    const title = el('input', 'panel-input');
    title.type = 'text';
    title.maxLength = 120;
    title.value = appointment.title;

    const description = el('textarea', 'panel-input');
    description.rows = 2;
    description.maxLength = 2000;
    description.placeholder = 'Descripción';
    description.value = appointment.description || '';

    section.append(title, description);
    section.appendChild(button('', 'Guardar', async () => {
        const response = await send('PUT', '/appointments/' + appointment.id + '/details', {
            title: title.value.trim(),
            description: description.value.trim() || null,
        });
        reportPanel(section, response, () => openPanel(appointment.id));
    }));

    return section;
}

function rescheduleSection(appointment) {
    const section = el('section', 'panel-section');
    section.appendChild(el('p', 'label', 'Reprogramar'));

    const date = el('input', 'panel-input');
    date.type = 'date';
    date.value = appointment.start.slice(0, 10);

    const time = el('input', 'panel-input');
    time.type = 'time';
    time.value = appointment.start.slice(11, 16);

    const duration = el('select', 'panel-input');
    const current = Math.round((new Date(appointment.end) - new Date(appointment.start)) / 60000);
    for (const minutes of DURATIONS) {
        const option = el('option', null, formatLeadTime(minutes));
        option.value = String(minutes);
        option.selected = minutes === current;
        duration.appendChild(option);
    }

    const row = el('div', 'panel-row');
    row.append(date, time);
    section.append(row, duration);

    const move = async allowOverlap => {
        const start = date.value + 'T' + time.value;
        const end = new Date(new Date(start).getTime() + Number(duration.value) * 60000);
        const response = await send('POST', '/appointments/' + appointment.id + '/reschedule', {
            newStart: start,
            newEnd: isoDateTime(end),
            allowOverlap,
        });

        if (response.status === 409 && response.body && response.body.code === 'Appointment.Overlaps') {
            reportPanel(section, response, null, button('', 'Mover igualmente', () => move(true)));
            return;
        }

        reportPanel(section, response, () => openPanel(appointment.id));
    };

    section.appendChild(button('', 'Mover', () => move(false)));

    return section;
}

function remindersSection(appointment) {
    const section = el('section', 'panel-section');
    section.appendChild(el('p', 'label', 'Avisos'));

    const list = el('ul', 'reminder-list');
    for (const reminder of appointment.reminders) {
        const line = el('li', reminder.acknowledgedAt ? 'acknowledged' : null);
        line.appendChild(el('span', null, formatLeadTime(reminder.leadTimeMinutes) + ' antes'));

        if (reminder.acknowledgedAt) {
            line.appendChild(el('span', 'muted', 'visto'));
        }

        line.appendChild(button('remove', '×', async () => {
            const response = await api(
                '/appointments/' + appointment.id + '/reminders/' + reminder.id, { method: 'DELETE' });
            reportPanel(section, response, () => openPanel(appointment.id));
        }));
        list.appendChild(line);
    }

    if (appointment.reminders.length === 0) {
        list.appendChild(el('li', 'muted', 'Sin avisos'));
    }
    section.appendChild(list);

    const lead = el('select', 'panel-input');
    for (const minutes of LEAD_TIMES) {
        const option = el('option', null, formatLeadTime(minutes) + ' antes');
        option.value = String(minutes);
        lead.appendChild(option);
    }

    section.appendChild(lead);
    section.appendChild(button('', 'Añadir aviso', async () => {
        const response = await send('POST', '/appointments/' + appointment.id + '/reminders', {
            leadTimeMinutes: Number(lead.value),
        });
        reportPanel(section, response, () => openPanel(appointment.id));
    }));

    return section;
}

function reportPanel(section, response, onSuccess, extra) {
    const previous = section.querySelector('.panel-feedback');
    if (previous) {
        previous.remove();
    }

    if (response.status < 300) {
        if (onSuccess) {
            onSuccess();
        }
        return;
    }

    const feedback = el('p', 'panel-feedback', errorMessage(response));
    if (extra) {
        feedback.appendChild(extra);
    }
    section.appendChild(feedback);
}

async function cancelAppointment(id) {
    const response = await api('/appointments/' + id + '/cancel', { method: 'POST' });
    if (response.status === 204) {
        openPanel(id);
    }
}

function showReminderToast(reminder) {
    const toast = el('div', 'toast');
    toast.appendChild(el('p', 'label', 'Recordatorio'));

    const title = el('p', 'title clickable', reminder.title);
    title.addEventListener('click', () => openPanel(reminder.appointmentId));
    toast.appendChild(title);

    toast.appendChild(el('p', 'when', 'A las ' + reminder.start.slice(11, 16)));
    toast.appendChild(button('seen', 'Visto', async () => {
        await api('/appointments/' + reminder.appointmentId + '/reminders/' + reminder.reminderId + '/acknowledge',
            { method: 'POST' });
        toast.remove();
    }));

    document.getElementById('toasts').appendChild(toast);
}

function refreshAll() {
    if (!state.authenticated) {
        return;
    }

    refreshInicio();

    if (state.view === 'agenda') {
        refreshAgenda();
    }
    if (state.openId) {
        openPanel(state.openId);
    }
}

function connectEvents() {
    if (eventSource) {
        return;
    }

    eventSource = new EventSource('/events');
    const refreshing = [
        'appointmentScheduled', 'appointmentRescheduled', 'appointmentCancelled', 'appointmentRestored',
        'appointmentDeleted',
        'appointmentDetailsChanged', 'reminderAdded', 'reminderRemoved', 'reminderAcknowledged',
    ];

    for (const name of refreshing) {
        eventSource.addEventListener(name, refreshAll);
    }

    eventSource.addEventListener('reminderDue', event => {
        showReminderToast(JSON.parse(event.data));
    });
}

function disconnectEvents() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
}

function setAuthenticated(authenticated) {
    const changed = state.authenticated !== authenticated;
    state.authenticated = authenticated;
    document.body.classList.toggle('locked', !authenticated);

    if (!authenticated) {
        if (changed) {
            Object.assign(state.gesture, {
                candidate: null, frames: 0, missing: 0, latched: null,
            });
            stopVoiceInput();
            setPointerTarget(null);
            document.getElementById('gesture-pointer').classList.remove('visible');
        }
        disconnectEvents();
        closePanel();
        document.getElementById('toasts').replaceChildren();
        state.next = null;
        state.view = 'inicio';
        document.getElementById('view-inicio').hidden = false;
        document.getElementById('view-agenda').hidden = true;
        renderNextCountdown();
        return;
    }

    if (changed) {
        setInteractionStatus('Gestos activos: apunta y junta los dedos para seleccionar.');
        refreshAll();
        connectEvents();
    }
}

async function refreshAuthentication() {
    const response = await api('/presence/authentication');
    if (response.status !== 200) {
        state.authentication = null;
        setAuthenticated(false);
        renderAuthenticationStatus(response);
        updatePresenceActions();
        return;
    }

    state.authentication = response.body;
    const active = Boolean(state.authentication.activeSession);
    document.getElementById('enrollment').hidden = !state.authentication.maintenanceMode;
    setAuthenticated(active);
    renderAuthenticationStatus();
    updatePresenceActions();
    if (state.authentication.maintenanceMode) {
        await refreshProfiles();
    }
}

function renderAuthenticationStatus(failedResponse) {
    if (state.authenticated || state.authBusy) {
        return;
    }

    const message = document.getElementById('access-message');
    if (failedResponse) {
        message.textContent = failedResponse.status === 503
            ? 'Presence no está disponible. Inicia su backend para acceder.'
            : presenceError(failedResponse);
        return;
    }
    if (!state.authentication) {
        message.textContent = 'Consultando el estado de acceso…';
        return;
    }
    if (state.authentication.enrolledProfiles === 0) {
        message.textContent = state.authentication.maintenanceMode
            ? 'Mira a cámara y registra el primer rostro.'
            : 'No hay un rostro registrado. Reinicia Presence en modo mantenimiento.';
        return;
    }
    message.textContent = state.cameraReady
        ? faceStatus(state.recognition).reason
        : 'Preparando el reconocimiento facial…';
}

function updatePresenceActions() {
    const active = state.authentication && state.authentication.activeSession;
    const enrolled = state.authentication && state.authentication.enrolledProfiles > 0;
    document.getElementById('authenticate').disabled = state.authBusy || !state.cameraReady || !enrolled
        || Boolean(active);
    document.getElementById('enroll').disabled = state.authBusy || !state.cameraReady
        || !document.getElementById('display-name').value.trim();
}

function presenceError(response) {
    return response.body && response.body.message
        ? response.body.message
        : 'No se pudo completar la operación.';
}

function setAuthFeedback(text, error = false) {
    const feedback = document.getElementById('auth-feedback');
    feedback.textContent = text;
    feedback.classList.toggle('error', error);
}

async function refreshProfiles() {
    const response = await api('/presence/profiles');
    if (response.status !== 200) {
        return;
    }

    const profiles = document.getElementById('profiles');
    profiles.replaceChildren();
    for (const profile of response.body) {
        const item = el('li');
        item.appendChild(el('span', null, profile.displayName + ' · ' + profile.templateCount + ' captura(s)'));
        const actions = el('span', 'profile-actions');
        actions.appendChild(button('quiet', 'Eliminar', () => {
            actions.replaceChildren(
                button('quiet', 'Confirmar', () => deleteProfile(profile.id)),
                button('quiet', 'Cancelar', refreshProfiles));
        }));
        item.appendChild(actions);
        profiles.appendChild(item);
    }
}

async function enrollProfile() {
    const status = faceStatus(state.recognition);
    if (!status.ready) {
        setAuthFeedback(status.reason, true);
        return;
    }

    state.authBusy = true;
    updatePresenceActions();
    setAuthFeedback('Registrando rostro…');
    const response = await send('POST', '/presence/profiles', {
        displayName: document.getElementById('display-name').value.trim(),
        modelVersion: MODEL_VERSION,
        descriptor: Array.from(status.face.embedding),
    });
    if (response.status < 300) {
        document.getElementById('display-name').value = '';
        setAuthFeedback('Rostro registrado. Reinicia Presence sin --maintenance para autenticarte.');
        await refreshAuthentication();
    } else {
        setAuthFeedback(presenceError(response), true);
    }
    state.authBusy = false;
    updatePresenceActions();
}

async function deleteProfile(id) {
    const response = await api('/presence/profiles/' + id, { method: 'DELETE' });
    if (response.status === 204) {
        await refreshAuthentication();
    } else {
        setAuthFeedback(presenceError(response), true);
    }
}

function challengeDetector(type) {
    return result => type === 'VICTORY'
        && (result.gesture || []).some(value => value.hand === 0 && value.gesture === 'victory');
}

function sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

async function waitForChallenge(challenge) {
    const detected = challengeDetector(challenge.type);
    const expiresAt = Date.parse(challenge.expiresAt);
    while (Date.now() < expiresAt) {
        document.getElementById('challenge-time').textContent = Math.max(0,
            Math.ceil((expiresAt - Date.now()) / 1000)) + ' s';
        const status = faceStatus(state.recognition);
        if (status.ready && detected(state.recognition)) {
            return status.face;
        }
        await sleep(80);
    }
    throw { message: 'El desafío ha caducado. Inténtalo de nuevo.' };
}

async function authenticate() {
    state.authBusy = true;
    updatePresenceActions();
    setAuthFeedback('Preparando prueba de vida…');
    const challengeNode = document.getElementById('auth-challenge');
    try {
        const started = await send('POST', '/presence/authentication/challenges', {});
        if (started.status >= 300) {
            throw started;
        }
        const challenge = started.body;
        challengeNode.hidden = false;
        document.getElementById('challenge-instruction').textContent = 'Introduce el código secreto';
        setAuthFeedback('Verificando código gestual…');
        const face = await waitForChallenge(challenge);
        const completed = await send('POST',
            '/presence/authentication/challenges/' + challenge.challengeId + '/complete', {
                modelVersion: MODEL_VERSION,
                descriptor: Array.from(face.embedding),
                observedType: challenge.type,
                nonce: challenge.nonce,
                capturedAt: new Date().toISOString(),
            });
        if (completed.status >= 300) {
            throw completed;
        }
        setAuthFeedback('Identidad confirmada.');
        await refreshAuthentication();
    } catch (error) {
        setAuthFeedback(error.status ? presenceError(error)
            : error.message || 'No se pudo completar la autenticación.', true);
    } finally {
        challengeNode.hidden = true;
        state.authBusy = false;
        updatePresenceActions();
    }
}

async function closePresenceSession() {
    const session = state.authentication && state.authentication.activeSession;
    if (!session) {
        return;
    }
    const response = await api('/presence/sessions/' + session.id, { method: 'DELETE' });
    if (response.status === 204) {
        state.authentication.activeSession = null;
        setAuthenticated(false);
        await refreshAuthentication();
    }
}

function switchView(view) {
    if (!state.authenticated) {
        return;
    }

    state.view = view;
    document.getElementById('view-inicio').hidden = view !== 'inicio';
    document.getElementById('view-agenda').hidden = view !== 'agenda';

    for (const link of document.querySelectorAll('.nav-link[data-view]')) {
        link.classList.toggle('active', link.dataset.view === view);
    }

    if (view === 'agenda') {
        refreshAgenda();
    }
}

function switchPeriod(period) {
    state.period = period;
    state.anchor = todayIso();

    for (const link of document.querySelectorAll('.period-link')) {
        link.classList.toggle('active', link.dataset.period === period);
    }

    refreshAgenda();
}

document.addEventListener('DOMContentLoaded', () => {
    startCamera();
    tickClock();
    setInterval(tickClock, 1000);
    refreshAuthentication();
    setInterval(refreshAuthentication, 2000);
    setInterval(refreshAll, 60000);

    for (const link of document.querySelectorAll('.nav-link[data-view]')) {
        link.addEventListener('click', () => switchView(link.dataset.view));
    }
    document.getElementById('authenticate').addEventListener('click', authenticate);
    document.getElementById('enroll').addEventListener('click', enrollProfile);
    document.getElementById('display-name').addEventListener('input', updatePresenceActions);
    document.getElementById('lock-agenda').addEventListener('click', closePresenceSession);
    for (const link of document.querySelectorAll('.period-link')) {
        link.addEventListener('click', () => switchPeriod(link.dataset.period));
    }

    document.getElementById('month-prev').addEventListener('click', () => {
        state.anchor = state.previousAnchor;
        refreshAgenda();
    });
    document.getElementById('month-next').addEventListener('click', () => {
        state.anchor = state.nextAnchor;
        refreshAgenda();
    });

    document.getElementById('add-date').value = todayIso();
    document.getElementById('add-form').addEventListener('submit', event => {
        event.preventDefault();
        submitAppointment(false);
    });

    for (const id of ['add-date', 'add-time', 'add-duration']) {
        document.getElementById(id).addEventListener('change', scheduleAddHint);
    }

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            closePanel();
        }
    });
});
