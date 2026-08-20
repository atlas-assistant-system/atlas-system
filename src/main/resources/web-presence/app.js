const MODEL_VERSION = 'human-faceres-3.3.6';
const MIN_CONFIDENCE = 0.6;
const MIN_FACE_SIZE = 224;
const MAX_FACE_ANGLE = 0.45;
const ENROLLMENT_SAMPLES = 3;
const HAND_CONFIDENCE = 0.65;
// ponytail: pinned CDN keeps this UI adapter small; serve the same assets locally for offline deployment.
const HUMAN_MODELS = 'https://cdn.jsdelivr.net/npm/@vladmandic/human@3.3.6/models/';
const MEDIAPIPE = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/vision_bundle.mjs';
const MEDIAPIPE_WASM = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/wasm';
const GESTURE_MODEL = 'https://storage.googleapis.com/mediapipe-models/gesture_recognizer/'
    + 'gesture_recognizer/float16/1/gesture_recognizer.task';

const state = {
    authentication: null,
    busy: false,
    cameraReady: false,
    human: null,
    result: null,
    resultVersion: 0,
    handRecognizer: null,
    handResult: null,
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
        throw body || { message: 'No se pudo completar la operación.' };
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
    return error && error.message ? error.message : 'No se pudo completar la operación.';
}

function setFeedback(text, error = false) {
    dom.feedback.textContent = text;
    dom.feedback.classList.toggle('error', error);
}

function updateActions() {
    const active = state.authentication && state.authentication.activeSession;
    const enrolled = state.authentication && state.authentication.enrolledProfiles > 0;
    dom.authenticate.disabled = state.busy || !state.cameraReady || !enrolled || Boolean(active);
    dom.enroll.disabled = state.busy || !state.cameraReady || !dom.displayName.value.trim();
    dom.closeSession.hidden = !active;
    dom.openAgenda.hidden = !active;
}

function faceStatus(result) {
    if (!result || result.face.length !== 1) {
        return { ready: false, reason: result && result.face.length > 1
            ? 'Debe aparecer una sola persona.' : 'Coloca tu rostro dentro de la guía.' };
    }

    const face = result.face[0];
    const confidence = face.faceScore || face.boxScore || 0;
    if (confidence < MIN_CONFIDENCE || Math.min(face.box[2], face.box[3]) < MIN_FACE_SIZE) {
        return { ready: false, reason: 'Acércate un poco y mira hacia la cámara.' };
    }
    const angle = face.rotation?.angle;
    if (angle && Math.max(Math.abs(angle.yaw), Math.abs(angle.pitch), Math.abs(angle.roll)) > MAX_FACE_ANGLE) {
        return { ready: false, reason: 'Mira de frente a la cámara.' };
    }
    if (!face.embedding || face.embedding.length === 0) {
        return { ready: false, reason: 'Calculando la firma facial…' };
    }
    if ((face.real || 0) < MIN_CONFIDENCE || (face.live || 0) < MIN_CONFIDENCE) {
        return { ready: false, reason: 'No se ha podido confirmar que sea un rostro real.' };
    }

    return { ready: true, face, reason: 'Rostro preparado.' };
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
                    minConfidence: MIN_CONFIDENCE, minSize: MIN_FACE_SIZE,
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
            gesture: { enabled: false },
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
    } catch (error) {
        dom.cameraState.textContent = 'No se pudo iniciar la cámara.';
        setFeedback(error.message || 'Autoriza el uso de la cámara para continuar.', true);
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
            if (dom.camera.currentTime !== state.handVideoTime) {
                state.handVideoTime = dom.camera.currentTime;
                state.handResult = state.handRecognizer.recognizeForVideo(dom.camera, performance.now());
            }
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

async function refresh() {
    try {
        state.authentication = await load('/authentication');
        const active = state.authentication.activeSession;
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
        const descriptors = await captureFaceDescriptors(ENROLLMENT_SAMPLES,
            count => setFeedback(`Capturando rostro ${count}/${ENROLLMENT_SAMPLES}…`));
        const profile = await send('/profiles', {
            displayName: dom.displayName.value.trim(),
            modelVersion: MODEL_VERSION,
            descriptor: descriptors[0],
        });
        let captures = 1;
        for (const descriptor of descriptors.slice(1)) {
            try {
                await send(`/profiles/${profile.id}/templates`, { modelVersion: MODEL_VERSION, descriptor });
                captures++;
            } catch (_) {}
        }
        dom.displayName.value = '';
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

function sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

async function captureFaceDescriptors(count, onCapture) {
    const descriptors = [];
    let lastVersion = -1;
    const expiresAt = Date.now() + 10000;
    while (descriptors.length < count && Date.now() < expiresAt) {
        if (state.resultVersion !== lastVersion) {
            lastVersion = state.resultVersion;
            const status = faceStatus(state.result);
            if (status.ready) {
                descriptors.push(Array.from(status.face.embedding));
                onCapture(descriptors.length);
            }
        }
        await sleep(60);
    }
    if (descriptors.length < count) {
        throw new Error('Mantén el rostro centrado y bien iluminado durante unos segundos.');
    }
    return descriptors;
}

async function waitForChallenge(challenge) {
    const detected = challengeDetector(challenge.type);
    const expiresAt = Date.parse(challenge.expiresAt);
    let consecutiveFrames = 0;
    let lastVersion = -1;
    while (Date.now() < expiresAt) {
        const remaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
        dom.challengeTime.textContent = `${remaining} s`;
        if (state.resultVersion !== lastVersion) {
            lastVersion = state.resultVersion;
            const status = faceStatus(state.result);
            consecutiveFrames = status.ready && detected(state.handResult) ? consecutiveFrames + 1 : 0;
            if (consecutiveFrames >= 3) {
                return status.face;
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
        const challenge = await send('/authentication/challenges', {});
        dom.challenge.hidden = false;
        dom.challengeInstruction.textContent = 'Mantén el puño cerrado';
        setFeedback('Verificando puño y rostro…');
        const face = await waitForChallenge(challenge);
        const session = await send(`/authentication/challenges/${challenge.challengeId}/complete`, {
            modelVersion: MODEL_VERSION,
            descriptor: Array.from(face.embedding),
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
