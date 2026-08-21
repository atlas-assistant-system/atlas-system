const MODEL_VERSION = 'human-faceres-3.3.6';
const MIN_CONFIDENCE = 0.6;
const FACE_RESULT_MAX_AGE_MS = 750;
const ENROLLMENT_HOLD_FRAMES = 2;
const ENROLLMENT_FRONT_FRAMES = 4;
const ENROLLMENT_POSES = [
    { id: 'front', instruction: 'Mira de frente' },
    { id: 'side', instruction: 'Gira ligeramente hacia el lado que prefieras' },
    { id: 'opposite', instruction: 'Ahora gira ligeramente hacia el otro lado' },
    { id: 'up', instruction: 'Mira ligeramente hacia arriba' },
    { id: 'down', instruction: 'Mira ligeramente hacia abajo' },
];
const HAND_CONFIDENCE = 0.65;
// ponytail: pinned CDN keeps this UI adapter small; serve the same assets locally for offline deployment.
const HUMAN_MODELS = 'https://cdn.jsdelivr.net/npm/@vladmandic/human@3.3.6/models/';
const MEDIAPIPE = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/vision_bundle.mjs';
const MEDIAPIPE_WASM = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/wasm';
const GESTURE_MODEL = 'https://storage.googleapis.com/mediapipe-models/gesture_recognizer/'
    + 'gesture_recognizer/float16/1/gesture_recognizer.task';

const ERROR_MESSAGES = {
    MALFORMED_INPUT: 'Los datos enviados no son válidos.',
    UNEXPECTED: 'Se ha producido un error inesperado.',
    'General.ValueIsRequired': 'Falta un dato obligatorio.',
    'General.InvalidValue': 'Uno de los datos no es válido.',
    'Profile.NameRequired': 'Introduce un nombre para el perfil.',
    'Profile.NameTooLong': 'El nombre del perfil es demasiado largo.',
    'Profile.DescriptorRequired': 'No se ha podido obtener la firma facial.',
    'Profile.ModelVersionRequired': 'Falta la versión del modelo facial.',
    'Profile.DescriptorDimensionMismatch': 'La captura facial no es compatible con el modelo actual.',
    'Profile.TooManyTemplates': 'El perfil ya tiene el máximo de capturas faciales.',
    'Profile.LastTemplateCannotBeRemoved': 'No se puede eliminar la última captura del perfil.',
    'Profile.ModelVersionMismatch': 'Las capturas pertenecen a versiones distintas del modelo facial.',
    'Profile.NotFound': 'Ese perfil biométrico ya no existe.',
    'Profile.TemplateNotFound': 'Esa captura facial ya no existe.',
    'Authentication.NoProfilesEnrolled': 'No hay perfiles biométricos registrados.',
    'Verification.InvalidSimilarityScore': 'La puntuación de similitud no es válida.',
    'Verification.InvalidMatchThreshold': 'El umbral de reconocimiento no es válido.',
    'Verification.NoMatch': 'El rostro no coincide con ningún perfil registrado.',
    'Liveness.ChallengeExpired': 'La prueba de vida ha caducado. Inténtalo de nuevo.',
    'Liveness.ChallengeAlreadyUsed': 'Esa prueba de vida ya se ha utilizado.',
    'Liveness.ChallengeNotFound': 'La prueba de vida ya no está disponible.',
    'Liveness.Failed': 'No se ha superado la prueba de vida.',
    'Session.InvalidDuration': 'La duración de la sesión no es válida.',
    'Session.Expired': 'La sesión ha caducado.',
    'Session.Closed': 'La sesión ya está cerrada.',
    'Session.NotFound': 'La sesión ya no existe.',
    'Interaction.RequiresSession': 'Necesitas una sesión activa para usar los gestos.',
    'Presence.MaintenanceModeRequired': 'Esta operación requiere iniciar Presence en modo mantenimiento.',
};

const STATUS_MESSAGES = {
    400: 'Los datos enviados no son válidos.',
    401: 'Necesitas autenticarte de nuevo.',
    403: 'No tienes permiso para realizar esta operación.',
    404: 'El recurso solicitado ya no existe.',
    409: 'La operación entra en conflicto con el estado actual.',
    500: 'Se ha producido un error inesperado.',
    503: 'El servicio no está disponible en este momento.',
};

const state = {
    authentication: null,
    busy: false,
    cameraReady: false,
    human: null,
    result: null,
    resultVersion: 0,
    faceCenter: null,
    handRecognizer: null,
    handResult: null,
    handResultVersion: 0,
    handVideoTime: -1,
};

const dom = {
    authenticate: document.getElementById('authenticate'),
    camera: document.getElementById('camera'),
    cameraState: document.getElementById('camera-state'),
    challenge: document.getElementById('challenge'),
    challengeInstruction: document.getElementById('challenge-instruction'),
    challengeTime: document.getElementById('challenge-time'),
    closeSession: document.getElementById('close-session'),
    displayName: document.getElementById('display-name'),
    enroll: document.getElementById('enroll'),
    enrollmentCard: document.getElementById('enrollment-card'),
    events: document.getElementById('events'),
    feedback: document.getElementById('feedback'),
    openAgenda: document.getElementById('open-agenda'),
    profiles: document.getElementById('profiles'),
    sessionState: document.getElementById('session-state'),
    startCamera: document.getElementById('start-camera'),
};

async function load(path, options) {
    const response = await fetch(path, options);
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        throw { ...(body || {}), status: response.status };
    }
    return body;
}

function send(path, value) {
    return load(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(value),
    });
}

function message(error) {
    if (error instanceof TypeError) {
        return 'No se pudo conectar con el servicio.';
    }
    return ERROR_MESSAGES[error?.code] || STATUS_MESSAGES[error?.status]
        || (error?.name === 'Error' ? error.message : null) || 'No se pudo completar la operación.';
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

function setFeedback(text, error = false) {
    dom.feedback.textContent = text;
    dom.feedback.classList.toggle('error', error);
}

function updateActions() {
    const active = state.authentication && state.authentication.activeSession;
    const enrolled = state.authentication && state.authentication.enrolledProfiles > 0;
    dom.authenticate.disabled = state.busy || !state.cameraReady || !enrolled || Boolean(active);
    dom.enroll.disabled = state.busy || !state.cameraReady || Boolean(active) || !dom.displayName.value.trim();
    dom.closeSession.hidden = !active;
    dom.openAgenda.hidden = !active;
}

function faceStatus(result, options = {}) {
    return AtlasFaceQuality.evaluate(result, {
        handResult: state.handResult,
        center: state.faceCenter,
        ...options,
    });
}

async function startCamera() {
    if (!window.Human || !window.Human.Human) {
        setFeedback('No se pudo cargar el motor facial. Comprueba la conexión y recarga la página.', true);
        return;
    }

    state.busy = true;
    updateActions();
    dom.cameraState.textContent = 'Solicitando permiso para la cámara…';
    try {
        const stream = await navigator.mediaDevices.getUserMedia({
            audio: false,
            video: { facingMode: 'user', width: { ideal: 960 }, height: { ideal: 720 } },
        });
        dom.camera.srcObject = stream;
        await dom.camera.play();
        dom.cameraState.textContent = 'Cargando modelos faciales…';
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
        dom.cameraState.textContent = 'Cargando seguimiento de manos…';
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
        dom.startCamera.hidden = true;
        detectLoop();
        detectHands();
    } catch (error) {
        dom.cameraState.textContent = 'No se pudo iniciar la cámara.';
        setFeedback(cameraErrorMessage(error), true);
    } finally {
        state.busy = false;
        updateActions();
    }
}

async function detectLoop() {
    if (!state.cameraReady) {
        return;
    }
    try {
        const sessionActive = Boolean(state.authentication?.activeSession);
        if (!sessionActive) {
            state.result = await state.human.detect(dom.camera);
            state.resultVersion++;
        }
        dom.cameraState.textContent = sessionActive
            ? 'Procesamiento en pausa mientras la sesión está activa.'
            : faceStatus(state.result).reason;
    } catch (error) {
        dom.cameraState.textContent = 'Error procesando la imagen.';
    }
    requestAnimationFrame(detectLoop);
}

function detectHands() {
    if (!state.cameraReady) {
        return;
    }
    try {
        if (dom.camera.currentTime !== state.handVideoTime) {
            state.handVideoTime = dom.camera.currentTime;
            state.handResult = state.handRecognizer.recognizeForVideo(dom.camera, performance.now());
            state.handResultVersion++;
        }
    } catch (_) {
        state.handResult = null;
    }
    requestAnimationFrame(detectHands);
}

async function refresh() {
    try {
        state.authentication = await load('/authentication');
        const active = state.authentication.activeSession;
        if (!active && !state.busy) {
            state.faceCenter = null;
        }
        dom.enrollmentCard.hidden = !state.authentication.maintenanceMode;
        dom.sessionState.textContent = active
            ? `Sesión activa para ${active.profileId}.`
            : state.authentication.enrolledProfiles === 0
                ? 'No hay ningún rostro registrado.'
                : 'Sin sesión activa.';
        if (state.authentication.maintenanceMode || active) {
            await refreshProfiles();
        }
        updateActions();
    } catch (error) {
        dom.sessionState.textContent = message(error);
    }
}

async function refreshProfiles() {
    const profiles = await load('/profiles');
    dom.profiles.replaceChildren();
    for (const profile of profiles) {
        const item = document.createElement('li');
        const name = document.createElement('span');
        name.textContent = `${profile.displayName} · ${profile.templateCount} captura(s)`;
        item.appendChild(name);
        if (state.authentication.maintenanceMode) {
            const variant = document.createElement('button');
            variant.type = 'button';
            variant.className = 'quiet';
            variant.textContent = 'Añadir variante';
            variant.addEventListener('click', () => addProfileVariant(profile));
            item.appendChild(variant);
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'quiet';
            remove.textContent = 'Eliminar';
            remove.addEventListener('click', () => deleteProfile(profile.id));
            item.appendChild(remove);
        }
        dom.profiles.appendChild(item);
    }
}

async function enroll() {
    state.busy = true;
    updateActions();
    try {
        const descriptors = await captureEnrollmentDescriptors(
            () => state.result,
            () => state.resultVersion,
            (index, pose, detail) => {
                showEnrollmentStep(index);
                setFeedback(`${index + 1}/${ENROLLMENT_POSES.length} · ${detail || pose.instruction}`);
            });
        const profile = await send('/profiles', {
            displayName: dom.displayName.value.trim(),
            modelVersion: MODEL_VERSION,
            descriptor: descriptors[0],
        });
        let captures = 1;
        try {
            for (const descriptor of descriptors.slice(1)) {
                await send(`/profiles/${profile.id}/templates`, { modelVersion: MODEL_VERSION, descriptor });
                captures++;
            }
        } catch (error) {
            await load(`/profiles/${profile.id}`, { method: 'DELETE' }).catch(() => {});
            throw error;
        }
        dom.displayName.value = '';
        showEnrollmentStep(ENROLLMENT_POSES.length);
        setFeedback(`Rostro registrado con ${captures} capturas. Reinicia Presence sin --maintenance.`);
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    } finally {
        state.busy = false;
        updateActions();
    }
}

async function deleteProfile(id) {
    if (!window.confirm('¿Eliminar este perfil biométrico?')) {
        return;
    }
    try {
        await load(`/profiles/${id}`, { method: 'DELETE' });
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    }
}

function challengeDetector(type) {
    return result => type === 'FIST'
        && result?.gestures?.[0]?.some(value => value.categoryName === 'Closed_Fist'
            && value.score >= HAND_CONFIDENCE);
}

function faceResultIsRecent(result) {
    const age = Date.now() - Number(result?.timestamp);
    return Number.isFinite(age) && age >= 0 && age <= FACE_RESULT_MAX_AGE_MS;
}

function sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function showEnrollmentStep(active) {
    document.querySelectorAll('.enrollment-steps li').forEach((node, index) => {
        node.classList.toggle('active', index === active);
        node.classList.toggle('complete', index < active);
        if (index === active) {
            node.setAttribute('aria-current', 'step');
        } else {
            node.removeAttribute('aria-current');
        }
    });
}

async function captureEnrollmentDescriptors(currentResult, currentVersion, onStep) {
    const descriptors = [];
    let firstSide = 0;
    state.faceCenter = null;
    for (const [index, pose] of ENROLLMENT_POSES.entries()) {
        onStep(index, pose);
        let lastVersion = -1;
        let stableFaces = [];
        let missedFrames = 0;
        const requiredFrames = pose.id === 'front' ? ENROLLMENT_FRONT_FRAMES : ENROLLMENT_HOLD_FRAMES;
        const expiresAt = Date.now() + 20000;
        while (Date.now() < expiresAt) {
            const version = currentVersion();
            if (version !== lastVersion) {
                lastVersion = version;
                const result = currentResult();
                const status = faceStatus(result, {
                    neutral: false, minSize: AtlasFaceQuality.ENROLLMENT_MIN_FACE_SIZE,
                });
                const poseState = status.ready ? AtlasFaceQuality.poseState(
                    status.face, pose.id, state.faceCenter, stableFaces.length > 0, firstSide) : null;
                if (poseState?.matches) {
                    stableFaces.push(status.face);
                    missedFrames = 0;
                } else if (++missedFrames >= 3) {
                    stableFaces = [];
                    missedFrames = 0;
                }
                onStep(index, pose, enrollmentPoseFeedback(
                    status, pose, poseState, stableFaces.length, requiredFrames));
                if (stableFaces.length >= requiredFrames) {
                    if (pose.id === 'front') {
                        state.faceCenter = AtlasFaceQuality.calibration(stableFaces);
                    } else if (pose.id === 'side') {
                        firstSide = Math.sign(AtlasFaceQuality.offsets(status.face, state.faceCenter).yaw) || 1;
                    }
                    descriptors.push(AtlasFaceQuality.averageDescriptors(stableFaces, state.faceCenter));
                    break;
                }
            }
            await sleep(60);
        }
        if (descriptors.length !== index + 1) {
            throw new Error(`No se pudo capturar: ${pose.instruction.toLowerCase()}.`);
        }
    }
    return descriptors;
}

function enrollmentPoseFeedback(status, pose, poseState, stableFrames, requiredFrames) {
    if (!status.ready) {
        return status.reason;
    }
    if (stableFrames > 0) {
        return `Mantén la posición · confirmando ${stableFrames}/${requiredFrames}`;
    }
    if (pose.id === 'front') {
        return 'Mantén el rostro centrado un instante';
    }
    if (!poseState.aligned) {
        return pose.id === 'side' || pose.id === 'opposite'
            ? `${pose.instruction} · mantén la cabeza nivelada`
            : `${pose.instruction} · evita girar hacia un lado`;
    }
    const progress = Math.floor(poseState.progress * 1800 / Math.PI) / 10;
    const target = Math.round(poseState.target * 180 / Math.PI);
    return `${pose.instruction} · ${progress.toFixed(1)}° / ${target}°`;
}

async function captureNeutralFaces(count, expiresAt, onProgress) {
    const faces = [];
    let lastVersion = -1;
    while (Date.now() < expiresAt) {
        if (state.resultVersion !== lastVersion) {
            lastVersion = state.resultVersion;
            const status = faceStatus(state.result, { center: state.faceCenter });
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
    setFeedback('Mira de frente: calibrando tu posición neutral…');
    const faces = await captureNeutralFaces(5, Date.now() + 5000,
        (current, total, reason) => setFeedback(reason || `Calibrando posición ${current}/${total}…`));
    state.faceCenter = AtlasFaceQuality.calibration(faces);
}

async function addProfileVariant(profile) {
    state.busy = true;
    updateActions();
    try {
        setFeedback(`Cambia tu aspecto si lo necesitas y mira de frente para añadirlo a ${profile.displayName}.`);
        await calibrateFaceCenter();
        const faces = await captureNeutralFaces(3, Date.now() + 5000,
            (current, total, reason) => setFeedback(reason || `Capturando variante ${current}/${total}…`));
        await send(`/profiles/${profile.id}/templates`, {
            modelVersion: MODEL_VERSION,
            descriptor: AtlasFaceQuality.averageDescriptors(faces, state.faceCenter),
        });
        setFeedback(`Variante añadida al perfil de ${profile.displayName}.`);
        await refreshProfiles();
    } catch (error) {
        setFeedback(message(error), true);
    } finally {
        state.busy = false;
        updateActions();
    }
}

async function waitForChallenge(challenge) {
    const detected = challengeDetector(challenge.type);
    const expiresAt = Date.parse(challenge.expiresAt);
    let consecutiveFrames = 0;
    let lastHandVersion = -1;
    while (Date.now() < expiresAt) {
        const remaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
        dom.challengeTime.textContent = `${remaining} s`;
        if (state.handResultVersion !== lastHandVersion) {
            lastHandVersion = state.handResultVersion;
            const status = faceStatus(state.result, { neutral: false });
            const recentFace = faceResultIsRecent(state.result);
            const closedFist = detected(state.handResult);
            consecutiveFrames = recentFace && status.ready && closedFist ? consecutiveFrames + 1 : 0;
            setFeedback(!recentFace ? 'Actualizando la captura facial…'
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
    state.busy = true;
    updateActions();
    setFeedback('Preparando desafío…');
    try {
        await calibrateFaceCenter();
        const challenge = await send('/authentication/challenges', {});
        dom.challenge.hidden = false;
        dom.challengeInstruction.textContent = 'Mantén el puño cerrado';
        setFeedback('Verificando puño y rostro…');
        await waitForChallenge(challenge);
        dom.challengeInstruction.textContent = 'Suelta el puño y mira de frente';
        const faces = await captureNeutralFaces(3, Date.parse(challenge.expiresAt),
            (current, total, reason) => setFeedback(reason || `Seleccionando captura frontal ${current}/${total}…`));
        const session = await send(`/authentication/challenges/${challenge.challengeId}/complete`, {
            modelVersion: MODEL_VERSION,
            descriptor: AtlasFaceQuality.averageDescriptors(faces, state.faceCenter),
            observedType: challenge.type,
            nonce: challenge.nonce,
            capturedAt: new Date().toISOString(),
        });
        setFeedback(`Sesión ${session.id} abierta correctamente.`);
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    } finally {
        dom.challenge.hidden = true;
        state.busy = false;
        updateActions();
    }
}

async function closeSession() {
    try {
        await load(`/sessions/${state.authentication.activeSession.id}`, { method: 'DELETE' });
        setFeedback('Sesión cerrada.');
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    }
}

dom.startCamera.addEventListener('click', startCamera);
dom.authenticate.addEventListener('click', authenticate);
dom.enroll.addEventListener('click', enroll);
dom.closeSession.addEventListener('click', closeSession);
dom.displayName.addEventListener('input', updateActions);

const stream = new EventSource('/events');
const eventNames = ['sessionOpened', 'sessionRefreshed', 'sessionExpired', 'sessionClosed', 'gestureDetected'];
eventNames.forEach(name => stream.addEventListener(name, event => {
    dom.events.textContent = `${name}: ${event.data}\n` + dom.events.textContent;
    refresh();
}));
stream.onerror = () => { dom.events.textContent = 'Reconectando…'; };

refresh();
