const MODEL_VERSION = 'human-faceres-3.3.6';
const MIN_CONFIDENCE = 0.6;
const DETECTOR_MIN_FACE_SIZE = AtlasFaceQuality.DETECTOR_MIN_FACE_SIZE;
const CAPTURE_MIN_FACE_SIZE = AtlasFaceQuality.CAPTURE_MIN_FACE_SIZE;
const MAX_FACE_ANGLE = AtlasFaceQuality.MAX_CAPTURE_ANGLE;
const HAND_CONFIDENCE = 0.65;
const GESTURE_HOLD_MS = 180;
const PINCH_HOLD_MS = 100;
const PINCH_DISTANCE_RATIO = 0.4;
const HUMAN_MODELS = 'https://cdn.jsdelivr.net/npm/@vladmandic/human@3.3.6/models/';
const MEDIAPIPE = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/vision_bundle.mjs';
const MEDIAPIPE_WASM = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/wasm';
const GESTURE_MODEL = 'https://storage.googleapis.com/mediapipe-models/gesture_recognizer/'
    + 'gesture_recognizer/float16/1/gesture_recognizer.task';

const HUMAN_CONFIG = {
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
            minConfidence: MIN_CONFIDENCE, minSize: DETECTOR_MIN_FACE_SIZE,
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
};

const HAND_CONFIG = {
    runningMode: 'VIDEO',
    numHands: 1,
    minHandDetectionConfidence: HAND_CONFIDENCE,
    minHandPresenceConfidence: HAND_CONFIDENCE,
    minTrackingConfidence: HAND_CONFIDENCE,
    cannedGesturesClassifierOptions: { maxResults: 1, scoreThreshold: HAND_CONFIDENCE },
};

const HAND_CONNECTIONS = [
    [0, 1], [1, 2], [2, 3], [3, 4],
    [0, 5], [5, 6], [6, 7], [7, 8],
    [5, 9], [9, 10], [10, 11], [11, 12],
    [9, 13], [13, 14], [14, 15], [15, 16],
    [13, 17], [17, 18], [18, 19], [19, 20], [17, 0],
];

const FINGERS = [
    ['Índice', 5], ['Medio', 9], ['Anular', 13], ['Meñique', 17],
];

const FACE_TESTS = [
    {
        id: 'F01', kind: 'BASELINE', evaluator: 'valid', title: 'Rostro frontal',
        instruction: 'Mira directamente a la cámara, mantén una expresión neutra y no te muevas.',
        note: 'Establece la referencia de confianza, liveness, antispoof y latencia.',
    },
    {
        id: 'F02', kind: 'GAFAS', evaluator: 'valid', title: 'Cambio de gafas',
        instruction: 'Cambia tu estado actual: ponte las gafas si no las llevas o quítatelas si las llevas.',
        note: 'Puedes saltarla si no tienes gafas disponibles.', optional: true,
    },
    {
        id: 'F03', kind: 'YAW', evaluator: 'yaw', direction: -1, title: 'Giro hacia la izquierda',
        instruction: 'Gira lentamente la cabeza hacia tu izquierda y mantenla ligeramente girada.',
        note: 'No hace falta llegar al perfil completo.',
    },
    {
        id: 'F04', kind: 'YAW', evaluator: 'yaw', direction: 1, title: 'Giro hacia la derecha',
        instruction: 'Gira lentamente la cabeza hacia tu derecha y mantenla ligeramente girada.',
        note: 'Registraremos el rango firmado aunque la cámara esté espejada.',
    },
    {
        id: 'F05', kind: 'PITCH', evaluator: 'pitch', direction: -1, title: 'Mirada hacia arriba',
        instruction: 'Levanta ligeramente la barbilla y mira hacia arriba.',
        note: 'Mantén los ojos y la mayor parte del rostro visibles.',
    },
    {
        id: 'F06', kind: 'PITCH', evaluator: 'pitch', direction: 1, title: 'Mirada hacia abajo',
        instruction: 'Baja ligeramente la barbilla y mira hacia abajo.',
        note: 'No ocultes completamente los ojos.',
    },
    {
        id: 'F07', kind: 'ROLL', evaluator: 'roll', title: 'Inclinación lateral',
        instruction: 'Inclina la cabeza hacia un hombro, vuelve al centro y después hacia el otro.',
        note: 'Haz el recorrido despacio durante toda la captura.',
    },
    {
        id: 'F08', kind: 'DISTANCIA', evaluator: 'distance', title: 'Límite de distancia',
        instruction: 'Empieza en tu posición normal y aléjate lentamente hasta que la captura deje de ser válida.',
        note: `Buscamos el límite operativo de ${CAPTURE_MIN_FACE_SIZE} px, no que desaparezcas de golpe.`,
    },
    {
        id: 'F09', kind: 'OCLUSIÓN', evaluator: 'occlusion', title: 'Rostro parcialmente cubierto',
        instruction: 'Cubre primero parte de la boca y después un ojo con la mano.',
        note: 'Hand debe permanecer activo. La captura debe detectar la mano sobre el rostro y rechazarla.',
    },
    {
        id: 'F10', kind: 'AUSENCIA', evaluator: 'absent', title: 'Salida del encuadre',
        instruction: 'Sal completamente del encuadre y deja la cámara sin ningún rostro.',
        note: 'Debe desaparecer cualquier detección facial.',
    },
    {
        id: 'F11', kind: 'RECUPERACIÓN', evaluator: 'recovery', title: 'Regreso al encuadre',
        instruction: 'Vuelve a tu posición frontal habitual y permanece quieto.',
        note: 'Medimos si el detector recupera una captura válida.',
    },
    {
        id: 'F12', kind: 'ANTISPOOF', evaluator: 'spoof', title: 'Fotografía o pantalla',
        instruction: 'Sal del encuadre y muestra a cámara una fotografía clara de un rostro o una imagen en otra pantalla.',
        note: `Acerca la imagen hasta superar ${CAPTURE_MIN_FACE_SIZE} px. Si no alcanza ese tamaño, la prueba será inconcluyente.`, optional: true,
    },
];

const FACE_TEST_DURATION_MS = 5000;
const HAND_TEST_DURATION_MS = 4000;
const HAND_TESTS = [
    {
        id: 'H01', kind: 'TRACKING', evaluator: 'tracking', title: 'Mano relajada',
        instruction: 'Muestra una mano completa, relajada y centrada, con los dedos ligeramente separados.',
        note: 'Establece la referencia de detección, landmarks, lateralidad y latencia.',
    },
    {
        id: 'H02', kind: 'GESTO', evaluator: 'gesture', expected: 'FIST', title: 'Puño cerrado',
        instruction: 'Cierra el puño con el pulgar visible y mantén la mano orientada hacia la cámara.',
        note: 'Atlas utiliza FIST para autenticar y cerrar sesión.',
    },
    {
        id: 'H03', kind: 'GESTO', evaluator: 'gesture', expected: 'OPEN_PALM', title: 'Palma abierta',
        instruction: 'Abre completamente la mano, separa los dedos y orienta la palma hacia la cámara.',
        note: 'Atlas utiliza OPEN_PALM para detener voz, cerrar paneles o volver a Inicio.',
    },
    {
        id: 'H04', kind: 'GESTO', evaluator: 'gesture', expected: 'PINCH', title: 'Pinch',
        instruction: 'Junta suavemente las puntas del pulgar y el índice; deja los demás dedos en una posición natural.',
        note: `El umbral activo es una distancia thumb–index ≤ ${PINCH_DISTANCE_RATIO} veces el ancho de palma.`,
    },
    {
        id: 'H05', kind: 'GESTO', evaluator: 'gesture', expected: 'POINT', title: 'Apuntar',
        instruction: 'Extiende solo el índice y mantén plegados el medio, anular y meñique.',
        note: 'Mide la selección y el seguimiento del puntero.',
    },
    {
        id: 'H06', kind: 'NAVEGACIÓN', evaluator: 'gesture', expected: 'PALM_LEFT', title: 'Dirección izquierda',
        instruction: 'Extiende juntos índice y medio y oriéntalos claramente hacia la izquierda de la pantalla.',
        note: 'Mantén anular y meñique plegados.',
    },
    {
        id: 'H07', kind: 'NAVEGACIÓN', evaluator: 'gesture', expected: 'PALM_RIGHT', title: 'Dirección derecha',
        instruction: 'Extiende juntos índice y medio y oriéntalos claramente hacia la derecha de la pantalla.',
        note: 'Mantén anular y meñique plegados.',
    },
    {
        id: 'H08', kind: 'NAVEGACIÓN', evaluator: 'gesture', expected: 'PALM_UP', title: 'Dirección arriba',
        instruction: 'Extiende juntos índice y medio y oriéntalos claramente hacia arriba.',
        note: 'Evita dejar los dedos en diagonal.',
    },
    {
        id: 'H09', kind: 'NAVEGACIÓN', evaluator: 'gesture', expected: 'PALM_DOWN', title: 'Dirección abajo',
        instruction: 'Extiende juntos índice y medio y oriéntalos claramente hacia abajo.',
        note: 'Evita dejar los dedos en diagonal.',
    },
    {
        id: 'H10', kind: 'DISTANCIA', evaluator: 'distance', title: 'Límite de distancia',
        instruction: 'Empieza con la mano cerca y aléjala lentamente hasta que el tracking se vuelva inestable o desaparezca.',
        note: 'Registraremos el tamaño normalizado y el punto de pérdida.',
    },
    {
        id: 'H11', kind: 'AUSENCIA', evaluator: 'absent', title: 'Salida del encuadre',
        instruction: 'Retira completamente la mano y deja el encuadre sin manos.',
        note: 'El detector debe dejar de producir landmarks.',
    },
    {
        id: 'H12', kind: 'RECUPERACIÓN', evaluator: 'recovery', expected: 'OPEN_PALM', title: 'Regreso al encuadre',
        instruction: 'Empieza sin mano. Pulsa registrar y después vuelve mostrando una palma abierta.',
        note: 'La captura comenzará automáticamente al recuperar los 21 landmarks.',
    },
];

const dom = {};
const state = {
    stream: null,
    human: null,
    handRecognizer: null,
    faceEnabled: true,
    handEnabled: true,
    faceModelReady: false,
    handModelReady: false,
    faceModelPromise: null,
    handModelPromise: null,
    loading: false,
    running: false,
    runId: 0,
    faceResult: null,
    handResult: null,
    faceLatency: null,
    handLatency: null,
    faceFrames: [],
    handFrames: [],
    lastHandVideoTime: -1,
    lastFaceTelemetry: 0,
    lastHandTelemetry: 0,
    previousFaceState: null,
    previousHandState: null,
    previousGesture: null,
    gesture: { candidate: null, candidateSince: 0, missingSince: 0, latched: null },
    faceTests: {
        active: false, index: 0, phase: 'idle', samples: [], results: [],
        captureStarted: 0, waitStarted: 0, recoveryMs: null, calibration: null,
        captureTimeout: null, timerInterval: null,
    },
    handTests: {
        active: false, index: 0, phase: 'idle', samples: [], results: [],
        captureStarted: 0, waitStarted: 0, recoveryMs: null,
        captureTimeout: null, timerInterval: null,
    },
};

document.addEventListener('DOMContentLoaded', () => {
    for (const id of [
        'camera', 'overlay', 'stage-empty', 'pipeline-message', 'camera-select', 'start', 'stop', 'clear-log',
        'face-enabled', 'hand-enabled', 'face-panel', 'hand-panel', 'gesture-panel',
        'face-badge', 'hand-badge', 'gesture-badge', 'resolution', 'face-fps', 'hand-fps', 'face-latency',
        'hand-latency', 'video-time', 'face-state', 'face-crop', 'face-reason', 'face-metrics', 'face-gestures',
        'face-raw', 'hand-state', 'hand-crop', 'hand-reason', 'hand-metrics', 'hand-analysis', 'hand-landmarks',
        'hand-world-landmarks', 'hand-raw', 'atlas-gesture', 'hold-label', 'hold-progress', 'gesture-metrics',
        'gesture-reason', 'thresholds', 'model-state', 'system-metrics', 'event-log', 'model-config',
        'face-test-suite', 'face-test-progress', 'start-face-tests', 'cancel-face-tests', 'face-test-step',
        'face-test-id', 'face-test-kind', 'face-test-title', 'face-test-instruction', 'face-test-note',
        'face-test-phase', 'capture-face-test', 'next-face-test', 'repeat-face-test', 'skip-face-test',
        'face-test-capture', 'face-test-countdown', 'face-test-timer', 'face-test-feedback',
        'face-test-results', 'face-test-export', 'face-test-summary', 'copy-face-results',
        'download-face-results',
        'hand-test-suite', 'hand-test-progress', 'start-hand-tests', 'cancel-hand-tests', 'hand-test-step',
        'hand-test-id', 'hand-test-kind', 'hand-test-title', 'hand-test-instruction', 'hand-test-note',
        'hand-test-phase', 'capture-hand-test', 'next-hand-test', 'repeat-hand-test', 'skip-hand-test',
        'hand-test-capture', 'hand-test-countdown', 'hand-test-timer', 'hand-test-feedback',
        'hand-test-results', 'hand-test-export', 'hand-test-summary', 'copy-hand-results',
        'download-hand-results',
    ]) {
        dom[toCamel(id)] = document.getElementById(id);
    }

    dom.start.addEventListener('click', start);
    dom.stop.addEventListener('click', stop);
    dom.clearLog.addEventListener('click', () => dom.eventLog.replaceChildren());
    dom.cameraSelect.addEventListener('change', () => state.running && startStream());
    dom.faceEnabled.addEventListener('change', () => setPipelineEnabled('face'));
    dom.handEnabled.addEventListener('change', () => setPipelineEnabled('hand'));
    dom.startFaceTests.addEventListener('click', startFaceTests);
    dom.cancelFaceTests.addEventListener('click', () => cancelFaceTests('Batería cancelada.'));
    dom.captureFaceTest.addEventListener('click', captureFaceTest);
    dom.nextFaceTest.addEventListener('click', nextFaceTest);
    dom.repeatFaceTest.addEventListener('click', showFaceTestStep);
    dom.skipFaceTest.addEventListener('click', skipFaceTest);
    dom.copyFaceResults.addEventListener('click', copyFaceResults);
    dom.downloadFaceResults.addEventListener('click', downloadFaceResults);
    dom.startHandTests.addEventListener('click', startHandTests);
    dom.cancelHandTests.addEventListener('click', () => cancelHandTests('Batería cancelada.'));
    dom.captureHandTest.addEventListener('click', captureHandTest);
    dom.nextHandTest.addEventListener('click', nextHandTest);
    dom.repeatHandTest.addEventListener('click', showHandTestStep);
    dom.skipHandTest.addEventListener('click', skipHandTest);
    dom.copyHandResults.addEventListener('click', copyHandResults);
    dom.downloadHandResults.addEventListener('click', downloadHandResults);
    window.addEventListener('beforeunload', stopStream);
    renderThresholds();
    renderSystem();
    renderFaceTestProgress();
    renderHandTestProgress();
    AtlasFaceQuality.selfCheck();
    faceTestEvaluatorSelfCheck();
    handTestEvaluatorSelfCheck();
    handPoseSelfCheck();
    dom.modelConfig.textContent = JSON.stringify({
        face: { modelVersion: MODEL_VERSION, ...HUMAN_CONFIG },
        hand: { model: GESTURE_MODEL, wasm: MEDIAPIPE_WASM, ...HAND_CONFIG },
    }, null, 2);
});

function toCamel(value) {
    return value.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
}

async function start() {
    if (!navigator.mediaDevices?.getUserMedia || state.faceEnabled && !window.Human?.Human) {
        fail('No se puede cargar la visión artificial o el navegador no expone la cámara.');
        return;
    }
    if (!state.faceEnabled && !state.handEnabled) {
        setPipeline('Activa Face o Hand antes de iniciar la cámara.');
        return;
    }

    state.loading = true;
    dom.start.disabled = true;
    setPipeline('Solicitando acceso a la cámara…');
    try {
        await startStream();
        await loadEnabledModels();
        state.running = true;
        state.runId += 1;
        state.lastHandVideoTime = -1;
        dom.stop.disabled = false;
        dom.stageEmpty.hidden = true;
        setPipeline('Pipeline activo. Muestra el rostro o una mano dentro del encuadre.');
        logEvent('PIPELINE', 'Captura iniciada');
        detectFaces(state.runId);
        detectHands(state.runId);
        updateFaceTestAvailability();
        updateHandTestAvailability();
    } catch (error) {
        stopStream();
        fail(cameraErrorMessage(error));
    } finally {
        state.loading = false;
        updateStartState();
    }
}

async function startStream() {
    stopStream();
    const deviceId = dom.cameraSelect.value;
    state.stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {
            deviceId: deviceId ? { exact: deviceId } : undefined,
            facingMode: deviceId ? undefined : 'user',
            width: { ideal: 960 },
            height: { ideal: 720 },
        },
    });
    dom.camera.srcObject = state.stream;
    await dom.camera.play();
    await populateCameras();
    renderSystem();
}

async function populateCameras() {
    const selected = dom.cameraSelect.value;
    const devices = (await navigator.mediaDevices.enumerateDevices()).filter(device => device.kind === 'videoinput');
    dom.cameraSelect.replaceChildren(new Option('Cámara predeterminada', ''));
    devices.forEach((device, index) => dom.cameraSelect.add(new Option(device.label || `Cámara ${index + 1}`, device.deviceId)));
    dom.cameraSelect.value = devices.some(device => device.deviceId === selected) ? selected : '';
    dom.cameraSelect.disabled = devices.length < 2;
}

async function loadEnabledModels() {
    setModelState('LOADING', 'warn');
    if (state.faceEnabled) {
        await loadFaceModel();
    }
    if (state.handEnabled) {
        await loadHandModel();
    }
    renderModelState();
    renderSystem();
}

async function loadFaceModel() {
    if (state.faceModelReady) {
        return;
    }
    if (!state.faceModelPromise) {
        state.faceModelPromise = (async () => {
            setPipeline('Cargando Human y los modelos faciales…');
            state.human = new window.Human.Human(HUMAN_CONFIG);
            const started = performance.now();
            await state.human.load();
            await state.human.warmup();
            state.faceModelReady = true;
            logEvent('MODEL', `Human listo en ${formatMs(performance.now() - started)}`);
        })().finally(() => state.faceModelPromise = null);
    }
    await state.faceModelPromise;
}

async function loadHandModel() {
    if (state.handModelReady) {
        return;
    }
    if (!state.handModelPromise) {
        state.handModelPromise = (async () => {
            setPipeline('Cargando MediaPipe Gesture Recognizer…');
            const started = performance.now();
            const { FilesetResolver, GestureRecognizer } = await import(MEDIAPIPE);
            const vision = await FilesetResolver.forVisionTasks(MEDIAPIPE_WASM);
            state.handRecognizer = await GestureRecognizer.createFromOptions(vision, {
                baseOptions: { modelAssetPath: GESTURE_MODEL },
                ...HAND_CONFIG,
            });
            state.handModelReady = true;
            logEvent('MODEL', `MediaPipe listo en ${formatMs(performance.now() - started)}`);
        })().finally(() => state.handModelPromise = null);
    }
    await state.handModelPromise;
}

async function setPipelineEnabled(name) {
    const enabled = dom[`${name}Enabled`].checked;
    if (name === 'face' && !enabled && state.faceTests.active) {
        cancelFaceTests('Batería cancelada porque Face se ha desactivado.');
    }
    if (name === 'hand' && !enabled && state.handTests.active) {
        cancelHandTests('Batería cancelada porque Hand se ha desactivado.');
    }
    state[`${name}Enabled`] = enabled;
    dom[`${name}Panel`].hidden = !enabled;
    dom[`${name}Badge`].hidden = !enabled;
    if (name === 'hand') {
        dom.gesturePanel.hidden = !enabled;
        dom.gestureBadge.hidden = !enabled;
    }
    resetPipeline(name);
    renderPerformance();
    renderModelState();
    renderSystem();
    updateStartState();
    updateFaceTestAvailability();
    updateHandTestAvailability();
    logEvent('PIPELINE', `${name.toUpperCase()} ${enabled ? 'activado' : 'desactivado'}`);

    if (!state.running) {
        setPipeline(enabled ? `${name.toUpperCase()} preparado para la próxima captura.`
            : activePipelineMessage());
        return;
    }
    if (!enabled) {
        setPipeline(activePipelineMessage());
        return;
    }

    try {
        setModelState('LOADING', 'warn');
        await (name === 'face' ? loadFaceModel() : loadHandModel());
        renderModelState();
        renderSystem();
        setPipeline(activePipelineMessage());
        updateFaceTestAvailability();
        updateHandTestAvailability();
    } catch (error) {
        dom[`${name}Enabled`].checked = false;
        state[`${name}Enabled`] = false;
        dom[`${name}Panel`].hidden = true;
        dom[`${name}Badge`].hidden = true;
        if (name === 'hand') {
            dom.gesturePanel.hidden = true;
            dom.gestureBadge.hidden = true;
        }
        fail(cameraErrorMessage(error));
        updateStartState();
        updateFaceTestAvailability();
        updateHandTestAvailability();
    }
}

function resetPipeline(name) {
    if (name === 'face') {
        state.faceResult = null;
        state.faceFrames.length = 0;
        state.faceLatency = null;
        state.previousFaceState = null;
        clearCanvas(dom.faceCrop);
        setState(dom.faceState, state.faceEnabled ? 'SIN DATOS' : 'OFF', 'idle');
    } else {
        state.handResult = null;
        state.handFrames.length = 0;
        state.handLatency = null;
        state.previousHandState = null;
        state.gesture = { candidate: null, candidateSince: 0, missingSince: 0, latched: null };
        clearCanvas(dom.handCrop);
        setState(dom.handState, state.handEnabled ? 'SIN DATOS' : 'OFF', 'idle');
        renderGesture(null, 0, GESTURE_HOLD_MS, 'Hand está desactivado.');
    }
    renderOverlay();
}

function activePipelineMessage() {
    const active = [state.faceEnabled && 'FACE', state.handEnabled && 'HAND'].filter(Boolean);
    return active.length ? `Pipeline activo: ${active.join(' + ')}.` : 'Todos los pipelines están desactivados.';
}

function updateStartState() {
    dom.start.disabled = state.running || state.loading || !state.faceEnabled && !state.handEnabled;
}

function renderModelState() {
    const ready = (!state.faceEnabled || state.faceModelReady)
        && (!state.handEnabled || state.handModelReady);
    const active = [state.faceEnabled && 'FACE', state.handEnabled && 'HAND'].filter(Boolean).join(' + ');
    setModelState(!active ? 'PAUSED' : ready ? `READY ${active}` : 'NO CARGADO', ready && active ? 'ready' : 'idle');
}

function stop() {
    if (state.faceTests.active) {
        cancelFaceTests('Batería cancelada porque la cámara se ha detenido.');
    }
    if (state.handTests.active) {
        cancelHandTests('Batería cancelada porque la cámara se ha detenido.');
    }
    state.running = false;
    state.runId += 1;
    stopStream();
    clearCanvas(dom.overlay);
    clearCanvas(dom.faceCrop);
    clearCanvas(dom.handCrop);
    dom.stageEmpty.hidden = false;
    updateStartState();
    dom.stop.disabled = true;
    setPipeline('Captura detenida. Los modelos permanecen cargados.');
    logEvent('PIPELINE', 'Captura detenida');
    updateFaceTestAvailability();
    updateHandTestAvailability();
}

function stopStream() {
    state.stream?.getTracks().forEach(track => track.stop());
    state.stream = null;
    dom.camera && (dom.camera.srcObject = null);
}

async function detectFaces(runId) {
    if (!state.running || runId !== state.runId) {
        return;
    }
    if (!state.faceEnabled || !state.faceModelReady) {
        requestAnimationFrame(() => detectFaces(runId));
        return;
    }
    const started = performance.now();
    try {
        state.faceResult = await state.human.detect(dom.camera);
        const ended = performance.now();
        state.faceLatency = average(state.faceLatency, ended - started);
        markFrame(state.faceFrames, ended);
        recordFaceTestSample(state.faceResult, ended);
        renderFace(state.faceResult);
    } catch (error) {
        logEvent('FACE ERROR', error.message || String(error));
    }
    requestAnimationFrame(() => detectFaces(runId));
}

function detectHands(runId) {
    if (!state.running || runId !== state.runId) {
        return;
    }
    if (!state.handEnabled || !state.handModelReady) {
        requestAnimationFrame(() => detectHands(runId));
        return;
    }
    try {
        if (dom.camera.currentTime !== state.lastHandVideoTime) {
            state.lastHandVideoTime = dom.camera.currentTime;
            const started = performance.now();
            state.handResult = state.handRecognizer.recognizeForVideo(dom.camera, started);
            const ended = performance.now();
            state.handLatency = average(state.handLatency, ended - started);
            markFrame(state.handFrames, ended);
            renderHand(state.handResult, ended);
            recordHandTestSample(state.handResult, ended, ended - started);
            renderOverlay();
            renderPerformance();
        }
    } catch (error) {
        logEvent('HAND ERROR', error.message || String(error));
    }
    requestAnimationFrame(() => detectHands(runId));
}

async function startFaceTests() {
    if (!state.running || !state.faceEnabled || !state.faceModelReady || state.handTests.active) {
        dom.faceTestFeedback.textContent = state.handTests.active
            ? 'Termina o cancela la batería Hand antes de comenzar.'
            : 'Inicia la cámara con Face activo antes de comenzar.';
        return;
    }
    clearFaceTestTimers();
    state.faceTests.active = true;
    state.faceTests.index = 0;
    state.faceTests.phase = 'ready';
    state.faceTests.samples = [];
    state.faceTests.results = [];
    state.faceTests.calibration = null;
    dom.faceTestResults.replaceChildren(emptyFaceTestRow());
    dom.faceTestExport.hidden = true;
    dom.startFaceTests.hidden = true;
    dom.cancelFaceTests.hidden = false;
    logEvent('FACE TEST', 'Batería guiada iniciada');
    showFaceTestStep();
    updateHandTestAvailability();
}

function showFaceTestStep() {
    if (!state.faceTests.active) {
        return;
    }
    clearFaceTestTimers();
    state.faceTests.phase = 'ready';
    state.faceTests.samples = [];
    const test = FACE_TESTS[state.faceTests.index];
    dom.faceTestStep.hidden = false;
    dom.faceTestCapture.hidden = true;
    dom.faceTestId.textContent = test.id;
    dom.faceTestKind.textContent = test.kind + (test.optional ? ' / OPCIONAL' : '');
    dom.faceTestTitle.textContent = test.title;
    dom.faceTestInstruction.textContent = test.instruction;
    dom.faceTestNote.textContent = test.note;
    setState(dom.faceTestPhase, 'PREPARACIÓN', 'idle');
    dom.captureFaceTest.hidden = false;
    dom.captureFaceTest.disabled = false;
    dom.nextFaceTest.hidden = true;
    dom.repeatFaceTest.hidden = true;
    dom.skipFaceTest.hidden = false;
    dom.faceTestFeedback.textContent = 'Colócate como indica la prueba y pulsa “Registrar 5 segundos” cuando estés preparado.';
    renderFaceTestProgress();
}

async function captureFaceTest() {
    if (!state.faceTests.active || state.faceTests.phase !== 'ready' || !state.running) {
        return;
    }
    state.faceTests.samples = [];
    state.faceTests.recoveryMs = null;
    const test = FACE_TESTS[state.faceTests.index];
    if (test.evaluator === 'occlusion' && (!state.handEnabled || !state.handModelReady)) {
        dom.handEnabled.checked = true;
        await setPipelineEnabled('hand');
        if (!state.handEnabled || !state.handModelReady) {
            dom.faceTestFeedback.textContent = 'No se pudo activar Hand; la prueba de oclusión no puede comenzar.';
            return;
        }
    }
    if (test.evaluator === 'recovery') {
        state.faceTests.phase = 'waiting';
        state.faceTests.waitStarted = performance.now();
        dom.faceTestCapture.hidden = false;
        dom.faceTestTimer.max = FACE_TEST_DURATION_MS;
        dom.faceTestTimer.value = 0;
        dom.faceTestCountdown.textContent = 'Esperando rostro…';
        dom.captureFaceTest.disabled = true;
        dom.skipFaceTest.hidden = true;
        dom.faceEnabled.disabled = true;
        dom.handEnabled.disabled = true;
        setState(dom.faceTestPhase, 'ESPERANDO', 'warn');
        dom.faceTestFeedback.textContent = 'Vuelve al encuadre. La medición comenzará automáticamente al detectar tu rostro.';
        state.faceTests.captureTimeout = setTimeout(finishFaceTestCapture, 10000);
        return;
    }
    beginFaceTestCapture(performance.now());
}

function beginFaceTestCapture(startedAt) {
    clearFaceTestTimers();
    state.faceTests.phase = 'capturing';
    state.faceTests.captureStarted = startedAt;
    dom.faceTestCapture.hidden = false;
    dom.faceTestTimer.max = FACE_TEST_DURATION_MS;
    dom.faceTestTimer.value = 0;
    dom.captureFaceTest.disabled = true;
    dom.skipFaceTest.hidden = true;
    dom.faceEnabled.disabled = true;
    dom.handEnabled.disabled = true;
    setState(dom.faceTestPhase, 'CAPTURANDO', 'warn');
    dom.faceTestFeedback.textContent = 'Mantén la acción hasta que termine la barra de captura.';
    updateFaceTestTimer();
    state.faceTests.timerInterval = setInterval(updateFaceTestTimer, 50);
    state.faceTests.captureTimeout = setTimeout(finishFaceTestCapture, FACE_TEST_DURATION_MS);
}

function updateFaceTestTimer() {
    const elapsed = Math.min(FACE_TEST_DURATION_MS, performance.now() - state.faceTests.captureStarted);
    dom.faceTestTimer.value = elapsed;
    dom.faceTestCountdown.textContent = `${Math.max(0, (FACE_TEST_DURATION_MS - elapsed) / 1000).toFixed(1)} s`;
}

function recordFaceTestSample(result, observedAt) {
    if (!state.faceTests.active || !['waiting', 'capturing'].includes(state.faceTests.phase)) {
        return;
    }
    if (state.faceTests.phase === 'waiting') {
        if (result?.face?.length !== 1) {
            return;
        }
        state.faceTests.recoveryMs = observedAt - state.faceTests.waitStarted;
        beginFaceTestCapture(observedAt);
        dom.faceTestFeedback.textContent = `Rostro recuperado en ${formatMs(state.faceTests.recoveryMs)}. Midiendo estabilidad…`;
    }
    const status = faceStatus(result);
    const face = status.face;
    const angle = face?.rotation?.angle || {};
    state.faceTests.samples.push({
        atMs: Number((observedAt - state.faceTests.captureStarted).toFixed(1)),
        faceCount: result?.face?.length || 0,
        ready: status.ready,
        reason: status.reason,
        confidence: finiteOrNull(face && (face.faceScore || face.boxScore || 0)),
        real: finiteOrNull(face?.real),
        live: finiteOrNull(face?.live),
        passiveAccepted: Boolean(face && (face.real ?? 0) >= MIN_CONFIDENCE && (face.live ?? 0) >= MIN_CONFIDENCE),
        occluded: Boolean(status.occluded || face && AtlasFaceQuality.handOccludesFace(face, state.handResult)),
        faceSize: face?.box ? Number(Math.min(face.box[2], face.box[3]).toFixed(1)) : null,
        yaw: finiteOrNull(angle.yaw),
        pitch: finiteOrNull(angle.pitch),
        roll: finiteOrNull(angle.roll),
        latencyMs: finiteOrNull(state.faceLatency),
        faceFps: fps(state.faceFrames),
    });
}

function finishFaceTestCapture() {
    if (!state.faceTests.active || !['waiting', 'capturing'].includes(state.faceTests.phase)) {
        return;
    }
    clearFaceTestTimers();
    state.faceTests.phase = 'result';
    dom.faceEnabled.disabled = false;
    dom.handEnabled.disabled = false;
    dom.faceTestTimer.value = FACE_TEST_DURATION_MS;
    dom.faceTestCountdown.textContent = '0.0 s';
    const test = FACE_TESTS[state.faceTests.index];
    const samples = state.faceTests.samples.slice();
    const summary = summarizeFaceSamples(samples);
    summary.recoveryMs = finiteOrNull(state.faceTests.recoveryMs);
    if (test.id === 'F01' && summary.facePresentRate > .5) {
        state.faceTests.calibration = {
            yaw: summary.yaw.mean,
            pitch: summary.pitch.mean,
            roll: summary.roll.mean,
        };
    }
    const evaluation = evaluateFaceTest(test, summary);
    const result = {
        id: test.id,
        kind: test.kind,
        title: test.title,
        instruction: test.instruction,
        capturedAt: new Date().toISOString(),
        recoveryMs: state.faceTests.recoveryMs,
        evaluation,
        summary,
        samples,
    };
    state.faceTests.results = state.faceTests.results.filter(existing => existing.id !== test.id);
    state.faceTests.results.push(result);
    renderFaceTestResults();
    setState(dom.faceTestPhase, evaluation.status, faceTestStatusClass(evaluation.status));
    dom.faceTestFeedback.textContent = evaluation.message;
    dom.captureFaceTest.hidden = true;
    dom.nextFaceTest.hidden = false;
    dom.nextFaceTest.textContent = state.faceTests.index === FACE_TESTS.length - 1 ? 'Finalizar batería' : 'Siguiente prueba';
    dom.repeatFaceTest.hidden = false;
    dom.skipFaceTest.hidden = true;
    logEvent('FACE TEST', `${test.id} ${evaluation.status} · ${evaluation.message}`);
}

function nextFaceTest() {
    if (!state.faceTests.active || state.faceTests.phase !== 'result') {
        return;
    }
    if (state.faceTests.index >= FACE_TESTS.length - 1) {
        completeFaceTests();
        return;
    }
    state.faceTests.index += 1;
    showFaceTestStep();
}

function skipFaceTest() {
    if (!state.faceTests.active || state.faceTests.phase !== 'ready') {
        return;
    }
    const test = FACE_TESTS[state.faceTests.index];
    state.faceTests.results = state.faceTests.results.filter(existing => existing.id !== test.id);
    state.faceTests.results.push({
        id: test.id, kind: test.kind, title: test.title, instruction: test.instruction,
        capturedAt: new Date().toISOString(),
        evaluation: { status: 'SKIPPED', message: 'Prueba omitida por el usuario.' },
        summary: null, samples: [],
    });
    renderFaceTestResults();
    logEvent('FACE TEST', `${test.id} omitida`);
    if (state.faceTests.index >= FACE_TESTS.length - 1) {
        completeFaceTests();
    } else {
        state.faceTests.index += 1;
        showFaceTestStep();
    }
}

function cancelFaceTests(message) {
    clearFaceTestTimers();
    state.faceTests.active = false;
    state.faceTests.phase = 'idle';
    dom.faceEnabled.disabled = false;
    dom.handEnabled.disabled = false;
    dom.faceTestStep.hidden = true;
    dom.faceTestCapture.hidden = true;
    dom.startFaceTests.hidden = false;
    dom.cancelFaceTests.hidden = true;
    dom.faceTestExport.hidden = state.faceTests.results.length === 0;
    dom.faceTestFeedback.textContent = message;
    renderFaceTestSummary();
    renderFaceTestProgress();
    updateHandTestAvailability();
}

function completeFaceTests() {
    clearFaceTestTimers();
    state.faceTests.active = false;
    state.faceTests.phase = 'complete';
    dom.faceTestStep.hidden = true;
    dom.faceTestCapture.hidden = true;
    dom.startFaceTests.hidden = false;
    dom.cancelFaceTests.hidden = true;
    dom.faceTestExport.hidden = false;
    dom.faceTestFeedback.textContent = 'Batería completada. Copia o descarga el JSON y pásamelo para analizarlo.';
    renderFaceTestSummary();
    renderFaceTestProgress();
    updateHandTestAvailability();
    logEvent('FACE TEST', 'Batería guiada completada');
}

function clearFaceTestTimers() {
    clearTimeout(state.faceTests.captureTimeout);
    clearInterval(state.faceTests.timerInterval);
    state.faceTests.captureTimeout = null;
    state.faceTests.timerInterval = null;
}

function summarizeFaceSamples(samples) {
    const present = samples.filter(sample => sample.faceCount > 0);
    const ready = samples.filter(sample => sample.ready);
    const values = key => present.map(sample => sample[key]).filter(Number.isFinite);
    const angle = key => range(values(key));
    const lastAt = samples.at(-1)?.atMs || 0;
    const tail = samples.filter(sample => sample.atMs >= Math.max(0, lastAt - 2000));
    const reasons = {};
    samples.forEach(sample => reasons[sample.reason] = (reasons[sample.reason] || 0) + 1);
    return {
        sampleCount: samples.length,
        facePresentRate: ratio(present.length, samples.length),
        singleFaceRate: ratio(samples.filter(sample => sample.faceCount === 1).length, samples.length),
        multipleFaceRate: ratio(samples.filter(sample => sample.faceCount > 1).length, samples.length),
        readyRate: ratio(ready.length, samples.length),
        readyWhenPresentRate: ratio(ready.length, present.length),
        passiveAcceptedRate: ratio(samples.filter(sample => sample.passiveAccepted).length, samples.length),
        passiveWhenPresentRate: ratio(present.filter(sample => sample.passiveAccepted).length, present.length),
        occlusionRate: ratio(samples.filter(sample => sample.occluded).length, samples.length),
        tailFacePresentRate: ratio(tail.filter(sample => sample.faceCount > 0).length, tail.length),
        tailReadyRate: ratio(tail.filter(sample => sample.ready).length, tail.length),
        firstFaceAtMs: present[0]?.atMs ?? null,
        lastFaceAtMs: present.at(-1)?.atMs ?? null,
        firstReadyAtMs: samples.find(sample => sample.ready)?.atMs ?? null,
        confidenceMean: mean(values('confidence')),
        realMean: mean(values('real')),
        liveMean: mean(values('live')),
        faceSize: range(values('faceSize')),
        yaw: angle('yaw'),
        pitch: angle('pitch'),
        roll: angle('roll'),
        latencyMedianMs: median(samples.map(sample => sample.latencyMs).filter(Number.isFinite)),
        faceFpsMean: mean(samples.map(sample => sample.faceFps).filter(Number.isFinite)),
        rejectionReasons: reasons,
    };
}

function evaluateFaceTest(test, summary) {
    if (!summary.sampleCount) {
        return { status: 'FAIL', message: 'No se ha registrado ninguna muestra facial.' };
    }
    if (test.evaluator === 'valid') {
        return summary.readyRate >= .7
            ? { status: 'PASS', message: `Captura válida en ${percent(summary.readyRate)} de las muestras.` }
            : summary.readyRate >= .4
                ? { status: 'WARN', message: `Captura inestable: solo ${percent(summary.readyRate)} de muestras válidas.` }
                : { status: 'FAIL', message: `Captura válida únicamente en ${percent(summary.readyRate)} de las muestras.` };
    }
    if (['yaw', 'pitch', 'roll'].includes(test.evaluator)) {
        const center = state.faceTests.calibration?.[test.evaluator] || 0;
        const motion = test.direction === -1 ? center - (summary[test.evaluator].min ?? center)
            : test.direction === 1 ? (summary[test.evaluator].max ?? center) - center
                : Math.max(Math.abs((summary[test.evaluator].min ?? center) - center),
                    Math.abs((summary[test.evaluator].max ?? center) - center));
        const threshold = test.evaluator === 'yaw' ? .18 : .12;
        return summary.facePresentRate >= .5 && motion >= threshold && summary.readyWhenPresentRate >= .6
            ? { status: 'PASS', message: `Acción detectada y calidad ${percent(summary.readyWhenPresentRate)}.` }
            : summary.facePresentRate >= .5 && motion >= threshold
                ? { status: 'WARN', message: `Acción detectada, pero calidad facial baja: ${percent(summary.readyWhenPresentRate)}.` }
            : motion >= threshold * .6
                ? { status: 'WARN', message: `Movimiento pequeño o detección inestable: ${angleValue(motion)}.` }
                : { status: 'FAIL', message: `No se ha observado suficiente movimiento: ${angleValue(motion)}.` };
    }
    if (test.evaluator === 'distance') {
        const boundaryFound = summary.facePresentRate <= .5
            || summary.faceSize.min != null && summary.faceSize.min < CAPTURE_MIN_FACE_SIZE
            || summary.readyRate <= .5;
        return boundaryFound
            ? { status: 'PASS', message: `Límite observado; mínimo ${formatPixels(summary.faceSize.min)} y ${percent(summary.readyRate)} ready.` }
            : { status: 'WARN', message: 'El rostro se mantuvo válido: aléjate más al repetir la prueba.' };
    }
    if (test.evaluator === 'absent') {
        return summary.tailFacePresentRate <= .1
            ? { status: 'PASS', message: 'El detector dejó de producir rostros en la ventana final.' }
            : summary.tailFacePresentRate <= .3
                ? { status: 'WARN', message: `Persistieron rostros en ${percent(summary.tailFacePresentRate)} de la ventana final.` }
                : { status: 'FAIL', message: `El detector siguió viendo un rostro en ${percent(summary.tailFacePresentRate)} de la ventana final.` };
    }
    if (test.evaluator === 'recovery') {
        if (!Number.isFinite(summary.recoveryMs)) {
            return { status: 'FAIL', message: 'No se recuperó ningún rostro durante los 10 segundos de espera.' };
        }
        return summary.readyWhenPresentRate >= .7
            ? { status: 'PASS', message: `Recuperado en ${formatMs(summary.recoveryMs)}; calidad ${percent(summary.readyWhenPresentRate)}.` }
            : summary.readyWhenPresentRate >= .4
                ? { status: 'WARN', message: `Recuperado en ${formatMs(summary.recoveryMs)}, pero con calidad inestable.` }
                : { status: 'WARN', message: `Rostro recuperado en ${formatMs(summary.recoveryMs)}, pero fuera de calidad.` };
    }
    if (test.evaluator === 'occlusion') {
        if (!state.handEnabled) {
            return { status: 'WARN', message: 'No se puede validar la oclusión con Hand desactivado.' };
        }
        return summary.occlusionRate >= .2 && summary.readyRate <= .5
            ? { status: 'PASS', message: `Oclusión detectada en ${percent(summary.occlusionRate)} y capturas bloqueadas.` }
            : summary.occlusionRate > 0
                ? { status: 'WARN', message: `Oclusión parcial: ${percent(summary.occlusionRate)} detectada, ${percent(summary.readyRate)} válida.` }
                : { status: 'FAIL', message: 'La mano sobre el rostro no se detectó como oclusión.' };
    }
    if (test.evaluator === 'spoof') {
        if (summary.facePresentRate < .5 || summary.faceSize.max < CAPTURE_MIN_FACE_SIZE) {
            return { status: 'WARN', message: `Prueba inconcluyente: acerca la imagen hasta superar ${CAPTURE_MIN_FACE_SIZE} px.` };
        }
        const rejected = summary.readyWhenPresentRate <= .2 && summary.passiveWhenPresentRate <= .2;
        return rejected
            ? { status: 'PASS', message: `Imagen rechazada; aceptación pasiva con rostro ${percent(summary.passiveWhenPresentRate)}.` }
            : { status: 'FAIL', message: `Fallo de seguridad: ${percent(summary.readyWhenPresentRate)} ready y ${percent(summary.passiveWhenPresentRate)} pasivas con rostro.` };
    }
    return { status: 'RECORDED', message: `Calidad registrada: ${percent(summary.readyRate)} ready.` };
}

function renderFaceTestResults() {
    const rows = FACE_TESTS.flatMap(test => {
        const result = state.faceTests.results.find(candidate => candidate.id === test.id);
        if (!result) {
            return [];
        }
        const summary = result.summary;
        const row = document.createElement('tr');
        const status = document.createElement('strong');
        status.className = `test-result-${faceTestStatusClass(result.evaluation.status)}`;
        status.textContent = result.evaluation.status;
        row.append(
            tableCell(`${result.id} · ${result.title}`), tableCell(status),
            tableCell(summary ? percent(summary.readyRate) : '—'),
            tableCell(summary ? percent(summary.readyWhenPresentRate) : '—'),
            tableCell(summary ? percent(summary.passiveWhenPresentRate) : '—'),
            tableCell(summary ? percent(summary.confidenceMean) : '—'),
            tableCell(summary ? percent(summary.realMean) : '—'),
            tableCell(summary ? percent(summary.liveMean) : '—'),
            tableCell(summary ? percent(summary.occlusionRate) : '—'),
            tableCell(summary ? degrees(Math.max(summary.yaw.maxAbs || 0, summary.pitch.maxAbs || 0, summary.roll.maxAbs || 0)) : '—'),
            tableCell(summary ? formatPixels(summary.faceSize.min) : '—'),
            tableCell(summary ? formatMs(summary.latencyMedianMs) : '—'),
            tableCell(summary?.sampleCount ?? 0));
        return [row];
    });
    dom.faceTestResults.replaceChildren(...(rows.length ? rows : [emptyFaceTestRow()]));
}

function renderFaceTestProgress() {
    const completed = state.faceTests.results.length;
    const current = state.faceTests.active ? state.faceTests.index + 1 : completed;
    dom.faceTestProgress.textContent = `${Math.min(current, FACE_TESTS.length)} / ${FACE_TESTS.length}`;
}

function renderFaceTestSummary() {
    const counts = {};
    state.faceTests.results.forEach(result => counts[result.evaluation.status] = (counts[result.evaluation.status] || 0) + 1);
    dom.faceTestSummary.textContent = Object.entries(counts).map(([status, count]) => `${status}: ${count}`).join(' · ');
}

function updateFaceTestAvailability() {
    dom.faceTestSuite.hidden = !state.faceEnabled;
    dom.startFaceTests.disabled = !state.running || !state.faceEnabled || !state.faceModelReady
        || state.faceTests.active || state.handTests.active;
    if (!state.faceTests.active && state.faceTests.phase !== 'complete') {
        dom.faceTestFeedback.textContent = state.handTests.active ? 'Termina primero la batería Hand.'
            : state.running && state.faceModelReady
                ? 'Face está listo. Inicia la batería cuando quieras.' : 'Inicia la cámara para comenzar.';
    }
}

async function copyFaceResults() {
    try {
        await navigator.clipboard.writeText(JSON.stringify(faceTestPayload(), null, 2));
        dom.faceTestFeedback.textContent = 'JSON copiado. Ya puedes pegarlo en el chat.';
    } catch (_) {
        dom.faceTestFeedback.textContent = 'No se pudo copiar. Usa “Descargar JSON”.';
    }
}

function downloadFaceResults() {
    const blob = new Blob([JSON.stringify(faceTestPayload(), null, 2)], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `atlas-presence-face-tests-${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(link.href), 0);
    dom.faceTestFeedback.textContent = 'Resultados descargados.';
}

function faceTestPayload() {
    const settings = state.stream?.getVideoTracks()[0]?.getSettings() || {};
    return {
        schemaVersion: 2,
        generatedAt: new Date().toISOString(),
        model: MODEL_VERSION,
        thresholds: {
            minConfidence: MIN_CONFIDENCE,
            detectorMinFaceSize: DETECTOR_MIN_FACE_SIZE,
            captureMinFaceSize: CAPTURE_MIN_FACE_SIZE,
            maxFaceAngle: MAX_FACE_ANGLE,
        },
        calibration: state.faceTests.calibration,
        environment: {
            pipelines: { faceEnabled: state.faceEnabled, handEnabled: state.handEnabled },
            camera: {
                width: settings.width, height: settings.height, frameRate: settings.frameRate,
                aspectRatio: settings.aspectRatio, facingMode: settings.facingMode,
            },
            hardwareConcurrency: navigator.hardwareConcurrency,
            deviceMemoryGb: navigator.deviceMemory,
            webgl: webGlInfo(),
            userAgent: navigator.userAgent,
        },
        results: FACE_TESTS.flatMap(test => {
            const result = state.faceTests.results.find(candidate => candidate.id === test.id);
            return result ? [result] : [];
        }),
    };
}

function faceTestEvaluatorSelfCheck() {
    const base = {
        sampleCount: 10, facePresentRate: 1, readyRate: .8, readyWhenPresentRate: .8,
        passiveAcceptedRate: .8, passiveWhenPresentRate: .8, occlusionRate: 0, tailFacePresentRate: 1,
        faceSize: { min: 400, max: 400 }, yaw: { min: 0, max: 0, maxAbs: 0 },
        pitch: { min: 0, max: 0, maxAbs: 0 }, roll: { min: 0, max: 0, maxAbs: 0 },
    };
    const valid = evaluateFaceTest({ evaluator: 'valid' }, base).status;
    const absent = evaluateFaceTest({ evaluator: 'absent' }, { ...base, facePresentRate: 0, tailFacePresentRate: 0 }).status;
    const inconclusiveSpoof = evaluateFaceTest({ evaluator: 'spoof' }, {
        ...base, facePresentRate: .05, faceSize: { min: 300, max: 305 },
    }).status;
    const acceptedSpoof = evaluateFaceTest({ evaluator: 'spoof' }, {
        ...base, readyWhenPresentRate: 1, passiveWhenPresentRate: 1,
    }).status;
    if (valid !== 'PASS' || absent !== 'PASS' || inconclusiveSpoof !== 'WARN' || acceptedSpoof !== 'FAIL') {
        throw new Error('Face test evaluator self-check failed.');
    }
}

function faceTestStatusClass(status) {
    return ({ PASS: 'pass', WARN: 'warn', FAIL: 'fail', RECORDED: 'recorded', SKIPPED: 'idle' })[status] || 'idle';
}

function startHandTests() {
    if (!state.running || !state.handEnabled || !state.handModelReady || state.faceTests.active) {
        dom.handTestFeedback.textContent = state.faceTests.active
            ? 'Termina o cancela la batería Face antes de comenzar.'
            : 'Inicia la cámara con Hand activo antes de comenzar.';
        return;
    }
    clearHandTestTimers();
    Object.assign(state.handTests, {
        active: true, index: 0, phase: 'ready', samples: [], results: [], recoveryMs: null,
    });
    dom.handTestResults.replaceChildren(emptyFaceTestRow());
    dom.handTestExport.hidden = true;
    dom.startHandTests.hidden = true;
    dom.cancelHandTests.hidden = false;
    logEvent('HAND TEST', 'Batería guiada iniciada');
    showHandTestStep();
    updateFaceTestAvailability();
}

function showHandTestStep() {
    if (!state.handTests.active) {
        return;
    }
    clearHandTestTimers();
    state.handTests.phase = 'ready';
    state.handTests.samples = [];
    state.handTests.recoveryMs = null;
    const test = HAND_TESTS[state.handTests.index];
    dom.handTestStep.hidden = false;
    dom.handTestCapture.hidden = true;
    dom.handTestId.textContent = test.id;
    dom.handTestKind.textContent = test.kind;
    dom.handTestTitle.textContent = test.title;
    dom.handTestInstruction.textContent = test.instruction;
    dom.handTestNote.textContent = test.note;
    setState(dom.handTestPhase, 'PREPARACIÓN', 'idle');
    dom.captureHandTest.hidden = false;
    dom.captureHandTest.disabled = false;
    dom.nextHandTest.hidden = true;
    dom.repeatHandTest.hidden = true;
    dom.skipHandTest.hidden = false;
    dom.handTestFeedback.textContent = `Haz ${test.expected || 'la acción indicada'} y pulsa “Registrar 4 segundos”.`;
    renderHandTestProgress();
}

function captureHandTest() {
    if (!state.handTests.active || state.handTests.phase !== 'ready' || !state.running) {
        return;
    }
    state.handTests.samples = [];
    state.handTests.recoveryMs = null;
    Object.assign(state.gesture, { candidate: null, candidateSince: 0, missingSince: 0, latched: null });
    const test = HAND_TESTS[state.handTests.index];
    if (test.evaluator === 'recovery') {
        state.handTests.phase = 'waiting';
        state.handTests.waitStarted = performance.now();
        dom.handTestCapture.hidden = false;
        dom.handTestTimer.max = HAND_TEST_DURATION_MS;
        dom.handTestTimer.value = 0;
        dom.handTestCountdown.textContent = 'Esperando mano…';
        dom.captureHandTest.disabled = true;
        dom.skipHandTest.hidden = true;
        dom.faceEnabled.disabled = true;
        dom.handEnabled.disabled = true;
        setState(dom.handTestPhase, 'ESPERANDO', 'warn');
        dom.handTestFeedback.textContent = 'Muestra ahora la palma abierta. La medición comenzará automáticamente.';
        state.handTests.captureTimeout = setTimeout(finishHandTestCapture, 10000);
        return;
    }
    beginHandTestCapture(performance.now());
}

function beginHandTestCapture(startedAt) {
    clearHandTestTimers();
    state.handTests.phase = 'capturing';
    state.handTests.captureStarted = startedAt;
    dom.handTestCapture.hidden = false;
    dom.handTestTimer.max = HAND_TEST_DURATION_MS;
    dom.handTestTimer.value = 0;
    dom.captureHandTest.disabled = true;
    dom.skipHandTest.hidden = true;
    dom.faceEnabled.disabled = true;
    dom.handEnabled.disabled = true;
    setState(dom.handTestPhase, 'CAPTURANDO', 'warn');
    dom.handTestFeedback.textContent = 'Mantén la acción hasta que termine la barra de captura.';
    updateHandTestTimer();
    state.handTests.timerInterval = setInterval(updateHandTestTimer, 50);
    state.handTests.captureTimeout = setTimeout(finishHandTestCapture, HAND_TEST_DURATION_MS);
}

function updateHandTestTimer() {
    const elapsed = Math.min(HAND_TEST_DURATION_MS, performance.now() - state.handTests.captureStarted);
    dom.handTestTimer.value = elapsed;
    dom.handTestCountdown.textContent = `${Math.max(0, (HAND_TEST_DURATION_MS - elapsed) / 1000).toFixed(1)} s`;
}

function recordHandTestSample(result, observedAt, latencyMs) {
    if (!state.handTests.active || !['waiting', 'capturing'].includes(state.handTests.phase)) {
        return;
    }
    const analysis = analyzeHand(result);
    if (state.handTests.phase === 'waiting') {
        if (!analysis.landmarks) {
            return;
        }
        state.handTests.recoveryMs = observedAt - state.handTests.waitStarted;
        beginHandTestCapture(observedAt);
        dom.handTestFeedback.textContent = `Mano recuperada en ${formatMs(state.handTests.recoveryMs)}. Midiendo estabilidad…`;
    }
    const landmarks = analysis.landmarks;
    const pose = analysis.worldLandmarks || landmarks;
    const pinch = landmarks ? pinchMetrics(pose) : null;
    const direction = landmarks ? directionMetrics(pose) : null;
    state.handTests.samples.push({
        atMs: Number((observedAt - state.handTests.captureStarted).toFixed(1)),
        handCount: result?.landmarks?.length || 0,
        landmarkCount: landmarks?.length || 0,
        worldLandmarkCount: analysis.worldLandmarks?.length || 0,
        handedness: analysis.handedness || null,
        handednessScore: finiteOrNull(analysis.handednessScore),
        cannedGesture: analysis.canned?.categoryName || null,
        cannedScore: finiteOrNull(analysis.canned?.score),
        recognized: analysis.recognized?.type || null,
        source: analysis.recognized?.source || null,
        confidence: finiteOrNull(analysis.recognized?.confidence),
        latched: state.gesture.latched,
        pinchRatio: finiteOrNull(pinch?.ratio),
        direction: direction ? {
            dx: finiteOrNull(direction.dx), dy: finiteOrNull(direction.dy),
            magnitude: finiteOrNull(direction.magnitude), deadZone: finiteOrNull(direction.deadZone),
        } : null,
        extendedFingers: pose ? FINGERS.filter(([, base]) => fingerIsExtended(pose, base)).map(([name]) => name) : [],
        handSize: finiteOrNull(normalizedHandSize(landmarks)),
        latencyMs: finiteOrNull(latencyMs),
        handFps: fps(state.handFrames),
    });
}

function finishHandTestCapture() {
    if (!state.handTests.active || !['waiting', 'capturing'].includes(state.handTests.phase)) {
        return;
    }
    clearHandTestTimers();
    state.handTests.phase = 'result';
    dom.faceEnabled.disabled = false;
    dom.handEnabled.disabled = false;
    dom.handTestTimer.value = HAND_TEST_DURATION_MS;
    dom.handTestCountdown.textContent = '0.0 s';
    const test = HAND_TESTS[state.handTests.index];
    const samples = state.handTests.samples.slice();
    const summary = summarizeHandSamples(test, samples);
    summary.recoveryMs = finiteOrNull(state.handTests.recoveryMs);
    const evaluation = evaluateHandTest(test, summary);
    const result = {
        id: test.id, kind: test.kind, title: test.title, instruction: test.instruction,
        expected: test.expected || null, capturedAt: new Date().toISOString(),
        recoveryMs: state.handTests.recoveryMs, evaluation, summary, samples,
    };
    state.handTests.results = state.handTests.results.filter(existing => existing.id !== test.id);
    state.handTests.results.push(result);
    renderHandTestResults();
    setState(dom.handTestPhase, evaluation.status, faceTestStatusClass(evaluation.status));
    dom.handTestFeedback.textContent = evaluation.message;
    dom.captureHandTest.hidden = true;
    dom.nextHandTest.hidden = false;
    dom.nextHandTest.textContent = state.handTests.index === HAND_TESTS.length - 1
        ? 'Finalizar batería' : 'Siguiente prueba';
    dom.repeatHandTest.hidden = false;
    dom.skipHandTest.hidden = true;
    logEvent('HAND TEST', `${test.id} ${evaluation.status} · ${evaluation.message}`);
}

function nextHandTest() {
    if (!state.handTests.active || state.handTests.phase !== 'result') {
        return;
    }
    if (state.handTests.index >= HAND_TESTS.length - 1) {
        completeHandTests();
        return;
    }
    state.handTests.index += 1;
    showHandTestStep();
}

function skipHandTest() {
    if (!state.handTests.active || state.handTests.phase !== 'ready') {
        return;
    }
    const test = HAND_TESTS[state.handTests.index];
    state.handTests.results = state.handTests.results.filter(existing => existing.id !== test.id);
    state.handTests.results.push({
        id: test.id, kind: test.kind, title: test.title, instruction: test.instruction,
        expected: test.expected || null, capturedAt: new Date().toISOString(),
        evaluation: { status: 'SKIPPED', message: 'Prueba omitida por el usuario.' },
        summary: null, samples: [],
    });
    renderHandTestResults();
    logEvent('HAND TEST', `${test.id} omitida`);
    if (state.handTests.index >= HAND_TESTS.length - 1) {
        completeHandTests();
    } else {
        state.handTests.index += 1;
        showHandTestStep();
    }
}

function cancelHandTests(message) {
    clearHandTestTimers();
    state.handTests.active = false;
    state.handTests.phase = 'idle';
    dom.faceEnabled.disabled = false;
    dom.handEnabled.disabled = false;
    dom.handTestStep.hidden = true;
    dom.handTestCapture.hidden = true;
    dom.startHandTests.hidden = false;
    dom.cancelHandTests.hidden = true;
    dom.handTestExport.hidden = state.handTests.results.length === 0;
    dom.handTestFeedback.textContent = message;
    renderHandTestSummary();
    renderHandTestProgress();
    updateFaceTestAvailability();
}

function completeHandTests() {
    clearHandTestTimers();
    state.handTests.active = false;
    state.handTests.phase = 'complete';
    dom.handTestStep.hidden = true;
    dom.handTestCapture.hidden = true;
    dom.startHandTests.hidden = false;
    dom.cancelHandTests.hidden = true;
    dom.handTestExport.hidden = false;
    dom.handTestFeedback.textContent = 'Batería completada. Copia o descarga el JSON y pásamelo para analizarlo.';
    renderHandTestSummary();
    renderHandTestProgress();
    updateFaceTestAvailability();
    logEvent('HAND TEST', 'Batería guiada completada');
}

function clearHandTestTimers() {
    clearTimeout(state.handTests.captureTimeout);
    clearInterval(state.handTests.timerInterval);
    state.handTests.captureTimeout = null;
    state.handTests.timerInterval = null;
}

function summarizeHandSamples(test, samples) {
    const detected = samples.filter(sample => sample.handCount > 0);
    const recognized = detected.filter(sample => sample.recognized);
    const expected = test.expected ? detected.filter(sample => sample.recognized === test.expected) : [];
    const wrong = test.expected ? detected.filter(sample => sample.recognized && sample.recognized !== test.expected) : [];
    const latched = test.expected ? samples.filter(sample => sample.latched === test.expected) : [];
    const lastAt = samples.at(-1)?.atMs || 0;
    const tail = samples.filter(sample => sample.atMs >= Math.max(0, lastAt - 1500));
    const gestures = {};
    samples.forEach(sample => {
        const name = sample.recognized || 'NONE';
        gestures[name] = (gestures[name] || 0) + 1;
    });
    return {
        sampleCount: samples.length,
        detectionRate: ratio(detected.length, samples.length),
        completeLandmarkRate: ratio(samples.filter(sample => sample.landmarkCount === 21).length, samples.length),
        worldLandmarkRate: ratio(samples.filter(sample => sample.worldLandmarkCount === 21).length, samples.length),
        recognizedRate: ratio(recognized.length, samples.length),
        expectedRate: ratio(expected.length, samples.length),
        expectedWhenDetectedRate: ratio(expected.length, detected.length),
        wrongGestureRate: ratio(wrong.length, detected.length),
        latchedExpectedRate: ratio(latched.length, samples.length),
        tailDetectionRate: ratio(tail.filter(sample => sample.handCount > 0).length, tail.length),
        firstHandAtMs: detected[0]?.atMs ?? null,
        lastHandAtMs: detected.at(-1)?.atMs ?? null,
        confidenceMean: mean(recognized.map(sample => sample.confidence).filter(Number.isFinite)),
        handednessScoreMean: mean(detected.map(sample => sample.handednessScore).filter(Number.isFinite)),
        handednessCounts: detected.reduce((counts, sample) => {
            const name = sample.handedness || 'UNKNOWN';
            counts[name] = (counts[name] || 0) + 1;
            return counts;
        }, {}),
        gestureCounts: gestures,
        pinchRatio: range(detected.map(sample => sample.pinchRatio).filter(Number.isFinite)),
        handSize: range(detected.map(sample => sample.handSize).filter(Number.isFinite)),
        latencyMedianMs: median(samples.map(sample => sample.latencyMs).filter(Number.isFinite)),
        latencyMaxMs: range(samples.map(sample => sample.latencyMs).filter(Number.isFinite)).max,
        handFpsMean: mean(samples.map(sample => sample.handFps).filter(Number.isFinite)),
    };
}

function evaluateHandTest(test, summary) {
    if (!summary.sampleCount) {
        return { status: 'FAIL', message: 'No se ha registrado ninguna muestra de Hand.' };
    }
    if (test.evaluator === 'tracking') {
        return summary.detectionRate >= .85 && summary.completeLandmarkRate >= .85
            ? { status: 'PASS', message: `Tracking completo en ${percent(summary.completeLandmarkRate)} de las muestras.` }
            : summary.detectionRate >= .6
                ? { status: 'WARN', message: `Tracking inestable: detección ${percent(summary.detectionRate)}, 21 puntos ${percent(summary.completeLandmarkRate)}.` }
                : { status: 'FAIL', message: `La mano solo se detectó en ${percent(summary.detectionRate)} de las muestras.` };
    }
    if (test.evaluator === 'gesture') {
        const pass = summary.detectionRate >= .7 && summary.expectedWhenDetectedRate >= .7
            && summary.latchedExpectedRate >= .4;
        return pass
            ? { status: 'PASS', message: `${test.expected} correcto en ${percent(summary.expectedWhenDetectedRate)}; estable ${percent(summary.latchedExpectedRate)}.` }
            : summary.expectedWhenDetectedRate >= .4
                ? { status: 'WARN', message: `${test.expected} inestable: ${percent(summary.expectedWhenDetectedRate)} correcto, ${percent(summary.wrongGestureRate)} otros.` }
                : { status: 'FAIL', message: `${test.expected} solo apareció en ${percent(summary.expectedWhenDetectedRate)} de las muestras detectadas.` };
    }
    if (test.evaluator === 'distance') {
        const sizeRatio = summary.handSize.max ? summary.handSize.min / summary.handSize.max : 1;
        const boundary = summary.tailDetectionRate <= .5 || sizeRatio <= .55;
        return summary.detectionRate > .2 && boundary
            ? { status: 'PASS', message: `Límite observado; tracking final ${percent(summary.tailDetectionRate)}, tamaño relativo ${decimal(sizeRatio, 2)}.` }
            : summary.detectionRate > .2
                ? { status: 'WARN', message: 'La mano permaneció estable; aléjala más al repetir.' }
                : { status: 'FAIL', message: 'No se detectó la mano inicial para medir la distancia.' };
    }
    if (test.evaluator === 'absent') {
        return summary.tailDetectionRate <= .1
            ? { status: 'PASS', message: 'El detector dejó de producir manos en la ventana final.' }
            : summary.tailDetectionRate <= .3
                ? { status: 'WARN', message: `Persistieron manos en ${percent(summary.tailDetectionRate)} de la ventana final.` }
                : { status: 'FAIL', message: `El detector mantuvo una mano fantasma en ${percent(summary.tailDetectionRate)} de la ventana final.` };
    }
    if (test.evaluator === 'recovery') {
        if (!Number.isFinite(summary.recoveryMs)) {
            return { status: 'FAIL', message: 'No se recuperó ninguna mano durante los 10 segundos de espera.' };
        }
        return summary.expectedWhenDetectedRate >= .7
            ? { status: 'PASS', message: `Recuperada en ${formatMs(summary.recoveryMs)}; OPEN_PALM ${percent(summary.expectedWhenDetectedRate)}.` }
            : summary.detectionRate >= .5
                ? { status: 'WARN', message: `Mano recuperada en ${formatMs(summary.recoveryMs)}, pero OPEN_PALM fue inestable.` }
                : { status: 'FAIL', message: 'La recuperación no mantuvo un tracking estable.' };
    }
    return { status: 'RECORDED', message: `Detección registrada: ${percent(summary.detectionRate)}.` };
}

function renderHandTestResults() {
    const rows = HAND_TESTS.flatMap(test => {
        const result = state.handTests.results.find(candidate => candidate.id === test.id);
        if (!result) {
            return [];
        }
        const summary = result.summary;
        const status = document.createElement('strong');
        status.className = `test-result-${faceTestStatusClass(result.evaluation.status)}`;
        status.textContent = result.evaluation.status;
        const size = summary && summary.handSize.min != null
            ? `${decimal(summary.handSize.min, 3)}–${decimal(summary.handSize.max, 3)}` : '—';
        const row = document.createElement('tr');
        row.append(
            tableCell(`${result.id} · ${result.title}`), tableCell(status),
            tableCell(summary ? percent(summary.detectionRate) : '—'),
            tableCell(summary ? percent(summary.completeLandmarkRate) : '—'),
            tableCell(result.expected && summary ? percent(summary.expectedWhenDetectedRate) : '—'),
            tableCell(result.expected && summary ? percent(summary.wrongGestureRate) : '—'),
            tableCell(result.expected && summary ? percent(summary.latchedExpectedRate) : '—'),
            tableCell(summary ? percent(summary.confidenceMean) : '—'),
            tableCell(summary ? percent(summary.handednessScoreMean) : '—'),
            tableCell(summary ? decimal(summary.pinchRatio.min, 3) : '—'),
            tableCell(size), tableCell(summary ? formatMs(summary.latencyMedianMs) : '—'),
            tableCell(summary?.sampleCount ?? 0));
        return [row];
    });
    dom.handTestResults.replaceChildren(...(rows.length ? rows : [emptyFaceTestRow()]));
}

function renderHandTestProgress() {
    const completed = state.handTests.results.length;
    const current = state.handTests.active ? state.handTests.index + 1 : completed;
    dom.handTestProgress.textContent = `${Math.min(current, HAND_TESTS.length)} / ${HAND_TESTS.length}`;
}

function renderHandTestSummary() {
    const counts = {};
    state.handTests.results.forEach(result => counts[result.evaluation.status] = (counts[result.evaluation.status] || 0) + 1);
    dom.handTestSummary.textContent = Object.entries(counts).map(([status, count]) => `${status}: ${count}`).join(' · ');
}

function updateHandTestAvailability() {
    dom.handTestSuite.hidden = !state.handEnabled;
    dom.startHandTests.disabled = !state.running || !state.handEnabled || !state.handModelReady
        || state.handTests.active || state.faceTests.active;
    if (!state.handTests.active && state.handTests.phase !== 'complete') {
        dom.handTestFeedback.textContent = state.faceTests.active ? 'Termina primero la batería Face.'
            : state.running && state.handModelReady
                ? 'Hand está listo. Inicia la batería cuando quieras.' : 'Inicia la cámara para comenzar.';
    }
}

async function copyHandResults() {
    try {
        await navigator.clipboard.writeText(JSON.stringify(handTestPayload(), null, 2));
        dom.handTestFeedback.textContent = 'JSON copiado. Ya puedes pegarlo en el chat.';
    } catch (_) {
        dom.handTestFeedback.textContent = 'No se pudo copiar. Usa “Descargar JSON”.';
    }
}

function downloadHandResults() {
    const blob = new Blob([JSON.stringify(handTestPayload(), null, 2)], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `atlas-presence-hand-tests-${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(link.href), 0);
    dom.handTestFeedback.textContent = 'Resultados descargados.';
}

function handTestPayload() {
    const settings = state.stream?.getVideoTracks()[0]?.getSettings() || {};
    return {
        schemaVersion: 1,
        generatedAt: new Date().toISOString(),
        model: GESTURE_MODEL,
        thresholds: {
            handConfidence: HAND_CONFIDENCE, gestureHoldMs: GESTURE_HOLD_MS,
            pinchHoldMs: PINCH_HOLD_MS, pinchDistanceRatio: PINCH_DISTANCE_RATIO,
        },
        environment: {
            pipelines: { faceEnabled: state.faceEnabled, handEnabled: state.handEnabled },
            camera: {
                width: settings.width, height: settings.height, frameRate: settings.frameRate,
                aspectRatio: settings.aspectRatio, facingMode: settings.facingMode,
            },
            hardwareConcurrency: navigator.hardwareConcurrency,
            deviceMemoryGb: navigator.deviceMemory,
            webgl: webGlInfo(),
            userAgent: navigator.userAgent,
        },
        results: HAND_TESTS.flatMap(test => {
            const result = state.handTests.results.find(candidate => candidate.id === test.id);
            return result ? [result] : [];
        }),
    };
}

function handTestEvaluatorSelfCheck() {
    const base = {
        sampleCount: 10, detectionRate: .9, completeLandmarkRate: .9,
        expectedWhenDetectedRate: .8, wrongGestureRate: .1, latchedExpectedRate: .6,
        tailDetectionRate: .9, handSize: { min: .2, max: .4 }, recoveryMs: 300,
    };
    const tracking = evaluateHandTest({ evaluator: 'tracking' }, base).status;
    const gesture = evaluateHandTest({ evaluator: 'gesture', expected: 'FIST' }, base).status;
    const absent = evaluateHandTest({ evaluator: 'absent' }, { ...base, tailDetectionRate: 0 }).status;
    const missingRecovery = evaluateHandTest({ evaluator: 'recovery' }, { ...base, recoveryMs: null }).status;
    if (tracking !== 'PASS' || gesture !== 'PASS' || absent !== 'PASS' || missingRecovery !== 'FAIL') {
        throw new Error('Hand test evaluator self-check failed.');
    }
}

function normalizedHandSize(landmarks) {
    if (!landmarks?.length) {
        return null;
    }
    const xs = landmarks.map(point => pointCoordinates(point).x);
    const ys = landmarks.map(point => pointCoordinates(point).y);
    return Math.max(Math.max(...xs) - Math.min(...xs), Math.max(...ys) - Math.min(...ys));
}

function emptyFaceTestRow() {
    const row = document.createElement('tr');
    row.className = 'empty';
    const cell = document.createElement('td');
    cell.colSpan = 13;
    cell.textContent = 'Todavía no hay resultados.';
    row.appendChild(cell);
    return row;
}

function tableCell(value) {
    const cell = document.createElement('td');
    value instanceof Node ? cell.appendChild(value) : cell.textContent = value;
    return cell;
}

function finiteOrNull(value) {
    return Number.isFinite(value) ? Number(value.toFixed(6)) : null;
}

function ratio(value, total) {
    return total ? value / total : 0;
}

function mean(values) {
    return values.length ? values.reduce((total, value) => total + value, 0) / values.length : null;
}

function median(values) {
    if (!values.length) {
        return null;
    }
    const sorted = values.toSorted((first, second) => first - second);
    const middle = Math.floor(sorted.length / 2);
    return sorted.length % 2 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
}

function range(values) {
    return values.length
        ? { min: Math.min(...values), max: Math.max(...values), mean: mean(values), maxAbs: Math.max(...values.map(Math.abs)) }
        : { min: null, max: null, mean: null, maxAbs: null };
}

function formatPixels(value) {
    return Number.isFinite(value) ? `${Math.round(value)} px` : '—';
}

function faceStatus(result) {
    return AtlasFaceQuality.evaluate(result, {
        handResult: state.handEnabled ? state.handResult : null,
        center: state.faceTests.calibration,
        neutral: false,
    });
}

function renderFace(result) {
    const status = faceStatus(result);
    const face = status.face;
    const stateName = status.ready ? 'VÁLIDO' : face ? 'RECHAZADO' : 'NO DETECTADO';
    setState(dom.faceState, stateName, status.ready ? 'ready' : face ? 'warn' : 'idle');
    setBadge(dom.faceBadge, `FACE ${result?.face?.length || 0}`, status.ready ? 'ready' : face ? 'warn' : 'idle');
    dom.faceReason.textContent = status.reason;
    logTransition('previousFaceState', 'FACE', stateName + ' · ' + status.reason);
    const now = performance.now();
    if (now - state.lastFaceTelemetry < 200) {
        return;
    }
    state.lastFaceTelemetry = now;

    if (!face) {
        renderData(dom.faceMetrics, [['Rostros', result?.face?.length || 0]]);
        dom.faceGestures.textContent = formatJson(result?.gesture || []);
        dom.faceRaw.textContent = formatJson({ faces: result?.face || [], performance: result?.performance });
        clearCanvas(dom.faceCrop);
        return;
    }

    const confidence = face.faceScore || face.boxScore || 0;
    const angle = face.rotation?.angle || {};
    const embedding = Array.from(face.embedding || []);
    const embeddingNorm = Math.hypot(...embedding);
    renderData(dom.faceMetrics, [
        ['Confianza', percent(confidence)],
        ['Box', vector(face.box, 1)],
        ['Tamaño mínimo', `${Math.round(Math.min(face.box[2], face.box[3]))} px`],
        ['Real / antispoof', percent(face.real || 0)],
        ['Live / liveness', percent(face.live || 0)],
        ['Malla', `${face.mesh?.length || 0} puntos`],
        ['Yaw', angleValue(angle.yaw)],
        ['Pitch', angleValue(angle.pitch)],
        ['Roll', angleValue(angle.roll)],
        ['Embedding', `${embedding.length} valores`],
        ['Norma embedding', decimal(embeddingNorm, 4)],
        ['Modelo', MODEL_VERSION],
    ]);
    dom.faceGestures.textContent = formatJson((result?.gesture || []).filter(item => item.face === 0));
    dom.faceRaw.textContent = formatJson({
        ...face,
        embedding: { length: embedding.length, norm: embeddingNorm, preview: embedding.slice(0, 24) },
        pipelinePerformance: result?.performance,
    });
    drawCrop(face.box, dom.faceCrop);
}

function renderHand(result, now) {
    const analysis = analyzeHand(result);
    updateGestureStability(analysis.recognized, now, analysis.reason);
    const detected = Boolean(analysis.landmarks);
    const stateName = detected ? analysis.recognized ? 'RECONOCIDO' : 'TRACKING' : 'NO DETECTADA';
    setState(dom.handState, stateName, analysis.recognized ? 'ready' : detected ? 'warn' : 'idle');
    setBadge(dom.handBadge, detected ? 'HAND 1' : 'HAND 0', detected ? 'ready' : 'idle');
    dom.handReason.textContent = analysis.reason;
    logTransition('previousHandState', 'HAND', stateName + ' · ' + analysis.reason);
    if (now - state.lastHandTelemetry < 100) {
        return;
    }
    state.lastHandTelemetry = now;

    renderData(dom.handMetrics, [
        ['Manos', result?.landmarks?.length || 0],
        ['Lateralidad', analysis.handedness || '—'],
        ['Score lateralidad', percent(analysis.handednessScore)],
        ['Gesto MediaPipe', analysis.canned?.categoryName || 'None'],
        ['Score MediaPipe', percent(analysis.canned?.score)],
        ['Gesto Atlas', analysis.recognized?.type || '—'],
        ['Fuente', analysis.recognized?.source || '—'],
        ['Landmarks', analysis.landmarks?.length || 0],
        ['World landmarks', analysis.worldLandmarks?.length || 0],
    ]);
    dom.handAnalysis.textContent = analysis.details;
    dom.handLandmarks.textContent = formatLandmarks(analysis.landmarks);
    dom.handWorldLandmarks.textContent = formatLandmarks(analysis.worldLandmarks);
    dom.handRaw.textContent = formatJson(result || {});
    if (analysis.landmarks) {
        drawHandCrop(analysis.landmarks, dom.handCrop);
    } else {
        clearCanvas(dom.handCrop);
    }
}

function analyzeHand(result) {
    const landmarks = result?.landmarks?.[0];
    const worldLandmarks = result?.worldLandmarks?.[0];
    const canned = result?.gestures?.[0]?.[0];
    const handedness = result?.handednesses?.[0]?.[0];
    if (!landmarks || landmarks.length !== 21) {
        return {
            landmarks: null, worldLandmarks: null, canned,
            handedness: handedness?.categoryName, handednessScore: handedness?.score,
            recognized: null, reason: 'MediaPipe no ha devuelto los 21 landmarks.', details: '—',
        };
    }

    const pose = worldLandmarks || landmarks;
    const normalizedPinch = pinchMetrics(landmarks);
    const worldPinch = worldLandmarks && pinchMetrics(worldLandmarks);
    const directional = isDirectionalPose(pose);
    const direction = directionMetrics(pose);
    const pointing = isPointingPose(pose) || pose !== landmarks && isPointingPose(landmarks);
    const openPalm = isOpenPalmPose(pose) || pose !== landmarks && isOpenPalmPose(landmarks);
    let recognized = null;
    let reason;
    if (openPalm && canned?.categoryName === 'Open_Palm') {
        recognized = observed('OPEN_PALM', canned, 'clasificador + geometría de palma');
        reason = 'OPEN_PALM: MediaPipe confirma la palma y los cuatro dedos están extendidos.';
    } else if (isPinch(pose) || pose !== landmarks && isPinch(landmarks)) {
        recognized = observed('PINCH', canned, 'geometría thumb–index');
        reason = `PINCH: ratio ${decimal((worldPinch || normalizedPinch).ratio, 3)} ≤ ${PINCH_DISTANCE_RATIO}.`;
    } else if (directional) {
        const type = staticDirection(pose);
        recognized = type && observed(type, canned, 'geometría de dos dedos');
        reason = type
            ? `${type}: índice y medio extendidos; vector dx=${decimal(direction.dx, 3)}, dy=${decimal(direction.dy, 3)}.`
            : 'Pose direccional válida, pero el vector está en diagonal o no supera la zona muerta.';
    } else if (pointing) {
        recognized = observed('POINT', canned, 'geometría del índice');
        reason = 'POINT: índice extendido; medio, anular y meñique plegados.';
    } else if (openPalm) {
        recognized = observed('OPEN_PALM', canned, 'geometría de palma');
        reason = 'OPEN_PALM: índice, medio, anular y meñique extendidos.';
    } else {
        const type = ({
            Closed_Fist: 'FIST', Open_Palm: 'OPEN_PALM', Pointing_Up: 'POINT', Thumb_Up: 'THUMBS_UP',
        })[canned?.categoryName];
        recognized = type ? observed(type, canned, 'clasificador MediaPipe') : null;
        reason = recognized
            ? `${recognized.type}: categoría MediaPipe ${canned.categoryName}.`
            : `Sin gesto Atlas: MediaPipe=${canned?.categoryName || 'None'} y no se cumplen las heurísticas.`;
    }

    const fingers = FINGERS.map(([name, base]) => fingerMetrics(pose, name, base));
    const details = [
        `Candidatos MediaPipe: ${formatJson(result.gestures?.[0] || [])}`,
        `Pinch normalizado: distancia=${decimal(normalizedPinch.distance, 4)}, palma=${decimal(normalizedPinch.palmWidth, 4)}, ratio=${decimal(normalizedPinch.ratio, 4)}`,
        worldPinch ? `Pinch world: distancia=${decimal(worldPinch.distance, 4)}, palma=${decimal(worldPinch.palmWidth, 4)}, ratio=${decimal(worldPinch.ratio, 4)}` : 'Pinch world: —',
        `Dirección: dx=${decimal(direction.dx, 4)}, dy=${decimal(direction.dy, 4)}, magnitud=${decimal(direction.magnitude, 4)}, zona-muerta=${decimal(direction.deadZone, 4)}`,
        `Pose direccional: ${directional ? 'sí' : 'no'} · Pose point: ${pointing ? 'sí' : 'no'}`,
        '',
        ...fingers.map(finger => `${finger.name.padEnd(7)} ${finger.extended ? 'EXTENDIDO' : 'plegado  '} · ángulos ${decimal(finger.angle1, 1)}° / ${decimal(finger.angle2, 1)}° · alcance ${decimal(finger.reach, 3)}×`),
    ].join('\n');

    return {
        landmarks, worldLandmarks, canned,
        handedness: handedness?.categoryName, handednessScore: handedness?.score,
        recognized, reason, details,
    };
}

function observed(type, canned, source) {
    return { type, confidence: Number((canned?.score || HAND_CONFIDENCE).toFixed(3)), source };
}

function updateGestureStability(observedGesture, now, reason) {
    const tracking = state.gesture;
    if (!observedGesture) {
        tracking.missingSince ||= now;
        if (now - tracking.missingSince >= GESTURE_HOLD_MS) {
            tracking.candidate = null;
            tracking.candidateSince = 0;
            tracking.latched = null;
        }
        renderGesture(null, 0, GESTURE_HOLD_MS, reason);
        return;
    }

    tracking.missingSince = 0;
    if (tracking.candidate !== observedGesture.type) {
        tracking.candidate = observedGesture.type;
        tracking.candidateSince = now;
        logEvent('CANDIDATE', `${observedGesture.type} · ${observedGesture.source}`);
    }
    const hold = observedGesture.type === 'PINCH' ? PINCH_HOLD_MS : GESTURE_HOLD_MS;
    const elapsed = Math.min(hold, now - tracking.candidateSince);
    if (elapsed >= hold && tracking.latched !== observedGesture.type) {
        tracking.latched = observedGesture.type;
        logEvent('LATCHED', `${observedGesture.type} estable durante ${hold} ms`);
    }
    renderGesture(observedGesture, elapsed, hold, reason);
}

function renderGesture(observedGesture, elapsed, hold, reason) {
    const name = observedGesture?.type || '—';
    dom.atlasGesture.textContent = name;
    setBadge(dom.gestureBadge, `GESTURE ${name}`, observedGesture ? 'ready' : 'idle');
    dom.holdProgress.max = hold;
    dom.holdProgress.value = elapsed;
    dom.holdLabel.textContent = `${Math.round(elapsed)} / ${hold} ms`;
    dom.gestureReason.textContent = reason;
    renderData(dom.gestureMetrics, [
        ['Candidato', state.gesture.candidate || '—'],
        ['Estabilizado', state.gesture.latched || '—'],
        ['Confianza', percent(observedGesture?.confidence)],
        ['Hold requerido', `${hold} ms`],
        ['Ausente desde', state.gesture.missingSince ? `${Math.round(performance.now() - state.gesture.missingSince)} ms` : '—'],
        ['Acción', 'No se publica'],
    ]);
    if (state.previousGesture !== name) {
        state.previousGesture = name;
    }
}

function renderOverlay() {
    const video = dom.camera;
    if (!video.videoWidth || !video.videoHeight) {
        return;
    }
    const canvas = dom.overlay;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (state.faceEnabled) {
        drawFaceOverlay(ctx, state.faceResult, canvas.width);
    }
    if (state.handEnabled) {
        drawHandOverlay(ctx, state.handResult, canvas.width, canvas.height);
    }
}

function drawFaceOverlay(ctx, result, width) {
    const status = faceStatus(result);
    for (const [index, face] of (result?.face || []).entries()) {
        const [x, y, boxWidth, boxHeight] = face.box;
        const mirroredX = width - x - boxWidth;
        ctx.strokeStyle = status.ready && index === 0 ? '#64f0a7' : '#ff667a';
        ctx.lineWidth = 3;
        ctx.strokeRect(mirroredX, y, boxWidth, boxHeight);
        ctx.fillStyle = ctx.strokeStyle;
        for (const point of face.mesh || []) {
            ctx.fillRect(width - point[0] - 1, point[1] - 1, 2, 2);
        }
        drawLabel(ctx, mirroredX, Math.max(22, y), `FACE ${index} · ${percent(face.faceScore || face.boxScore)}`, ctx.strokeStyle);
    }
}

function drawHandOverlay(ctx, result, width, height) {
    const landmarks = result?.landmarks?.[0];
    if (!landmarks) {
        return;
    }
    ctx.strokeStyle = '#54e1ff';
    ctx.fillStyle = '#ffffff';
    ctx.lineWidth = 3;
    for (const [from, to] of HAND_CONNECTIONS) {
        ctx.beginPath();
        ctx.moveTo(width * (1 - landmarks[from].x), height * landmarks[from].y);
        ctx.lineTo(width * (1 - landmarks[to].x), height * landmarks[to].y);
        ctx.stroke();
    }
    landmarks.forEach((point, index) => {
        const x = width * (1 - point.x);
        const y = height * point.y;
        ctx.beginPath();
        ctx.arc(x, y, index === 4 || index === 8 ? 6 : 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#54e1ff';
        ctx.font = '12px ui-monospace, monospace';
        ctx.fillText(String(index), x + 7, y - 7);
        ctx.fillStyle = '#ffffff';
    });
}

function drawLabel(ctx, x, y, text, color) {
    ctx.font = '700 14px ui-monospace, monospace';
    const width = ctx.measureText(text).width + 14;
    ctx.fillStyle = 'rgba(3, 5, 6, .82)';
    ctx.fillRect(x, y - 22, width, 22);
    ctx.fillStyle = color;
    ctx.fillText(text, x + 7, y - 7);
}

function drawCrop(box, canvas) {
    if (!box || !dom.camera.videoWidth) {
        clearCanvas(canvas);
        return;
    }
    const padding = Math.max(box[2], box[3]) * .22;
    drawVideoRegion(canvas, box[0] - padding, box[1] - padding, box[2] + padding * 2, box[3] + padding * 2);
}

function drawHandCrop(landmarks, canvas) {
    const width = dom.camera.videoWidth;
    const height = dom.camera.videoHeight;
    const xs = landmarks.map(point => point.x * width);
    const ys = landmarks.map(point => point.y * height);
    const x = Math.min(...xs);
    const y = Math.min(...ys);
    const boxWidth = Math.max(...xs) - x;
    const boxHeight = Math.max(...ys) - y;
    const padding = Math.max(boxWidth, boxHeight) * .3;
    drawVideoRegion(canvas, x - padding, y - padding, boxWidth + padding * 2, boxHeight + padding * 2);
}

function drawVideoRegion(canvas, x, y, width, height) {
    const videoWidth = dom.camera.videoWidth;
    const videoHeight = dom.camera.videoHeight;
    x = Math.max(0, x);
    y = Math.max(0, y);
    width = Math.min(videoWidth - x, width);
    height = Math.min(videoHeight - y, height);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.save();
    ctx.translate(canvas.width, 0);
    ctx.scale(-1, 1);
    ctx.drawImage(dom.camera, x, y, width, height, 0, 0, canvas.width, canvas.height);
    ctx.restore();
}

function pinchMetrics(points) {
    const palmWidth = pointDistance(points[5], points[17]);
    const distance = pointDistance(points[4], points[8]);
    return { palmWidth, distance, ratio: palmWidth ? distance / palmWidth : Infinity };
}

function isPinch(points) {
    return pinchMetrics(points).ratio <= PINCH_DISTANCE_RATIO;
}

function isDirectionalPose(points) {
    const palmWidth = pointDistance(points[5], points[17]);
    return fingerIsExtended(points, 5) && fingerIsExtended(points, 9)
        && !fingerIsExtended(points, 13) && !fingerIsExtended(points, 17)
        && palmWidth > 0 && pointDistance(points[8], points[12]) <= palmWidth * .55;
}

function isPointingPose(points) {
    return fingerIsExtended(points, 5)
        && !fingerIsExtended(points, 9)
        && !fingerIsExtended(points, 13)
        && !fingerIsExtended(points, 17);
}

function isOpenPalmPose(points) {
    return [5, 9, 13, 17].every(base => fingerIsExtended(points, base));
}

function handPoseSelfCheck() {
    const points = Array.from({ length: 21 }, () => ({ x: 0, y: 0, z: 0 }));
    [5, 9, 13, 17].forEach((base, index) => {
        const x = (index - 1.5) * .2;
        [0, 1, 2, 3].forEach(offset => points[base + offset] = { x, y: .2 + offset * .2, z: 0 });
    });
    if (!isOpenPalmPose(points)) {
        throw new Error('Open palm pose self-check failed.');
    }
    if (analyzeHand({ landmarks: [points], worldLandmarks: [points], gestures: [[]] }).recognized?.type
        !== 'OPEN_PALM') {
        throw new Error('Open palm classifier self-check failed.');
    }
    points[12] = points[10];
    if (isOpenPalmPose(points)) {
        throw new Error('Folded finger pose self-check failed.');
    }
    directionSelfCheck();
}

function directionSelfCheck() {
    const twoFingers = rotate => {
        const points = Array.from({ length: 21 }, () => ({ x: 0, y: 0, z: 0 }));
        [5, 9, 13, 17].forEach((base, index) => {
            const x = (index - 1.5) * .2;
            [0, 1, 2, 3].forEach(offset => {
                const y = .2 + offset * .2;
                points[base + offset] = rotate ? { x: y, y: -x, z: 0 } : { x, y, z: 0 };
            });
        });
        points[16] = points[14];
        points[20] = points[18];
        return points;
    };
    if (!isDirectionalPose(twoFingers(false)) || staticDirection(twoFingers(false)) !== 'PALM_DOWN') {
        throw new Error('Direction self-check failed for PALM_DOWN.');
    }
    if (staticDirection(twoFingers(true)) !== 'PALM_LEFT') {
        throw new Error('Direction self-check failed for PALM_LEFT.');
    }
}

function fingerIsExtended(points, base) {
    return fingerMetrics(points, '', base).extended;
}

function fingerMetrics(points, name, base) {
    const angle1 = jointAngle(points[base], points[base + 1], points[base + 3]);
    const angle2 = jointAngle(points[base + 1], points[base + 2], points[base + 3]);
    const reach = pointDistance(points[0], points[base + 3]) / pointDistance(points[0], points[base + 1]);
    return { name, angle1, angle2, reach, extended: angle1 > 155 && angle2 > 145 && reach > 1.12 };
}

function jointAngle(first, middle, last) {
    const a = pointCoordinates(first);
    const b = pointCoordinates(middle);
    const c = pointCoordinates(last);
    const ab = Math.hypot(a.x - b.x, a.y - b.y, (a.z || 0) - (b.z || 0));
    const cb = Math.hypot(c.x - b.x, c.y - b.y, (c.z || 0) - (b.z || 0));
    if (!ab || !cb) {
        return 0;
    }
    const cosine = ((a.x - b.x) * (c.x - b.x) + (a.y - b.y) * (c.y - b.y)
        + ((a.z || 0) - (b.z || 0)) * ((c.z || 0) - (b.z || 0))) / (ab * cb);
    return Math.acos(Math.max(-1, Math.min(1, cosine))) * 180 / Math.PI;
}

function pointDistance(first, second) {
    const a = pointCoordinates(first);
    const b = pointCoordinates(second);
    return Math.hypot(a.x - b.x, a.y - b.y, (a.z || 0) - (b.z || 0));
}

function pointCoordinates(point) {
    return Array.isArray(point) ? { x: point[0], y: point[1], z: point[2] || 0 } : point;
}

function directionMetrics(points) {
    // dx va del dedo hacia la palma a proposito: el video se muestra en espejo (scaleX(-1)),
    // asi que la izquierda del usuario es la derecha de la imagen cruda.
    const indexBase = pointCoordinates(points[5]);
    const middleBase = pointCoordinates(points[9]);
    const indexTip = pointCoordinates(points[8]);
    const middleTip = pointCoordinates(points[12]);
    const dx = (indexBase.x + middleBase.x - indexTip.x - middleTip.x) / 2;
    const dy = (indexTip.y + middleTip.y - indexBase.y - middleBase.y) / 2;
    const palmWidth = pointDistance(points[5], points[17]);
    return { dx, dy, magnitude: Math.hypot(dx, dy), deadZone: palmWidth * .65 };
}

function staticDirection(points) {
    const { dx, dy, magnitude, deadZone } = directionMetrics(points);
    if (magnitude < deadZone) {
        return null;
    }
    if (Math.abs(dx) > Math.abs(dy) * 1.2) {
        return dx > 0 ? 'PALM_RIGHT' : 'PALM_LEFT';
    }
    if (Math.abs(dy) > Math.abs(dx) * 1.2) {
        return dy > 0 ? 'PALM_DOWN' : 'PALM_UP';
    }
    return null;
}

function renderPerformance() {
    dom.resolution.textContent = dom.camera.videoWidth ? `${dom.camera.videoWidth} × ${dom.camera.videoHeight}` : '—';
    dom.faceFps.textContent = state.faceEnabled ? `${fps(state.faceFrames)} fps` : 'OFF';
    dom.handFps.textContent = state.handEnabled ? `${fps(state.handFrames)} fps` : 'OFF';
    dom.faceLatency.textContent = state.faceEnabled ? formatMs(state.faceLatency) : 'OFF';
    dom.handLatency.textContent = state.handEnabled ? formatMs(state.handLatency) : 'OFF';
    dom.videoTime.textContent = `${dom.camera.currentTime.toFixed(3)} s`;
}

function renderThresholds() {
    renderDefinitionList(dom.thresholds, [
        ['Confianza facial mínima', MIN_CONFIDENCE],
        ['Lado mínimo del detector', `${DETECTOR_MIN_FACE_SIZE} px`],
        ['Lado mínimo para captura', `${CAPTURE_MIN_FACE_SIZE} px`],
        ['Ángulo facial máximo', `${MAX_FACE_ANGLE} rad / ${degrees(MAX_FACE_ANGLE)}`],
        ['Confianza de mano', HAND_CONFIDENCE],
        ['Ratio pinch máximo', PINCH_DISTANCE_RATIO],
        ['Hold pinch', `${PINCH_HOLD_MS} ms`],
        ['Hold otros gestos', `${GESTURE_HOLD_MS} ms`],
        ['Ángulo articulación 1', '> 155°'],
        ['Ángulo articulación 2', '> 145°'],
        ['Alcance de dedo', '> 1.12×'],
        ['Separación índice/medio', '≤ 0.55× palma'],
        ['Zona muerta dirección', '0.65× palma'],
        ['Dominancia de eje', '1.35×'],
    ]);
}

function renderSystem() {
    const settings = state.stream?.getVideoTracks()[0]?.getSettings() || {};
    const webgl = webGlInfo();
    renderData(dom.systemMetrics, [
        ['Human', state.faceEnabled ? state.faceModelReady ? 'ready' : 'no cargado' : 'off'],
        ['Backend solicitado', HUMAN_CONFIG.backend],
        ['Backend activo', window.Human?.tf?.getBackend?.() || '—'],
        ['MediaPipe', state.handEnabled ? state.handModelReady ? 'ready' : 'no cargado' : 'off'],
        ['Cámara', settings.deviceId ? 'activa' : '—'],
        ['Frame rate cámara', settings.frameRate ? `${decimal(settings.frameRate, 2)} fps` : '—'],
        ['Aspect ratio', settings.aspectRatio || '—'],
        ['CPU lógicos', navigator.hardwareConcurrency || '—'],
        ['Memoria dispositivo', navigator.deviceMemory ? `${navigator.deviceMemory} GB` : 'no expuesta'],
        ['WebGL vendor', webgl.vendor],
        ['WebGL renderer', webgl.renderer],
        ['Navegador', navigator.userAgent],
    ]);
}

function webGlInfo() {
    try {
        const gl = document.createElement('canvas').getContext('webgl');
        const extension = gl?.getExtension('WEBGL_debug_renderer_info');
        return {
            vendor: extension ? gl.getParameter(extension.UNMASKED_VENDOR_WEBGL) : 'no expuesto',
            renderer: extension ? gl.getParameter(extension.UNMASKED_RENDERER_WEBGL) : 'no expuesto',
        };
    } catch (_) {
        return { vendor: '—', renderer: '—' };
    }
}

function renderData(container, values) {
    container.replaceChildren(...values.map(([name, value]) => {
        const wrapper = document.createElement('div');
        const term = document.createElement('dt');
        const definition = document.createElement('dd');
        term.textContent = name;
        definition.textContent = value ?? '—';
        wrapper.append(term, definition);
        return wrapper;
    }));
}

function renderDefinitionList(container, values) {
    container.replaceChildren(...values.flatMap(([name, value]) => {
        const term = document.createElement('dt');
        const definition = document.createElement('dd');
        term.textContent = name;
        definition.textContent = value;
        return [term, definition];
    }));
}

function formatLandmarks(points) {
    if (!points?.length) {
        return '—';
    }
    return points.map((point, index) => `${String(index).padStart(2, '0')}  x=${signed(point.x)}  y=${signed(point.y)}  z=${signed(point.z)}`).join('\n');
}

function formatJson(value) {
    try {
        return JSON.stringify(value, (_, item) => ArrayBuffer.isView(item) ? Array.from(item) : item, 2);
    } catch (error) {
        return `No serializable: ${error.message}`;
    }
}

function logTransition(property, type, message) {
    if (state[property] === message) {
        return;
    }
    state[property] = message;
    logEvent(type, message);
}

function logEvent(type, message) {
    const item = document.createElement('li');
    const time = document.createElement('time');
    const name = document.createElement('b');
    const detail = document.createElement('span');
    time.textContent = new Date().toLocaleTimeString('es-ES', { hour12: false, fractionalSecondDigits: 3 });
    name.textContent = type;
    detail.textContent = message;
    item.append(time, name, detail);
    dom.eventLog.prepend(item);
    while (dom.eventLog.children.length > 80) {
        dom.eventLog.lastElementChild.remove();
    }
}

function setPipeline(message) {
    dom.pipelineMessage.textContent = message;
}

function fail(message) {
    setPipeline(message);
    setModelState('ERROR', 'error');
    logEvent('ERROR', message);
}

function setModelState(text, className) {
    setState(dom.modelState, text, className);
}

function setState(node, text, className) {
    node.textContent = text;
    node.className = `state ${className}`;
}

function setBadge(node, text, className) {
    node.textContent = text;
    node.className = `badge ${className}`;
}

function markFrame(frames, now) {
    frames.push(now);
    while (frames.length && frames[0] < now - 1000) {
        frames.shift();
    }
}

function fps(frames) {
    return frames.length;
}

function average(previous, value) {
    return previous == null ? value : previous * .82 + value * .18;
}

function clearCanvas(canvas) {
    canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
}

function percent(value) {
    return Number.isFinite(value) ? `${(value * 100).toFixed(1)}%` : '—';
}

function decimal(value, digits = 3) {
    return Number.isFinite(value) ? Number(value).toFixed(digits) : '—';
}

function signed(value) {
    return Number.isFinite(value) ? Number(value).toFixed(6).padStart(10, ' ') : '         —';
}

function vector(values, digits) {
    return Array.isArray(values) ? values.map(value => decimal(value, digits)).join(', ') : '—';
}

function degrees(radians) {
    return `${(radians * 180 / Math.PI).toFixed(1)}°`;
}

function angleValue(value) {
    return Number.isFinite(value) ? `${decimal(value, 3)} rad / ${degrees(value)}` : '—';
}

function formatMs(value) {
    return Number.isFinite(value) ? `${value.toFixed(1)} ms` : '—';
}

function cameraErrorMessage(error) {
    return ({
        NotAllowedError: 'Permiso de cámara denegado.',
        NotFoundError: 'No se encuentra ninguna cámara.',
        NotReadableError: 'La cámara está ocupada por otra aplicación.',
        OverconstrainedError: 'La cámara no admite la configuración solicitada.',
        SecurityError: 'El navegador ha bloqueado la cámara.',
    })[error?.name] || `No se pudo iniciar el sandbox: ${error?.message || error}`;
}
