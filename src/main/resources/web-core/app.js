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
    home: null,
    homeProfileId: null,
    human: null,
    recognition: null,
    recognitionVersion: 0,
    faceCenter: null,
    handRecognizer: null,
    handResult: null,
    handResultVersion: 0,
    handVideoTime: -1,
    gesture: {
        candidate: null, candidateSince: 0, missingSince: 0, latched: null,
    },
};

let eventSource = null;

const DURATIONS = [30, 60, 90, 120];
const LEAD_TIMES = [10, 30, 60, 1440];
const MONTH_INITIALS = ['E', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D'];
const VIEWS = ['inicio', 'agenda', 'rutinas', 'economia', 'nutricion', 'entrenamiento'];
const MODEL_VERSION = 'human-faceres-3.3.6';
const MIN_CONFIDENCE = 0.6;
const {
    pointCoordinates, jointAngle, fingerIsExtended, isPinch,
    isDirectionalPose, isPointingPose, isOpenPalmPose, staticDirection,
    PINCH_DISTANCE_RATIO, HAND_CONFIDENCE, GESTURE_HOLD_MS, PINCH_HOLD_MS, challengeDetector,
} = AtlasGestures;
const {
    ENROLLMENT_POSES, ENROLLMENT_HOLD_FRAMES, ENROLLMENT_FRONT_FRAMES,
    sleep, faceResultIsRecent, showEnrollmentStep, enrollmentPoseFeedback,
} = AtlasEnrollment;
const enrollment = AtlasEnrollment.bind({
    faceStatus: (result, options) => faceStatus(result, options),
    center: { get: () => state.faceCenter, set: value => { state.faceCenter = value; } },
});
// ponytail: pinned CDN keeps face recognition out of the Agenda build; self-host it if offline use is required.
const HUMAN_MODELS = 'https://cdn.jsdelivr.net/npm/@vladmandic/human@3.3.6/models/';
const MEDIAPIPE = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/vision_bundle.mjs';
const MEDIAPIPE_WASM = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/wasm';
const GESTURE_MODEL = 'https://storage.googleapis.com/mediapipe-models/gesture_recognizer/'
    + 'gesture_recognizer/float16/1/gesture_recognizer.task';

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

function errorMessage(response) {
    const code = response.body && response.body.code;

    return window.AtlasErrorMessages[code] || window.AtlasStatusMessages[response.status]
        || 'No se pudo completar la operación.';
}

function cameraErrorMessage(error) {
    return ({
        NotAllowedError: 'No se ha concedido permiso para usar la cámara.',
        NotFoundError: 'No se encuentra ninguna cámara disponible.',
        NotReadableError: 'La cámara está siendo utilizada por otra aplicación.',
        OverconstrainedError: 'La cámara no admite la configuración solicitada.',
        SecurityError: 'El navegador ha bloqueado el acceso a la cámara.',
    })[error?.name] || 'No se pudo iniciar la cámara.';
}

function frontendErrorMessage(error, fallback) {
    if (error instanceof TypeError) {
        return 'No se pudo conectar con el servicio.';
    }
    return error?.name === 'Error' && error.message ? error.message : fallback;
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
            async: true,
            cacheSensitivity: 0,
            debug: false,
            modelBasePath: HUMAN_MODELS,
            filter: { enabled: true, autoBrightness: true, equalization: false },
            face: {
                enabled: true,
                detector: {
                    rotation: true, return: false, maxDetected: 2,
                    minConfidence: MIN_CONFIDENCE, minSize: AtlasFaceQuality.DETECTOR_MIN_FACE_SIZE,
                },
                mesh: { enabled: true },
                iris: { enabled: false },
                description: { enabled: true },
                emotion: { enabled: false },
                antispoof: { enabled: true },
                liveness: { enabled: true },
            },
            body: { enabled: false },
            hand: { enabled: false },
            object: { enabled: false },
            segmentation: { enabled: false },
            gesture: { enabled: true },
        });
        await state.human.load();
        await state.human.warmup();
        document.getElementById('access-message').textContent = 'Cargando seguimiento de manos…';
        const { FilesetResolver, GestureRecognizer } = await import(MEDIAPIPE);
        const vision = await FilesetResolver.forVisionTasks(MEDIAPIPE_WASM);
        state.handRecognizer = await GestureRecognizer.createFromOptions(vision, {
            baseOptions: { modelAssetPath: GESTURE_MODEL },
            runningMode: 'VIDEO',
            numHands: 1,
            minHandDetectionConfidence: HAND_CONFIDENCE,
            minHandPresenceConfidence: HAND_CONFIDENCE,
            minTrackingConfidence: HAND_CONFIDENCE,
            cannedGesturesClassifierOptions: { maxResults: 1, scoreThreshold: HAND_CONFIDENCE },
        });
        state.cameraReady = true;
        detectFaces();
        detectHands();
    } catch (error) {
        setAuthFeedback(cameraErrorMessage(error), true);
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
        const video = document.getElementById('mirror');
        if (!state.authenticated) {
            state.recognition = await state.human.detect(video);
            state.recognitionVersion++;
            renderAuthenticationStatus();
        }
    } catch (_) {
        document.getElementById('access-message').textContent = 'No se pudo procesar la imagen.';
    }
    requestAnimationFrame(detectFaces);
}

function detectHands() {
    if (!state.cameraReady) {
        return;
    }
    try {
        const video = document.getElementById('mirror');
        if (video.currentTime !== state.handVideoTime) {
            state.handVideoTime = video.currentTime;
            state.handResult = state.handRecognizer.recognizeForVideo(video, performance.now());
            state.handResultVersion++;
            trackHandGesture(state.handResult);
        }
    } catch (_) {
        state.handResult = null;
    }
    requestAnimationFrame(detectHands);
}

function faceStatus(result, options = {}) {
    return AtlasFaceQuality.evaluate(result, {
        handResult: state.handResult,
        center: state.faceCenter,
        ...options,
    });
}

function trackHandGesture(result) {
    const observed = recognizedHandGesture(result);
    AtlasInteraction.track(result, observed);
    const tracking = state.gesture;
    const now = performance.now();
    if (!observed) {
        tracking.missingSince ||= now;
        if (now - tracking.missingSince >= GESTURE_HOLD_MS) {
            tracking.candidate = null;
            tracking.candidateSince = 0;
            tracking.latched = null;
        }
        return;
    }

    tracking.missingSince = 0;
    if (tracking.candidate !== observed.type) {
        tracking.candidate = observed.type;
        tracking.candidateSince = now;
        return;
    }
    const hold = observed.type === 'PINCH' ? PINCH_HOLD_MS : GESTURE_HOLD_MS;
    if (now - tracking.candidateSince < hold || tracking.latched === observed.type) {
        return;
    }

    tracking.latched = observed.type;
    publishGesture(observed);
}

function publishGesture(observed) {
    if (!state.authenticated) {
        if (observed.type === 'FIST' && !state.authBusy && state.cameraReady
            && state.authentication
            && state.authentication.enrolledProfiles > 0) {
            authenticate();
        } else if (observed.type === 'PINCH' || observed.type === 'POINT') {
            AtlasInteraction.apply(observed.type);
        }
        return;
    }

    send('POST', '/interactions/gestures', {
        ...observed,
        observedAt: new Date().toISOString(),
    }).then(response => {
        if (observed.type === 'FIST' && response.status === 204) {
            setAuthenticated(false);
            refreshAuthentication();
        } else if (response.status === 204) {
            AtlasInteraction.apply(observed.type);
        } else if (response.status === 401) {
            refreshAuthentication();
        }
    }).catch(() => {});
}

function recognizedHandGesture(result) {
    const landmarks = result?.landmarks?.[0];
    if (!landmarks || landmarks.length !== 21) {
        return null;
    }

    const pose = result.worldLandmarks?.[0] || landmarks;
    const gesture = result.gestures?.[0]?.[0];
    const openPalm = isOpenPalmPose(pose) || pose !== landmarks && isOpenPalmPose(landmarks);
    let type = null;
    if (openPalm && gesture?.categoryName === 'Open_Palm') {
        type = 'OPEN_PALM';
    } else if (isPinch(pose) || pose !== landmarks && isPinch(landmarks)) {
        type = 'PINCH';
    } else if (isDirectionalPose(pose)) {
        type = staticDirection(pose);
        if (!type) {
            return null;
        }
    } else if (isPointingPose(pose) || pose !== landmarks && isPointingPose(landmarks)) {
        type = 'POINT';
    } else if (openPalm) {
        type = 'OPEN_PALM';
    } else {
        type = ({
            Closed_Fist: 'FIST', Open_Palm: 'OPEN_PALM', Pointing_Up: 'POINT',
            Thumb_Up: 'THUMBS_UP',
        })[gesture?.categoryName] || null;
    }

    return type ? {
        type,
        handIndex: 0,
        confidence: Number((gesture?.score || HAND_CONFIDENCE).toFixed(3)),
    } : null;
}

const SKY_ICONS = {
    clear: '<circle cx="12" cy="12" r="4.2"/><path d="M12 2.4v2.6M12 19v2.6M4.2 12H1.6M22.4 12h-2.6'
        + 'M6.5 6.5 4.6 4.6M19.4 19.4l-1.9-1.9M17.5 6.5l1.9-1.9M4.6 19.4l1.9-1.9"/>',
    partly: '<circle cx="8.4" cy="7.6" r="3.2"/><path d="M8.4 1.6v1.8M2.8 7.6H1M4.4 3.6 3.1 2.3"/>'
        + '<path d="M7.4 20.4h9.8a3.6 3.6 0 0 0 .4-7.2 5.2 5.2 0 0 0-10.2 1 3.1 3.1 0 0 0 0 6.2Z"/>',
    cloudy: '<path d="M6.8 17.4h10.4a3.8 3.8 0 0 0 .4-7.6 5.6 5.6 0 0 0-10.8 1 3.3 3.3 0 0 0 0 6.6Z"/>',
    fog: '<path d="M6.8 14.4h10.4a3.8 3.8 0 0 0 .4-7.6 5.6 5.6 0 0 0-10.8 1 3.3 3.3 0 0 0 0 6.6Z"/>'
        + '<path d="M4 18h16M6.5 21.4h11"/>',
    rain: '<path d="M6.8 14.6h10.4a3.8 3.8 0 0 0 .4-7.6 5.6 5.6 0 0 0-10.8 1 3.3 3.3 0 0 0 0 6.6Z"/>'
        + '<path d="M8.6 17.6 7.4 21M12.6 17.6l-1.2 3.4M16.6 17.6l-1.2 3.4"/>',
    snow: '<path d="M6.8 14.6h10.4a3.8 3.8 0 0 0 .4-7.6 5.6 5.6 0 0 0-10.8 1 3.3 3.3 0 0 0 0 6.6Z"/>'
        + '<path d="M8 19.4h.02M12 18.4h.02M16 19.4h.02M10 22h.02M14 22h.02"/>',
    thunder: '<path d="M6.8 14.2h10.4a3.8 3.8 0 0 0 .4-7.6 5.6 5.6 0 0 0-10.8 1 3.3 3.3 0 0 0 0 6.6Z"/>'
        + '<path d="M13.6 15.6 10.6 20h3.1L11.6 23.4"/>',
};

const SKY_ICON_FOR = {
    0: 'clear', 1: 'partly', 2: 'partly', 3: 'cloudy',
    45: 'fog', 48: 'fog',
    51: 'rain', 53: 'rain', 55: 'rain', 56: 'rain', 57: 'rain',
    61: 'rain', 63: 'rain', 65: 'rain', 66: 'rain', 67: 'rain',
    80: 'rain', 81: 'rain', 82: 'rain',
    71: 'snow', 73: 'snow', 75: 'snow', 77: 'snow', 85: 'snow', 86: 'snow',
    95: 'thunder', 96: 'thunder', 99: 'thunder',
};

function renderSkyIcon(code) {
    const icon = document.getElementById('weather-icon');
    const shape = SKY_ICONS[SKY_ICON_FOR[code]];
    icon.hidden = !shape;
    if (shape) {
        icon.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true">' + shape + '</svg>';
    }
}

const SKY = {
    0: 'Despejado', 1: 'Casi despejado', 2: 'Parcialmente nublado', 3: 'Nublado',
    45: 'Niebla', 48: 'Niebla helada',
    51: 'Llovizna débil', 53: 'Llovizna', 55: 'Llovizna intensa',
    56: 'Llovizna helada', 57: 'Llovizna helada intensa',
    61: 'Lluvia débil', 63: 'Lluvia', 65: 'Lluvia intensa',
    66: 'Lluvia helada', 67: 'Lluvia helada intensa',
    71: 'Nieve débil', 73: 'Nieve', 75: 'Nieve intensa', 77: 'Granizo blando',
    80: 'Chubascos', 81: 'Chubascos', 82: 'Chubascos fuertes',
    85: 'Chubascos de nieve', 86: 'Chubascos de nieve',
    95: 'Tormenta', 96: 'Tormenta con granizo', 99: 'Tormenta con granizo',
};

const WEATHER_REFRESH = 900000;
const NEWS_REFRESH = 1800000;

function degrees(value) {
    return Math.round(value) + '°';
}

async function refreshWeather() {
    const place = state.home?.location;
    const block = document.getElementById('weather');
    if (!place) {
        block.hidden = true;
        return;
    }

    const url = 'https://api.open-meteo.com/v1/forecast'
        + '?latitude=' + place.latitude + '&longitude=' + place.longitude
        + '&current=temperature_2m,weather_code'
        + '&daily=temperature_2m_max,temperature_2m_min'
        + '&timezone=auto&forecast_days=1';

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('No se pudo consultar el tiempo.');
        }
        const data = await response.json();
        document.getElementById('weather-now').textContent = degrees(data.current.temperature_2m);
        renderSkyIcon(data.current.weather_code);
        const sky = SKY[data.current.weather_code];
        const range = degrees(data.daily.temperature_2m_max[0]) + ' / ' + degrees(data.daily.temperature_2m_min[0]);
        document.getElementById('weather-detail').textContent = (sky ? sky + ' · ' : '') + range + ' · ' + place.name;
        block.hidden = false;
    } catch (_) {
        block.hidden = true;
    }
}

async function refreshNews() {
    const block = document.getElementById('news');
    const categories = state.home?.newsCategories;
    if (!categories?.length) {
        block.hidden = true;
        return;
    }
    try {
        const response = await api('/news?categories=' + encodeURIComponent(categories.join(',')));
        if (response.status !== 200 || !Array.isArray(response.body) || response.body.length === 0) {
            throw new Error('Las noticias no están disponibles.');
        }
        const list = document.getElementById('news-list');
        list.replaceChildren();
        for (const item of response.body) {
            const link = el('a');
            link.href = item.url;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            link.append(
                el('span', 'news-meta', item.category + ' · ' + formatNewsDate(item.publishedAt)),
                el('span', 'news-title', item.title));
            const entry = el('li', 'news-item');
            entry.appendChild(link);
            list.appendChild(entry);
        }
        block.hidden = false;
    } catch (_) {
        block.hidden = true;
    }
}

function formatNewsDate(date) {
    return new Date(date + 'T12:00:00').toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

function tickClock() {
    const now = new Date();
    const timeZone = state.home?.location.timeZone;
    document.getElementById('clock').textContent = new Intl.DateTimeFormat('es-ES', {
        hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone,
    }).format(now);
    document.getElementById('clock-date').textContent = capitalize(new Intl.DateTimeFormat('es-ES', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone,
    }).format(now));
    renderNextCountdown();
}

async function refreshInicio() {
    window.AtlasRoutines?.summary();
    window.AtlasEconomy?.summary();
    window.AtlasNutrition?.summary();

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
        document.getElementById('add-title').focus({ preventScroll: true });
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
    if (state.view === 'rutinas') {
        window.AtlasRoutines?.activate();
    }
    if (state.view === 'economia') {
        window.AtlasEconomy?.activate();
    }
    if (state.view === 'entrenamiento') {
        window.AtlasTraining?.activate();
    }
    if (state.view === 'nutricion') {
        window.AtlasNutrition?.activate();
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
        if (!state.authBusy) {
            state.faceCenter = null;
        }
        if (changed) {
            Object.assign(state.gesture, {
                candidate: null, candidateSince: 0, missingSince: 0, latched: null,
            });
            AtlasInteraction.reset();
        }
        disconnectEvents();
        closePanel();
        document.getElementById('toasts').replaceChildren();
        state.home = null;
        state.homeProfileId = null;
        document.getElementById('weather').hidden = true;
        document.getElementById('news').hidden = true;
        state.next = null;
        state.view = 'inicio';
        document.body.classList.remove('routines-active');
        document.getElementById('view-inicio').hidden = false;
        document.getElementById('view-agenda').hidden = true;
        document.getElementById('view-rutinas').hidden = true;
        document.getElementById('view-economia').hidden = true;
        document.getElementById('view-nutricion').hidden = true;
        document.getElementById('view-entrenamiento').hidden = true;
        renderNextCountdown();
        return;
    }

    if (changed) {
        AtlasInteraction.status('Gestos activos: junta pulgar e índice para seleccionar; orienta índice y corazón para navegar.');
        refreshAll();
        connectEvents();
    }
}

async function refreshAuthentication() {
    const response = await api('/authentication');
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
    if (active) {
        await refreshHomeProfile(state.authentication.activeSession.profileId);
    }
    renderAuthenticationStatus();
    updatePresenceActions();
    if (state.authentication.maintenanceMode) {
        await refreshProfiles();
    }
}

async function refreshHomeProfile(profileId) {
    if (state.homeProfileId === profileId) {
        return;
    }

    const response = await api('/home/profiles/' + encodeURIComponent(profileId));
    state.homeProfileId = profileId;
    state.home = response.status === 200 ? response.body : null;
    tickClock();
    await Promise.all([refreshWeather(), refreshNews()]);
    if (response.status === 404) {
        AtlasInteraction.status('Completa el onboarding de Inicio en modo mantenimiento.');
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
            : 'No hay un rostro registrado. Reinicia Atlas en modo mantenimiento.';
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
        || Boolean(active) || !document.getElementById('display-name').value.trim()
        || !document.getElementById('home-location').value.trim()
        || selectedNewsCategories().length === 0;
}

function presenceError(response) {
    return errorMessage(response);
}

function setAuthFeedback(text, error = false) {
    const feedback = document.getElementById('auth-feedback');
    feedback.textContent = text;
    feedback.classList.toggle('error', error);
}

async function refreshProfiles() {
    const response = await api('/profiles');
    if (response.status !== 200) {
        return;
    }

    const profiles = document.getElementById('profiles');
    profiles.replaceChildren();
    for (const profile of response.body) {
        const item = el('li');
        item.appendChild(el('span', null, profile.displayName + ' · ' + profile.templateCount + ' captura(s)'));
        const actions = el('span', 'profile-actions');
        actions.appendChild(button('quiet', 'Añadir variante', () => addProfileVariant(profile)));
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
    state.authBusy = true;
    updatePresenceActions();
    try {
        setAuthFeedback('Buscando la ubicación…');
        const location = await resolveOnboardingLocation();
        const newsCategories = selectedNewsCategories();
        const descriptors = await enrollment.capture(
            () => state.recognition,
            () => state.recognitionVersion,
            (index, pose, detail) => {
                showEnrollmentStep(index);
                setAuthFeedback(`${index + 1}/${ENROLLMENT_POSES.length} · ${detail || pose.instruction}`);
            });
        const response = await send('POST', '/profiles', {
            displayName: document.getElementById('display-name').value.trim(),
            modelVersion: MODEL_VERSION,
            descriptor: descriptors[0],
        });
        if (response.status >= 300) {
            setAuthFeedback(presenceError(response), true);
            return;
        }

        let captures = 1;
        for (const descriptor of descriptors.slice(1)) {
            const added = await send('POST', `/profiles/${response.body.id}/templates`, {
                modelVersion: MODEL_VERSION,
                descriptor,
            });
            if (added.status >= 300) {
                await api('/profiles/' + response.body.id, { method: 'DELETE' });
                throw new Error(presenceError(added));
            }
            captures++;
        }
        const configured = await send('PUT', `/home/profiles/${response.body.id}`, {
            locationName: location.name,
            latitude: location.latitude,
            longitude: location.longitude,
            timeZone: location.timezone,
            newsCategories,
        });
        if (configured.status >= 300) {
            await api('/profiles/' + response.body.id, { method: 'DELETE' });
            throw new Error(errorMessage(configured));
        }
        document.getElementById('display-name').value = '';
        document.getElementById('home-location').value = '';
        showEnrollmentStep(ENROLLMENT_POSES.length);
        setAuthFeedback(`Onboarding completado para ${location.name} con ${captures} capturas. Reinicia Atlas sin --maintenance.`);
        await refreshAuthentication();
    } catch (error) {
        setAuthFeedback(frontendErrorMessage(error, 'No se pudo capturar el rostro.'), true);
    } finally {
        state.authBusy = false;
        updatePresenceActions();
    }
}

function selectedNewsCategories() {
    return Array.from(document.querySelectorAll('#home-news-categories input:checked'), input => input.value);
}

async function resolveOnboardingLocation() {
    const query = document.getElementById('home-location').value.trim();
    const url = 'https://geocoding-api.open-meteo.com/v1/search?name=' + encodeURIComponent(query)
        + '&count=1&language=es&format=json';
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error('No se pudo consultar la ubicación.');
    }
    const place = (await response.json()).results?.[0];
    if (!place) {
        throw new Error('No se ha encontrado esa ubicación. Añade la ciudad y el país.');
    }

    return {
        name: [place.name, place.admin1, place.country].filter(Boolean)
            .filter((value, index, values) => values.indexOf(value) === index).join(', '),
        latitude: place.latitude,
        longitude: place.longitude,
        timezone: place.timezone,
    };
}

async function deleteProfile(id) {
    const response = await api('/profiles/' + id, { method: 'DELETE' });
    if (response.status === 204) {
        await refreshAuthentication();
    } else {
        setAuthFeedback(presenceError(response), true);
    }
}

async function captureNeutralFaces(count, expiresAt, onProgress) {
    const faces = [];
    let lastVersion = -1;
    while (Date.now() < expiresAt) {
        if (state.recognitionVersion !== lastVersion) {
            lastVersion = state.recognitionVersion;
            const status = faceStatus(state.recognition, { center: state.faceCenter });
            if (status.ready) {
                faces.push(status.face);
                onProgress?.(faces.length, count);
                if (faces.length >= count) {
                    return faces;
                }
            } else {
                faces.length = 0;
                onProgress?.(0, count, status.reason);
            }
        }
        await sleep(40);
    }
    throw new Error('No se han podido obtener capturas frontales estables.');
}

async function calibrateFaceCenter() {
    state.faceCenter = null;
    setAuthFeedback('Mira de frente: calibrando tu posición neutral…');
    const faces = await captureNeutralFaces(5, Date.now() + 5000,
        (current, total, reason) => setAuthFeedback(reason || `Calibrando posición ${current}/${total}…`));
    state.faceCenter = AtlasFaceQuality.calibration(faces);
    return faces;
}

async function addProfileVariant(profile) {
    state.authBusy = true;
    updatePresenceActions();
    try {
        setAuthFeedback(`Cambia tu aspecto si lo necesitas y mira de frente para añadirlo a ${profile.displayName}.`);
        await calibrateFaceCenter();
        const faces = await captureNeutralFaces(3, Date.now() + 5000,
            (current, total, reason) => setAuthFeedback(reason || `Capturando variante ${current}/${total}…`));
        const added = await send('POST', `/profiles/${profile.id}/templates`, {
            modelVersion: MODEL_VERSION,
            descriptor: AtlasFaceQuality.averageDescriptors(faces, state.faceCenter),
        });
        if (added.status >= 300) {
            throw added;
        }
        setAuthFeedback(`Variante añadida al perfil de ${profile.displayName}.`);
        await refreshProfiles();
    } catch (error) {
        setAuthFeedback(error.status ? presenceError(error)
            : frontendErrorMessage(error, 'No se pudo añadir la variante facial.'), true);
    } finally {
        state.authBusy = false;
        updatePresenceActions();
    }
}

async function waitForChallenge(challenge) {
    const detected = challengeDetector(challenge.type);
    const expiresAt = Date.parse(challenge.expiresAt);
    let consecutiveFrames = 0;
    let lastHandVersion = -1;
    while (Date.now() < expiresAt) {
        document.getElementById('challenge-time').textContent = Math.max(0,
            Math.ceil((expiresAt - Date.now()) / 1000)) + ' s';
        if (state.handResultVersion !== lastHandVersion) {
            lastHandVersion = state.handResultVersion;
            const status = faceStatus(state.recognition, { neutral: false });
            const recentFace = faceResultIsRecent(state.recognition);
            const closedFist = detected(state.handResult);
            consecutiveFrames = recentFace && status.ready && closedFist ? consecutiveFrames + 1 : 0;
            setAuthFeedback(!recentFace ? 'Actualizando la captura facial…'
                : !status.ready ? status.reason
                    : !closedFist ? 'Mantén el puño cerrado dentro de la imagen.'
                        : `Confirmando el puño ${consecutiveFrames}/3…`);
            if (consecutiveFrames >= 3) {
                return;
            }
        }
        await sleep(60);
    }
    throw { message: 'El desafío ha caducado. Inténtalo de nuevo.' };
}

async function authenticate() {
    state.authBusy = true;
    updatePresenceActions();
    setAuthFeedback('Preparando prueba de vida…');
    const challengeNode = document.getElementById('auth-challenge');
    try {
        await calibrateFaceCenter();
        const started = await send('POST', '/authentication/challenges', {});
        if (started.status >= 300) {
            throw started;
        }
        const challenge = started.body;
        challengeNode.hidden = false;
        document.getElementById('challenge-instruction').textContent = 'Mantén el puño cerrado';
        setAuthFeedback('Verificando puño y rostro…');
        await waitForChallenge(challenge);
        document.getElementById('challenge-instruction').textContent = 'Suelta el puño y mira de frente';
        const faces = await captureNeutralFaces(3, Date.parse(challenge.expiresAt),
            (current, total, reason) => setAuthFeedback(reason || `Seleccionando captura frontal ${current}/${total}…`));
        const descriptor = AtlasFaceQuality.averageDescriptors(faces, state.faceCenter);
        const completed = await send('POST',
            '/authentication/challenges/' + challenge.challengeId + '/complete', {
                modelVersion: MODEL_VERSION,
                descriptor,
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
            : frontendErrorMessage(error, 'No se pudo completar la autenticación.'), true);
    } finally {
        challengeNode.hidden = true;
        state.authBusy = false;
        updatePresenceActions();
    }
}

function switchView(view) {
    if (!state.authenticated) {
        return;
    }

    state.view = view;
    document.body.classList.toggle('routines-active', view === 'rutinas');
    document.getElementById('view-inicio').hidden = view !== 'inicio';
    document.getElementById('view-agenda').hidden = view !== 'agenda';
    document.getElementById('view-rutinas').hidden = view !== 'rutinas';
    document.getElementById('view-economia').hidden = view !== 'economia';
    document.getElementById('view-nutricion').hidden = view !== 'nutricion';
    document.getElementById('view-entrenamiento').hidden = view !== 'entrenamiento';

    for (const link of document.querySelectorAll('.nav-link[data-view]')) {
        link.classList.toggle('active', link.dataset.view === view);
    }

    if (view === 'agenda') {
        refreshAgenda();
    }
    if (view === 'rutinas') {
        window.AtlasRoutines?.activate();
    }
    if (view === 'economia') {
        window.AtlasEconomy?.activate();
    }
    if (view === 'entrenamiento') {
        window.AtlasTraining?.activate();
    }
    if (view === 'nutricion') {
        window.AtlasNutrition?.activate();
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
    AtlasInteraction.bind({
        video: document.getElementById('mirror'),
        shiftView: offset => switchView(VIEWS[Math.max(0,
            Math.min(VIEWS.length - 1, VIEWS.indexOf(state.view) + offset))]),
        onCancel: () => {
            if (!document.getElementById('panel').hidden) {
                closePanel();
            } else if (state.view !== 'inicio') {
                switchView('inicio');
            }
        },
        scrollRoot: () => (document.getElementById('panel').hidden
            ? null : document.getElementById('panel')),
        scrollFallback: () => document.querySelector(state.view === 'agenda'
            ? '#month-grid' : '#view-inicio'),
    });
    startCamera();
    tickClock();
    setInterval(tickClock, 1000);
    setInterval(refreshWeather, WEATHER_REFRESH);
    setInterval(refreshNews, NEWS_REFRESH);
    refreshAuthentication();
    setInterval(refreshAuthentication, 2000);
    setInterval(refreshAll, 60000);

    for (const link of document.querySelectorAll('.nav-link[data-view]')) {
        link.addEventListener('click', () => switchView(link.dataset.view));
    }
    document.getElementById('authenticate').addEventListener('click', authenticate);
    document.getElementById('enroll').addEventListener('click', enrollProfile);
    document.getElementById('display-name').addEventListener('input', updatePresenceActions);
    document.getElementById('home-location').addEventListener('input', updatePresenceActions);
    document.getElementById('home-news-categories').addEventListener('change', updatePresenceActions);
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
